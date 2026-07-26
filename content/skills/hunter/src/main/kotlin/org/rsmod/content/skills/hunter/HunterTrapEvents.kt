package org.rsmod.content.skills.hunter

import dev.openrune.ServerCacheManager
import dev.openrune.types.ObjectServerType
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hunterLvl
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.stats.levelmod.InvisibleLevels
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class HunterTrapEvents
@Inject
constructor(
    private val locRepo: LocRepository,
    private val npcRepo: NpcRepository,
    private val invisibleLevels: InvisibleLevels,
    private val xpMods: XpModifiers,
) : PluginScript() {
    override fun ScriptContext.startup() {
        HunterDefinitions.trapRecipes.filter(HunterTrapRecipe::portable).forEach { recipe ->
            onOpHeld1(recipe.tool) { layTrap(recipe, it.slot) }
        }
        onOpHeld1(HunterDefinitions.BIRD_SNARE_PACK) {
            unpackTrapPack(it.slot, HunterDefinitions.BIRD_SNARE_PACK, HunterDefinitions.BIRD_SNARE)
        }
        onOpHeld1(HunterDefinitions.BOX_TRAP_PACK) {
            unpackTrapPack(it.slot, HunterDefinitions.BOX_TRAP_PACK, HunterDefinitions.BOX_TRAP)
        }

        HunterDefinitions.trapRecipes
            .flatMap { listOf(it.setupLocId, it.caughtLocId, it.failedLocId) }
            .mapNotNull(::locTypeOrNull)
            .distinctBy { it.id }
            .forEach { type ->
                onOpLoc1(type) { clickTrap(it.loc) }
                onOpLoc2(type) { clickTrap(it.loc) }
        }

        HunterDefinitions.deadfallBoulderLocIds
            .mapNotNull(::locTypeOrNull)
            .distinctBy { it.id }
            .forEach { type ->
                onOpLocU(type, HunterDefinitions.DEADFALL_LOGS) {
                    layDeadfallTrap(it.loc, it.invSlot)
                }
        }

        onPlayerQueueWithArgs<HunterTrapTask>(TRAP_QUEUE) { resolveTrap(it.args) }
        onPlayerLogout { cleanupTraps(player) }
    }

    private suspend fun ProtectedAccess.layTrap(recipe: HunterTrapRecipe, slot: Int) {
        val trapCoords = coords
        val target = targetFor(recipe, trapCoords)
        val requiredLevel = target?.level ?: recipe.level
        if (player.hunterLvl < requiredLevel) {
            mes("You need a Hunter level of $requiredLevel to use this trap.")
            return
        }
        if (activeTrapCount() >= trapLimit()) {
            mes("You are already using as many traps as you can.")
            return
        }
        if (trapStates().containsKey(trapKey(trapCoords))) {
            mes("There is already a trap here.")
            return
        }
        if (locRepo.findExact(trapCoords, LocShape.CentrepieceStraight) != null) {
            mes("You can't place a trap here.")
            return
        }
        if (invDel(inv, recipe.tool, 1, slot = slot).failure) {
            return
        }

        anim(SET_TRAP_ANIM)
        delay(recipe.setupTicks)

        val trapLoc = locRepo.add(
            trapCoords,
            locType(recipe.setupLocId),
            Int.MAX_VALUE,
            LocAngle.South,
            LocShape.CentrepieceStraight,
        )
        trapStates()[trapKey(trapCoords)] =
            HunterTrapState(
                recipeIndex = HunterDefinitions.trapIndex.getValue(recipe),
                result = HunterTrapResult.Waiting,
            )

        mes("You set up the ${trapName(recipe)}.")
        queueTrapTask(recipe, trapLoc.coords, recipe.triggerTicks)
    }

    private suspend fun ProtectedAccess.layDeadfallTrap(loc: BoundLocInfo, slot: Int) {
        val recipe = HunterDefinitions.trapByTool.getValue(HunterDefinitions.DEADFALL_LOGS)
        val trapCoords = loc.coords
        val target = targetFor(recipe, trapCoords)
        val requiredLevel = target?.level ?: recipe.level
        if (player.hunterLvl < requiredLevel) {
            mes("You need a Hunter level of $requiredLevel to use this trap.")
            return
        }
        if (activeTrapCount() >= trapLimit()) {
            mes("You are already using as many traps as you can.")
            return
        }
        if (trapStates().containsKey(trapKey(trapCoords))) {
            mes("There is already a trap here.")
            return
        }
        if (invDel(inv, recipe.tool, 1, slot = slot).failure) {
            return
        }

        anim(SET_TRAP_ANIM)
        delay(recipe.setupTicks)

        locRepo.change(loc, locType(recipe.setupLocId), Int.MAX_VALUE)
        trapStates()[trapKey(trapCoords)] =
            HunterTrapState(
                recipeIndex = HunterDefinitions.trapIndex.getValue(recipe),
                result = HunterTrapResult.Waiting,
                restoreLocId = loc.id,
            )

        mes("You set up the ${trapName(recipe)}.")
        queueTrapTask(recipe, trapCoords, recipe.triggerTicks)
    }

    private fun ProtectedAccess.unpackTrapPack(slot: Int, pack: String, trap: String) {
        if (invDel(inv, pack, 1, slot = slot).failure) {
            return
        }
        if (invAdd(inv, trap, 1).failure) {
            invAdd(inv, pack, 1)
            mes("You don't have enough inventory space.")
            return
        }
        mes("You unpack the trap.")
    }

    private fun ProtectedAccess.resolveTrap(task: HunterTrapTask) {
        val coords = CoordGrid(task.coords)
        if (task.expire) {
            expireTrap(task.recipeIndex, coords)
            return
        }

        val key = trapKey(coords)
        val state = player.attr[HUNTER_TRAPS_ATTR]?.get(key) ?: return
        if (state.recipeIndex != task.recipeIndex || state.result != HunterTrapResult.Waiting) {
            return
        }

        val recipe = HunterDefinitions.trapRecipes.getOrNull(task.recipeIndex) ?: return
        val loc = locRepo.findExact(coords, locType(recipe.setupLocId)) ?: return
        val target = targetFor(recipe, coords)
        val success =
            target != null &&
                player.hunterLvl >= target.level &&
                statRandom("stat.hunter", target.lowChance, target.highChance, invisibleLevels)
        val result = if (success) HunterTrapResult.Caught else HunterTrapResult.Failed
        val nextLoc = if (success) recipe.caughtLocId else recipe.failedLocId

        locRepo.change(BoundLocInfo(loc, locType(recipe.setupLocId)), locType(nextLoc), Int.MAX_VALUE)
        trapStates()[key] =
            state.copy(
                result = result,
                reward = target?.reward,
                rewardCount = target?.rewardCount ?: 0,
                xp = target?.xp ?: 0.0,
            )
        queueTrapTask(recipe, coords, recipe.expireTicks, expire = true)
    }

    private suspend fun ProtectedAccess.clickTrap(loc: BoundLocInfo) {
        val key = trapKey(loc.coords)
        val state = player.attr[HUNTER_TRAPS_ATTR]?.get(key)
        if (state == null) {
            mes("This isn't your trap.")
            return
        }

        val recipe = HunterDefinitions.trapRecipes.getOrNull(state.recipeIndex)
        if (recipe == null) {
            player.attr[HUNTER_TRAPS_ATTR]?.remove(key)
            mes("The trap falls apart.")
            return
        }

        when (state.result) {
            HunterTrapResult.Waiting -> mes("The trap is still waiting.")
            HunterTrapResult.Caught -> collectCaughtTrap(loc, key, recipe, state)
            HunterTrapResult.Failed -> collectFailedTrap(loc, key, recipe, state)
        }
    }

    private suspend fun ProtectedAccess.collectCaughtTrap(
        loc: BoundLocInfo,
        key: String,
        recipe: HunterTrapRecipe,
        state: HunterTrapState,
    ) {
        val reward = state.reward ?: recipe.reward
        val rewardCount = state.rewardCount.takeIf { it > 0 } ?: recipe.rewardCount
        val xp = state.xp.takeIf { it > 0.0 } ?: recipe.xp
        val neededSlots = if (inv.contains(reward)) {
            if (recipe.returnsTool) 1 else 0
        } else {
            if (recipe.returnsTool) 2 else 1
        }
        if (neededSlots > 0 && inv.freeSpace() < neededSlots) {
            mes("You don't have enough inventory space.")
            return
        }

        anim(CHECK_TRAP_ANIM)
        delay(1)

        clearTrapLoc(loc, recipe, state)
        player.attr[HUNTER_TRAPS_ATTR]?.remove(key)
        if (recipe.returnsTool) {
            invAdd(inv, recipe.tool, 1)
        }
        if (invAdd(inv, reward, rewardCount).failure) {
            mes("You don't have enough inventory space.")
            return
        }
        statAdvance("stat.hunter", xp * xpMods.get(player, "stat.hunter"))
        mes("You dismantle the trap and claim your catch.")
    }

    private suspend fun ProtectedAccess.collectFailedTrap(
        loc: BoundLocInfo,
        key: String,
        recipe: HunterTrapRecipe,
        state: HunterTrapState,
    ) {
        if (recipe.returnsTool && inv.isFull()) {
            mes("You don't have enough inventory space.")
            return
        }

        anim(CHECK_TRAP_ANIM)
        delay(1)

        clearTrapLoc(loc, recipe, state)
        player.attr[HUNTER_TRAPS_ATTR]?.remove(key)
        if (recipe.returnsTool) {
            invAdd(inv, recipe.tool, 1)
        }
        mes("You dismantle the failed trap.")
    }

    private fun ProtectedAccess.queueTrapTask(
        recipe: HunterTrapRecipe,
        coords: CoordGrid,
        cycles: Int,
        expire: Boolean = false,
    ) {
        weakQueue(TRAP_QUEUE, cycles, HunterTrapTask(HunterDefinitions.trapIndex.getValue(recipe), coords.packed, expire))
    }

    private fun ProtectedAccess.expireTrap(recipeIndex: Int, coords: CoordGrid) {
        val key = trapKey(coords)
        val state = player.attr[HUNTER_TRAPS_ATTR]?.get(key) ?: return
        if (state.recipeIndex != recipeIndex || state.result == HunterTrapResult.Waiting) {
            return
        }
        val recipe = HunterDefinitions.trapRecipes.getOrNull(recipeIndex) ?: return
        clearTrapLoc(coords, recipe, state)
        player.attr[HUNTER_TRAPS_ATTR]?.remove(key)
        if (player.attr[HUNTER_TRAPS_ATTR]?.isEmpty() == true) {
            player.attr.remove(HUNTER_TRAPS_ATTR)
        }
    }

    private fun cleanupTraps(player: Player) {
        val states = player.attr[HUNTER_TRAPS_ATTR] ?: return
        states.forEach { (key, state) ->
            val coords = key.toIntOrNull()?.let(::CoordGrid) ?: return@forEach
            val recipe = HunterDefinitions.trapRecipes.getOrNull(state.recipeIndex) ?: return@forEach
            clearTrapLoc(coords, recipe, state)
        }
        player.attr.remove(HUNTER_TRAPS_ATTR)
    }

    private fun clearTrapLoc(coords: CoordGrid, recipe: HunterTrapRecipe, state: HunterTrapState) {
        sequenceOf(recipe.setupLocId, recipe.caughtLocId, recipe.failedLocId)
            .mapNotNull(::locTypeOrNull)
            .forEach { type ->
                val loc = locRepo.findExact(coords, type) ?: return@forEach
                clearTrapLoc(BoundLocInfo(loc, type), recipe, state)
                return
            }
    }

    private fun clearTrapLoc(loc: BoundLocInfo, recipe: HunterTrapRecipe, state: HunterTrapState) {
        val restoreLocId = state.restoreLocId
        if (restoreLocId != null) {
            locRepo.change(loc, locType(restoreLocId), Int.MAX_VALUE)
        } else {
            locRepo.del(loc, Int.MAX_VALUE)
        }
    }

    private fun ProtectedAccess.trapStates(): MutableMap<String, HunterTrapState> =
        player.attr.getOrPut(HUNTER_TRAPS_ATTR) { mutableMapOf() }

    private fun ProtectedAccess.activeTrapCount(): Int = trapStates().size

    private fun ProtectedAccess.trapLimit(): Int =
        when {
            player.hunterLvl >= 80 -> 5
            player.hunterLvl >= 60 -> 4
            player.hunterLvl >= 40 -> 3
            player.hunterLvl >= 20 -> 2
            else -> 1
        }

    private fun trapName(recipe: HunterTrapRecipe): String =
        when (recipe.tool) {
            HunterDefinitions.BIRD_SNARE -> "bird snare"
            HunterDefinitions.BOX_TRAP -> "box trap"
            HunterDefinitions.DEADFALL_LOGS -> "deadfall trap"
            else -> "trap"
        }

    private fun targetFor(recipe: HunterTrapRecipe, coords: CoordGrid): HunterTrapTarget? {
        val targets =
            when (recipe.tool) {
                HunterDefinitions.BIRD_SNARE -> HunterDefinitions.birdSnareTargets
                HunterDefinitions.BOX_TRAP -> HunterDefinitions.boxTrapTargets
                HunterDefinitions.DEADFALL_LOGS -> HunterDefinitions.deadfallTargets
                else -> return null
            }
        return nearbyTargets(coords, targets)
            .minWithOrNull(compareBy<Pair<HunterTrapTarget, Int>> { it.second }.thenByDescending { it.first.level })
            ?.first
    }

    private fun nearbyTargets(
        coords: CoordGrid,
        targets: List<HunterTrapTarget>,
    ): Sequence<Pair<HunterTrapTarget, Int>> {
        val byNpc = targets.associateBy(HunterTrapTarget::npc)
        return npcRepo.findAll(ZoneKey.from(coords), TRAP_TARGET_ZONE_RADIUS)
            .filter { it.coords.level == coords.level }
            .mapNotNull { npc ->
                val target = byNpc[npc.type.internalName] ?: return@mapNotNull null
                val distance = npc.coords.chebyshevDistance(coords)
                if (distance <= TRAP_TARGET_RADIUS) target to distance else null
            }
    }

    private fun trapKey(coords: CoordGrid): String = coords.packed.toString()

    private fun locType(id: Int): ObjectServerType =
        locTypeOrNull(id) ?: error("Missing hunter trap loc: $id")

    private fun locTypeOrNull(id: Int): ObjectServerType? =
        ServerCacheManager.getObject(id)

    private companion object {
        private const val TRAP_QUEUE = "queue.hunter_trap"
        private const val SET_TRAP_ANIM = "seq.human_generic_middletake"
        private const val CHECK_TRAP_ANIM = "seq.human_pickupfloor"
        private const val TRAP_TARGET_RADIUS = 12
        private const val TRAP_TARGET_ZONE_RADIUS = 2
    }
}
