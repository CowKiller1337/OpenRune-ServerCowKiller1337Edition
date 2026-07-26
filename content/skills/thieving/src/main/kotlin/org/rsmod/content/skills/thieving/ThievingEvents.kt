package org.rsmod.content.skills.thieving

import dev.openrune.ServerCacheManager
import dev.openrune.types.ObjectServerType
import jakarta.inject.Inject
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.thievingLvl
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.stats.levelmod.InvisibleLevels
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.isType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ThievingEvents
@Inject
constructor(
    private val locRepo: LocRepository,
    private val invisibleLevels: InvisibleLevels,
    private val xpMods: XpModifiers,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (target in ThievingDefinitions.pickpocketTargets) {
            for (npc in target.npcs) {
                onOpNpc3(npc) { pickpocket(target) }
            }
        }

        for (pouch in ThievingDefinitions.pouches.keys) {
            onOpHeld1(pouch) { openPouch(pouch, it.slot) }
        }

        for (stall in ThievingDefinitions.stallTargets) {
            val registeredLocIds = hashSetOf<Int>()
            for (locId in stall.locIds) {
                val type = locTypeOrNull(locId) ?: continue
                if (registeredLocIds.add(type.id)) {
                    onOpLoc1(type) { event -> stealFromStall(event.loc, stall) }
                }
            }
        }

        for (chest in ThievingDefinitions.chestTargets) {
            val registeredLocIds = hashSetOf<Int>()
            for (locId in chest.locIds) {
                val type = locTypeOrNull(locId) ?: continue
                if (registeredLocIds.add(type.id)) {
                    onOpLoc1(type) { event -> stealFromChest(event.loc, chest) }
                }
            }
        }
    }

    private suspend fun ProtectedAccess.pickpocket(target: PickpocketTarget) {
        if (actionDelay > mapClock) {
            return
        }
        if (player.thievingLvl < target.level) {
            mes("You need a Thieving level of ${target.level} to pickpocket this ${target.displayName}.")
            return
        }

        val reward = target.rewards.randomOrNull()
        if (!hasSpaceForPickpocket(target, reward)) {
            mes("You don't have enough inventory space.")
            return
        }

        actionDelay = mapClock + 3
        anim(PICKPOCKET_ANIM)
        delay(1)

        if (!rollPickpocketSuccess(target)) {
            failPickpocket(target)
            return
        }

        if (!addPickpocketReward(target, reward)) {
            mes("You don't have enough inventory space.")
            return
        }

        statAdvance("stat.thieving", target.xp * xpMods.get(player, "stat.thieving"))
        mes("You pick the ${target.displayName}'s pocket.")
    }

    private fun ProtectedAccess.openPouch(pouch: String, slot: Int) {
        val coins = ThievingDefinitions.pouches[pouch] ?: return
        val pouchObj = inv[slot]?.takeIf { it.isType(pouch) } ?: return
        val pouchCount = pouchObj.count
        if (invDel(inv, pouch, pouchCount, slot = slot).failure) {
            return
        }
        if (invAdd(inv, ThievingDefinitions.COINS, coins * pouchCount).failure) {
            invAdd(inv, pouch, pouchCount)
            mes("You don't have enough inventory space.")
            return
        }
        if (pouchCount == 1) {
            mes("You open the coin pouch.")
        } else {
            mes("You open the coin pouches.")
        }
    }

    private suspend fun ProtectedAccess.stealFromStall(loc: BoundLocInfo, stall: StallTarget) {
        if (actionDelay > mapClock) {
            return
        }
        if (player.thievingLvl < stall.level) {
            mes("You need a Thieving level of ${stall.level} to steal from this stall.")
            return
        }

        val reward = stall.rewards.random()
        if (inv.freeSpace() < requiredSpace(reward.obj)) {
            mes("You don't have enough inventory space.")
            return
        }

        actionDelay = mapClock + 3
        anim(PICKPOCKET_ANIM)
        delay(1)

        if (invAdd(inv, reward.obj, reward.count).failure) {
            mes("You don't have enough inventory space.")
            return
        }

        statAdvance("stat.thieving", stall.xp * xpMods.get(player, "stat.thieving"))
        mes("You steal from the ${stall.displayName}.")

        val emptyType = stall.emptyLocId?.let(::locTypeOrNull)
        if (emptyType != null) {
            locRepo.change(loc, emptyType, stall.respawnTicks)
        }
    }

    private suspend fun ProtectedAccess.stealFromChest(loc: BoundLocInfo, chest: ChestTarget) {
        if (actionDelay > mapClock) {
            return
        }
        if (player.thievingLvl < chest.level) {
            mes("You need a Thieving level of ${chest.level} to steal from this ${chest.displayName}.")
            return
        }
        if (chestLocked(loc)) {
            mes("This ${chest.displayName} has already been looted.")
            return
        }

        val reward = chest.rewards.random()
        if (inv.freeSpace() < requiredSpace(reward.obj)) {
            mes("You don't have enough inventory space.")
            return
        }

        actionDelay = mapClock + 3
        anim(OPEN_CHEST_ANIM)
        delay(1)

        if (invAdd(inv, reward.obj, reward.count).failure) {
            mes("You don't have enough inventory space.")
            return
        }

        statAdvance("stat.thieving", chest.xp * xpMods.get(player, "stat.thieving"))
        mes("You steal from the ${chest.displayName}.")
        lockChest(loc, chest.respawnTicks)

        val openType = chest.openLocId?.let(::locTypeOrNull)
        if (openType != null) {
            locRepo.change(loc, openType, chest.respawnTicks)
        }
    }

    private fun ProtectedAccess.hasSpaceForPickpocket(target: PickpocketTarget, reward: ThievingReward?): Boolean {
        val pouch = target.pouch
        if (pouch != null) {
            return !inv.isFull() || inv.contains(pouch)
        }
        if (reward == null) {
            return false
        }
        return inv.freeSpace() >= requiredSpace(reward.obj)
    }

    private fun ProtectedAccess.addPickpocketReward(target: PickpocketTarget, reward: ThievingReward?): Boolean {
        val multiplier = if (wearingRogueOutfit()) 2 else 1
        val pouch = target.pouch
        if (pouch != null) {
            return !invAdd(inv, pouch, multiplier).failure
        }
        if (reward == null) {
            return false
        }
        return !invAdd(inv, reward.obj, reward.count * multiplier).failure
    }

    private fun ProtectedAccess.requiredSpace(obj: String): Int =
        if (inv.contains(obj)) 0 else 1

    private fun ProtectedAccess.failPickpocket(target: PickpocketTarget) {
        if (hasDodgyNecklace() && random.randomBoolean(DODGY_NECKLACE_SAVE_CHANCE)) {
            actionDelay = mapClock + 2
            mes("Your dodgy necklace helps you avoid being stunned.")
            return
        }
        actionDelay = mapClock + 5
        takeInstantHit(type = HitType.Typeless, damage = target.failDamage)
        mes("You fail to pick the ${target.displayName}'s pocket.")
    }

    private fun ProtectedAccess.rollPickpocketSuccess(target: PickpocketTarget): Boolean {
        if (statRandom("stat.thieving", target.lowChance, target.highChance, invisibleLevels)) {
            return true
        }
        return wearingGlovesOfSilence() && random.of(maxExclusive = 100) < GLOVES_OF_SILENCE_SAVE_PERCENT
    }

    private fun ProtectedAccess.hasDodgyNecklace(): Boolean =
        DODGY_NECKLACE in player.worn

    private fun ProtectedAccess.wearingGlovesOfSilence(): Boolean =
        GLOVES_OF_SILENCE in player.worn

    private fun ProtectedAccess.wearingRogueOutfit(): Boolean =
        ROGUE_OUTFIT.all { it in player.worn }

    private fun ProtectedAccess.chestLocked(loc: BoundLocInfo): Boolean {
        val key = chestKey(loc)
        val cooldowns = chestCooldowns()
        val unlockTick = cooldowns[key] ?: return false
        if (unlockTick > mapClock) {
            return true
        }
        cooldowns.remove(key)
        return false
    }

    private fun ProtectedAccess.lockChest(loc: BoundLocInfo, ticks: Int) {
        chestCooldowns()[chestKey(loc)] = mapClock + ticks
    }

    private fun ProtectedAccess.chestCooldowns(): MutableMap<String, Int> =
        player.attr.getOrPut(THIEVING_CHEST_COOLDOWNS_ATTR) { mutableMapOf() }

    private fun chestKey(loc: BoundLocInfo): String =
        loc.coords.packed.toString()

    private fun locTypeOrNull(id: Int): ObjectServerType? =
        ServerCacheManager.getObject(id)

    private companion object {
        private const val PICKPOCKET_ANIM = "seq.human_pickpocket"
        private const val OPEN_CHEST_ANIM = "seq.human_openchest"
        private const val DODGY_NECKLACE = "obj.dodgy_necklace"
        private const val DODGY_NECKLACE_SAVE_CHANCE = 4
        private const val GLOVES_OF_SILENCE = "obj.hunting_silent_gloves"
        private const val GLOVES_OF_SILENCE_SAVE_PERCENT = 5
        private val THIEVING_CHEST_COOLDOWNS_ATTR: AttributeKey<MutableMap<String, Int>> =
            AttributeKey(temp = true)
        private val ROGUE_OUTFIT =
            listOf(
                "obj.roguesden_helm",
                "obj.roguesden_body",
                "obj.roguesden_legs",
                "obj.roguesden_gloves",
                "obj.roguesden_boots",
            )
    }
}
