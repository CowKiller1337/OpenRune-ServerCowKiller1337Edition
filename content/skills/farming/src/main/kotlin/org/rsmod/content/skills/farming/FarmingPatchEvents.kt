package org.rsmod.content.skills.farming

import dev.openrune.ServerCacheManager
import dev.openrune.types.ObjectServerType
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.farmingLvl
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLoc4
import org.rsmod.api.script.onOpLoc5
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FarmingPatchEvents
@Inject
constructor(
    private val locRepo: LocRepository,
    private val xpMods: XpModifiers,
) : PluginScript() {
    override fun ScriptContext.startup() {
        FarmingDefinitions.trackedLocIds.mapNotNull(::locTypeOrNull).distinctBy { it.id }.forEach { type ->
            bindPatchClick(type)
            onOpLocU(type) { useItemOnPatch(it.loc, it.objType.internalName) }
        }
    }

    private fun ScriptContext.bindPatchClick(type: ObjectServerType) {
        val indexes =
            listOfNotNull(
                type.optionIndex("Harvest"),
                type.optionIndex("Pick"),
                type.optionIndex("Pick-fruit"),
                type.optionIndex("Pick fruit"),
                type.optionIndex("Check-health"),
                type.optionIndex("Check health"),
                type.optionIndex("Rake"),
                type.optionIndex("Clear"),
                type.optionIndex("Inspect"),
            ).distinct()
        val ops = if (indexes.isEmpty()) listOf(1) else indexes
        ops.forEach { index -> bindLocOp(type, index) { loc, clickedType -> clickPatch(loc, clickedType) } }
    }

    private fun ScriptContext.bindLocOp(
        type: ObjectServerType,
        index: Int,
        action: suspend ProtectedAccess.(BoundLocInfo, ObjectServerType) -> Unit,
    ) {
        when (index) {
            1 -> onOpLoc1(type) { action(it.loc, it.type) }
            2 -> onOpLoc2(type) { action(it.loc, it.type) }
            3 -> onOpLoc3(type) { action(it.loc, it.type) }
            4 -> onOpLoc4(type) { action(it.loc, it.type) }
            5 -> onOpLoc5(type) { action(it.loc, it.type) }
        }
    }

    private suspend fun ProtectedAccess.useItemOnPatch(loc: BoundLocInfo, item: String) {
        val recipe = FarmingDefinitions.recipeBySeed[item]
        if (recipe != null) {
            plant(loc, recipe)
            return
        }

        val compostLevel = FarmingDefinitions.compostItems[item]
        if (compostLevel != null) {
            applyCompost(loc, item, compostLevel)
            return
        }

        val wateringCanCharges = FarmingDefinitions.wateringCans[item]
        if (wateringCanCharges != null) {
            waterPatch(loc, item, wateringCanCharges)
            return
        }

        when (item) {
            FarmingDefinitions.RAKE -> rake(loc)
            FarmingDefinitions.SPADE -> clearPatch(loc)
            else -> mes("Nothing interesting happens.")
        }
    }

    private suspend fun ProtectedAccess.clickPatch(loc: BoundLocInfo, type: ObjectServerType) {
        if (FarmingDefinitions.weedProgression.containsKey(type.id)) {
            rake(loc)
            return
        }

        val state = patchState(loc)
        if (state == null) {
            inspectEmptyPatch(type)
            return
        }

        val recipe = state.recipe
        if (isReady(state)) {
            if (recipe.harvestMode == FarmingHarvestMode.CheckHealth) {
                checkHealthOrHarvestTree(loc, state)
            } else {
                harvest(loc, recipe)
            }
        } else {
            inspectGrowingPatch(state)
        }
    }

    private suspend fun ProtectedAccess.plant(loc: BoundLocInfo, recipe: FarmingRecipe) {
        val kind = patchKind(loc)
        if (kind == null) {
            mes("You can't plant that here.")
            return
        }
        if (kind != recipe.kind) {
            mes("This is a ${kind.displayName}; you can't plant ${recipe.displayName} here.")
            return
        }
        if (patchState(loc) != null) {
            mes("Something is already growing in this patch.")
            return
        }
        if (player.farmingLvl < recipe.level) {
            mes("You need a Farming level of ${recipe.level} to plant ${recipe.displayName}.")
            return
        }
        val plantTool = recipe.plantTool
        if (plantTool != null && !inv.contains(plantTool)) {
            mes(recipe.plantToolMessage)
            return
        }
        if (inv.count(recipe.seed) < recipe.seedCount) {
            val itemName = if (recipe.seedCount == 1) recipe.plantItemName else "${recipe.plantItemName}s"
            mes("You need ${recipe.seedCount} $itemName to plant this.")
            return
        }

        anim(PLANT_ANIM)
        delay(2)

        if (invDel(inv, recipe.seed, recipe.seedCount).failure) {
            return
        }

        val key = patchKey(loc.coords)
        player.attr.getOrPut(FARMING_PATCH_RECIPE_ATTR) { mutableMapOf() }[key] =
            FarmingDefinitions.recipeIndex.getValue(recipe)
        player.attr.getOrPut(FARMING_PATCH_PLANTED_AT_ATTR) { mutableMapOf() }[key] =
            currentEpochSeconds()
        player.attr[FARMING_PATCH_HARVESTS_LEFT_ATTR]?.remove(key)
        player.attr[FARMING_PATCH_WATERED_ATTR]?.remove(key)
        player.attr[FARMING_PATCH_HEALTH_CHECKED_ATTR]?.remove(key)

        locRepo.change(loc, locType(recipe.plantedLocId), Int.MAX_VALUE)
        statAdvance("stat.farming", recipe.plantXp * xpMods.get(player, "stat.farming"))
        mes("You plant the ${recipe.displayName}.")
    }

    private suspend fun ProtectedAccess.harvest(loc: BoundLocInfo, recipe: FarmingRecipe) {
        val product = recipe.product
        if (product == null) {
            mes("There is nothing to harvest from this patch.")
            return
        }
        if (inv.isFull()) {
            mes("Your inventory is too full to hold any more crops.")
            return
        }

        val key = patchKey(loc.coords)
        val harvests = player.attr.getOrPut(FARMING_PATCH_HARVESTS_LEFT_ATTR) { mutableMapOf() }
        val remaining = harvests.getOrPut(key) { rollYield(recipe, key) }

        anim(PICK_ANIM)
        delay(1)

        if (invAdd(inv, product, 1).failure) {
            mes("Your inventory is too full to hold any more crops.")
            return
        }

        statAdvance("stat.farming", recipe.harvestXp * xpMods.get(player, "stat.farming"))
        mes("You harvest some ${recipe.displayName}.")

        val nextRemaining = remaining - 1
        if (nextRemaining > 0) {
            harvests[key] = nextRemaining
            return
        }

        harvests.remove(key)
        clearState(key)
        locRepo.change(loc, locType(recipe.kind.cleanLocIds.first()), Int.MAX_VALUE)
        mes("The patch is now empty.")
    }

    private suspend fun ProtectedAccess.checkHealthOrHarvestTree(
        loc: BoundLocInfo,
        state: FarmingPatchState,
    ) {
        val recipe = state.recipe
        val key = patchKey(loc.coords)
        if (!state.healthChecked) {
            anim(PICK_ANIM)
            delay(1)
            player.attr.getOrPut(FARMING_PATCH_HEALTH_CHECKED_ATTR) { mutableMapOf() }[key] = 1
            if (recipe.product != null) {
                player.attr.getOrPut(FARMING_PATCH_HARVESTS_LEFT_ATTR) { mutableMapOf() }[key] =
                    rollYield(recipe, key)
            }
            statAdvance("stat.farming", recipe.harvestXp * xpMods.get(player, "stat.farming"))
            mes("You check the health of the ${recipe.displayName}.")
            return
        }

        if (recipe.product == null) {
            mes("The ${recipe.displayName} has already been checked. Use a spade to clear it.")
            return
        }

        harvestFruitTreeProduce(loc, recipe)
    }

    private suspend fun ProtectedAccess.harvestFruitTreeProduce(
        loc: BoundLocInfo,
        recipe: FarmingRecipe,
    ) {
        val product = recipe.product ?: return
        if (inv.isFull()) {
            mes("Your inventory is too full to hold any more crops.")
            return
        }

        val key = patchKey(loc.coords)
        val harvests = player.attr.getOrPut(FARMING_PATCH_HARVESTS_LEFT_ATTR) { mutableMapOf() }
        val remaining = harvests.getOrPut(key) { rollYield(recipe, key) }
        if (remaining <= 0) {
            mes("There is no fruit left on this tree.")
            return
        }

        anim(PICK_ANIM)
        delay(1)

        if (invAdd(inv, product, 1).failure) {
            mes("Your inventory is too full to hold any more crops.")
            return
        }

        val nextRemaining = remaining - 1
        harvests[key] = nextRemaining
        mes("You pick some fruit from the ${recipe.displayName}.")
        if (nextRemaining <= 0) {
            mes("You have picked all the fruit from this tree.")
        }
    }

    private suspend fun ProtectedAccess.rake(loc: BoundLocInfo) {
        val next = FarmingDefinitions.weedProgression[loc.id]
        if (next == null) {
            mes("This patch does not need weeding.")
            return
        }
        if (!inv.contains(FarmingDefinitions.RAKE)) {
            mes("You need a rake to clear the weeds.")
            return
        }

        anim(RAKE_ANIM)
        delay(2)
        invAdd(inv, WEEDS, 1)
        statAdvance("stat.farming", RAKE_XP * xpMods.get(player, "stat.farming"))
        locRepo.change(loc, locType(next), Int.MAX_VALUE)
        mes(if (FarmingDefinitions.weedProgression.containsKey(next)) "You clear some weeds." else "You clear the patch.")
    }

    private suspend fun ProtectedAccess.clearPatch(loc: BoundLocInfo) {
        val state = patchState(loc)
        if (state == null) {
            mes("There is nothing growing here.")
            return
        }
        if (!inv.contains(FarmingDefinitions.SPADE)) {
            mes("You need a spade to clear this patch.")
            return
        }

        anim(DIG_ANIM)
        delay(2)

        val key = patchKey(loc.coords)
        clearState(key)
        locRepo.change(loc, locType(state.recipe.kind.cleanLocIds.first()), Int.MAX_VALUE)
        mes("You clear the patch.")
    }

    private suspend fun ProtectedAccess.applyCompost(
        loc: BoundLocInfo,
        compost: String,
        compostLevel: Int,
    ) {
        if (patchState(loc) != null) {
            mes("You need to compost the patch before planting.")
            return
        }
        if (patchKind(loc) == null) {
            mes("You can't compost this patch.")
            return
        }

        val key = patchKey(loc.coords)
        val compostMap = player.attr.getOrPut(FARMING_PATCH_COMPOST_ATTR) { mutableMapOf() }
        if ((compostMap[key] ?: 0) >= compostLevel) {
            mes("This patch has already been treated.")
            return
        }

        anim(COMPOST_ANIM)
        delay(2)

        if (invDel(inv, compost, 1).failure) {
            return
        }
        invAdd(inv, FarmingDefinitions.BUCKET_EMPTY, 1)
        compostMap[key] = compostLevel
        mes("You treat the patch with compost.")
    }

    private suspend fun ProtectedAccess.waterPatch(
        loc: BoundLocInfo,
        wateringCan: String,
        charges: Int,
    ) {
        val state = patchState(loc)
        if (state == null) {
            mes("You can only water growing crops.")
            return
        }
        if (state.recipe.kind == FarmingPatchKind.Herb) {
            mes("Herbs do not need watering.")
            return
        }
        if (state.recipe.kind == FarmingPatchKind.Seaweed) {
            mes("Seaweed does not need watering.")
            return
        }
        if (state.recipe.harvestMode == FarmingHarvestMode.CheckHealth) {
            mes("Saplings do not need watering after they are planted.")
            return
        }

        val key = patchKey(loc.coords)
        val wateredMap = player.attr.getOrPut(FARMING_PATCH_WATERED_ATTR) { mutableMapOf() }
        if (wateredMap[key] != null) {
            mes("This patch has already been watered.")
            return
        }

        anim(WATER_ANIM)
        delay(2)

        if (invDel(inv, wateringCan, 1).failure) {
            return
        }
        invAdd(inv, wateringCanAfterUse(charges), 1)

        wateredMap[key] = currentEpochSeconds()
        mes("You water the ${state.recipe.displayName}.")
    }

    private fun ProtectedAccess.inspectEmptyPatch(type: ObjectServerType) {
        when {
            FarmingDefinitions.weedProgression.containsKey(type.id) -> mes("This patch needs weeding.")
            patchKind(type.id) != null -> mes("This patch is empty.")
            else -> mes("Nothing interesting happens.")
        }
    }

    private fun ProtectedAccess.inspectGrowingPatch(state: FarmingPatchState) {
        val remaining = state.secondsUntilReady()
        val singular = state.recipe.isSingularDisplay()
        val verb = if (singular) "is" else "are"
        val pronoun = if (singular) "It" else "They"
        if (remaining <= 0) {
            if (state.recipe.harvestMode == FarmingHarvestMode.CheckHealth) {
                mes("The ${state.recipe.displayName} $verb ready to check.")
            } else {
                mes("The ${state.recipe.displayName} $verb ready to harvest.")
            }
        } else {
            mes("The ${state.recipe.displayName} $verb growing. $pronoun should be ready soon.")
        }
    }

    private fun FarmingRecipe.isSingularDisplay(): Boolean =
        harvestMode == FarmingHarvestMode.CheckHealth ||
            !displayName.endsWith("s") ||
            displayName.endsWith("cactus")

    private fun ProtectedAccess.patchState(loc: BoundLocInfo): FarmingPatchState? {
        val key = patchKey(loc.coords)
        val recipeIndex = player.attr[FARMING_PATCH_RECIPE_ATTR]?.get(key) ?: return null
        val recipe = FarmingDefinitions.recipeByIndex[recipeIndex] ?: return null
        val plantedAt = player.attr[FARMING_PATCH_PLANTED_AT_ATTR]?.get(key) ?: currentEpochSeconds()
        val healthChecked = player.attr[FARMING_PATCH_HEALTH_CHECKED_ATTR]?.get(key) != null
        return FarmingPatchState(recipe, plantedAt, healthChecked)
    }

    private fun ProtectedAccess.isReady(state: FarmingPatchState): Boolean =
        currentEpochSeconds() - state.plantedAt >= state.recipe.growSeconds

    private fun ProtectedAccess.patchKind(loc: BoundLocInfo): FarmingPatchKind? =
        patchKind(loc.id) ?: patchState(loc)?.recipe?.kind

    private fun patchKind(locId: Int): FarmingPatchKind? =
        FarmingDefinitions.cleanLocKind[locId] ?: FarmingDefinitions.weedCleanKind[locId]

    private fun ProtectedAccess.clearState(key: String) {
        player.attr[FARMING_PATCH_RECIPE_ATTR]?.remove(key)
        player.attr[FARMING_PATCH_PLANTED_AT_ATTR]?.remove(key)
        player.attr[FARMING_PATCH_HARVESTS_LEFT_ATTR]?.remove(key)
        player.attr[FARMING_PATCH_COMPOST_ATTR]?.remove(key)
        player.attr[FARMING_PATCH_WATERED_ATTR]?.remove(key)
        player.attr[FARMING_PATCH_HEALTH_CHECKED_ATTR]?.remove(key)
    }

    private fun ProtectedAccess.rollYield(recipe: FarmingRecipe, key: String): Int {
        val compostLevel = player.attr[FARMING_PATCH_COMPOST_ATTR]?.get(key) ?: 0
        val minYield = recipe.minYield + compostLevel
        val maxYield = recipe.maxYield + (compostLevel * 2)
        return random.of(minYield, maxYield)
    }

    private fun FarmingPatchState.secondsUntilReady(): Int =
        (recipe.growSeconds - (currentEpochSeconds() - plantedAt)).coerceAtLeast(0)

    private fun patchKey(coords: CoordGrid): String = coords.packed.toString()

    private fun currentEpochSeconds(): Int =
        (System.currentTimeMillis() / 1000L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

    private fun locType(id: Int): ObjectServerType =
        locTypeOrNull(id) ?: error("Missing farming loc: $id")

    private fun locTypeOrNull(id: Int): ObjectServerType? = ServerCacheManager.getObject(id)

    private fun wateringCanAfterUse(charges: Int): String =
        "obj.watering_can_${(charges - 1).coerceAtLeast(0)}"

    private fun ObjectServerType.optionIndex(option: String): Int? {
        for (op in 1..5) {
            val text = actions.getOpOrNull(op - 1) ?: continue
            if (text.equals(option, ignoreCase = true)) {
                return op
            }
        }
        return null
    }

    private data class FarmingPatchState(
        val recipe: FarmingRecipe,
        val plantedAt: Int,
        val healthChecked: Boolean,
    )

    private companion object {
        private const val RAKE_ANIM = "seq.farming_rake"
        private const val PLANT_ANIM = "seq.human_generic_middletake"
        private const val PICK_ANIM = "seq.human_pickupfloor"
        private const val DIG_ANIM = "seq.human_dig_spade"
        private const val COMPOST_ANIM = "seq.farming_ingredient_sprinkle"
        private const val WATER_ANIM = "seq.farming_ingredient_sprinkle"
        private const val WEEDS = "obj.weeds"
        private const val RAKE_XP = 4.0
    }
}
