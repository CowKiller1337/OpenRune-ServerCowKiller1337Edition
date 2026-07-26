package org.rsmod.content.skills.crafting

import jakarta.inject.Inject
import org.rsmod.api.config.Constants
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.craftingLvl
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.script.onProtectedEvent
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.skills.SkillMultiConfig
import org.rsmod.content.skills.SkillMultiEntry
import org.rsmod.content.skills.SkillingActionType
import org.rsmod.content.skills.openSkillMulti
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PotteryEvents @Inject constructor(private val xpMods: XpModifiers) : PluginScript() {

    override fun ScriptContext.startup() {
        CraftingDefinitions.potteryWheelLocIds.forEach { locId ->
            onProtectedEvent<LocEvents.Op1>(locId) { promptShapePottery() }
        }
        CraftingDefinitions.potteryOvenLocIds.forEach { locId ->
            onProtectedEvent<LocEvents.Op1>(locId) { promptFirePottery() }
        }

        onPlayerQueueWithArgs<PotteryTask>("queue.crafting_pottery_shape") {
            processShapePottery(it.args)
        }
        onPlayerQueueWithArgs<PotteryTask>("queue.crafting_pottery_fire") {
            processFirePottery(it.args)
        }
    }

    private suspend fun ProtectedAccess.promptShapePottery() {
        if (!inv.contains(CraftingDefinitions.SOFT_CLAY)) {
            mes(Constants.dm_default)
            return
        }

        val available = CraftingDefinitions.potteryRecipes.filter { player.craftingLvl >= it.level }
        if (available.isEmpty()) {
            val level = CraftingDefinitions.potteryRecipes.minOf { it.level }
            mesbox("You need a Crafting level of $level to shape pottery.")
            return
        }

        openSkillMulti(
            SkillMultiConfig(
                actionType = SkillingActionType.MAKE,
                verb = "make",
                entries =
                    available.map { recipe ->
                        SkillMultiEntry(recipe.unfired, CraftingDefinitions.potteryShapeMaterials())
                    },
                maxCountProvider = { inventory, _ -> inventory.count(CraftingDefinitions.SOFT_CLAY) },
            ),
        ) { selection ->
            val recipe = available.firstOrNull { it.unfired == selection.entry.internal }
                ?: return@openSkillMulti
            weakQueue(
                "queue.crafting_pottery_shape",
                1,
                PotteryTask(recipe, selection.amount, completed = 0),
            )
        }
    }

    private suspend fun ProtectedAccess.promptFirePottery() {
        val possible = CraftingDefinitions.potteryRecipes.filter { inv.contains(it.unfired) }
        if (possible.isEmpty()) {
            mes(Constants.dm_default)
            return
        }

        val available = possible.filter { player.craftingLvl >= it.level }
        if (available.isEmpty()) {
            val level = possible.minOf { it.level }
            mesbox("You need a Crafting level of $level to fire this pottery.")
            return
        }

        openSkillMulti(
            SkillMultiConfig(
                actionType = SkillingActionType.FIRE,
                verb = "fire",
                entries =
                    available.map { recipe ->
                        SkillMultiEntry(recipe.fired, CraftingDefinitions.potteryFireMaterials(recipe))
                    },
                maxCountProvider = { inventory, entry ->
                    val recipe = available.firstOrNull { it.fired == entry.internal }
                    recipe?.let { inventory.count(it.unfired) } ?: 0
                },
            ),
        ) { selection ->
            val recipe = available.firstOrNull { it.fired == selection.entry.internal }
                ?: return@openSkillMulti
            weakQueue(
                "queue.crafting_pottery_fire",
                1,
                PotteryTask(recipe, selection.amount, completed = 0),
            )
        }
    }

    private suspend fun ProtectedAccess.processShapePottery(task: PotteryTask) {
        val recipe = task.recipe
        if (!canShape(recipe)) {
            resetAnim()
            return
        }

        anim(SHAPE_ANIM)
        delay(3)

        if (invDel(inv, CraftingDefinitions.SOFT_CLAY, 1).failure) {
            resetAnim()
            return
        }
        if (invAdd(inv, recipe.unfired, 1).failure) {
            invAdd(inv, CraftingDefinitions.SOFT_CLAY, 1)
            mes("You don't have enough inventory space.")
            resetAnim()
            return
        }

        advanceCraftingXp(recipe.shapeXp, xpMods.get(player, "stat.crafting"))
        mes("You shape the soft clay.")

        val completed = task.completed + 1
        if (completed < task.amount && canShape(recipe, showMessages = false)) {
            weakQueue(
                "queue.crafting_pottery_shape",
                3,
                task.copy(completed = completed),
            )
        } else {
            resetAnim()
        }
    }

    private suspend fun ProtectedAccess.processFirePottery(task: PotteryTask) {
        val recipe = task.recipe
        if (!canFire(recipe)) {
            resetAnim()
            return
        }

        anim(FIRE_ANIM)
        soundSynth("synth.furnace")
        delay(3)

        if (invDel(inv, recipe.unfired, 1).failure) {
            resetAnim()
            return
        }
        if (invAdd(inv, recipe.fired, 1).failure) {
            invAdd(inv, recipe.unfired, 1)
            mes("You don't have enough inventory space.")
            resetAnim()
            return
        }

        advanceCraftingXp(recipe.fireXp, xpMods.get(player, "stat.crafting"))
        mes("You fire the pottery in the oven.")

        val completed = task.completed + 1
        if (completed < task.amount && canFire(recipe, showMessages = false)) {
            weakQueue(
                "queue.crafting_pottery_fire",
                3,
                task.copy(completed = completed),
            )
        } else {
            resetAnim()
        }
    }

    private suspend fun ProtectedAccess.canShape(
        recipe: PotteryRecipe,
        showMessages: Boolean = true,
    ): Boolean {
        if (player.craftingLvl < recipe.level) {
            if (showMessages) {
                mesbox("You need a Crafting level of ${recipe.level} to make this.")
            }
            return false
        }
        if (!inv.contains(CraftingDefinitions.SOFT_CLAY)) {
            if (showMessages) {
                mes(Constants.dm_default)
            }
            return false
        }
        return true
    }

    private suspend fun ProtectedAccess.canFire(
        recipe: PotteryRecipe,
        showMessages: Boolean = true,
    ): Boolean {
        if (player.craftingLvl < recipe.level) {
            if (showMessages) {
                mesbox("You need a Crafting level of ${recipe.level} to fire this pottery.")
            }
            return false
        }
        if (!inv.contains(recipe.unfired)) {
            if (showMessages) {
                mes(Constants.dm_default)
            }
            return false
        }
        return true
    }

    private data class PotteryTask(
        val recipe: PotteryRecipe,
        val amount: Int,
        val completed: Int,
    )

    private companion object {
        private const val SHAPE_ANIM = "seq.human_cutting"
        private const val FIRE_ANIM = "seq.human_furnace"
    }
}
