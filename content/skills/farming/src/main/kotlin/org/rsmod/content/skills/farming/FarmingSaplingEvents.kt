package org.rsmod.content.skills.farming

import org.rsmod.api.config.Constants
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeldU
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FarmingSaplingEvents : PluginScript() {
    override fun ScriptContext.startup() {
        FarmingDefinitions.saplingRecipes.forEach { recipe ->
            onOpHeldU(FarmingDefinitions.PLANT_POT_EMPTY, recipe.seed) { potSeed(recipe) }
            FarmingDefinitions.wateringCans.forEach { (wateringCan, charges) ->
                onOpHeldU(wateringCan, recipe.pottedSeed) { waterPottedSeed(recipe, wateringCan, charges) }
            }
        }
    }

    private suspend fun ProtectedAccess.potSeed(recipe: FarmingSaplingRecipe) {
        if (!inv.contains(FarmingDefinitions.GARDENING_TROWEL)) {
            mes("You need a gardening trowel to plant this seed.")
            return
        }
        if (!inv.contains(FarmingDefinitions.PLANT_POT_EMPTY) || !inv.contains(recipe.seed)) {
            mes(Constants.dm_default)
            return
        }

        anim(POT_SEED_ANIM)
        delay(1)

        if (invDel(inv, FarmingDefinitions.PLANT_POT_EMPTY, 1).failure) {
            return
        }
        if (invDel(inv, recipe.seed, 1).failure) {
            invAdd(inv, FarmingDefinitions.PLANT_POT_EMPTY, 1)
            return
        }
        if (invAdd(inv, recipe.pottedSeed, 1).failure) {
            invAdd(inv, FarmingDefinitions.PLANT_POT_EMPTY, 1)
            invAdd(inv, recipe.seed, 1)
            return
        }

        mes("You plant the seed in the plant pot.")
    }

    private suspend fun ProtectedAccess.waterPottedSeed(
        recipe: FarmingSaplingRecipe,
        wateringCan: String,
        charges: Int,
    ) {
        if (!inv.contains(wateringCan) || !inv.contains(recipe.pottedSeed)) {
            mes(Constants.dm_default)
            return
        }

        anim(WATER_ANIM)
        delay(1)

        if (invDel(inv, wateringCan, 1).failure) {
            return
        }
        if (invDel(inv, recipe.pottedSeed, 1).failure) {
            invAdd(inv, wateringCan, 1)
            return
        }

        invAdd(inv, wateringCanAfterUse(charges), 1)
        if (invAdd(inv, recipe.sapling, 1).failure) {
            invDel(inv, wateringCanAfterUse(charges), 1)
            invAdd(inv, wateringCan, 1)
            invAdd(inv, recipe.pottedSeed, 1)
            return
        }

        mes("You water the seedling. It grows into a sapling.")
    }

    private fun wateringCanAfterUse(charges: Int): String =
        "obj.watering_can_${(charges - 1).coerceAtLeast(0)}"

    private companion object {
        private const val POT_SEED_ANIM = "seq.human_generic_middletake"
        private const val WATER_ANIM = "seq.farming_ingredient_sprinkle"
    }
}
