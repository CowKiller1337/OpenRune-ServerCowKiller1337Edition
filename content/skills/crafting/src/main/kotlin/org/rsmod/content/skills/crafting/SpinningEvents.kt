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

class SpinningEvents @Inject constructor(private val xpMods: XpModifiers) : PluginScript() {

    override fun ScriptContext.startup() {
        CraftingDefinitions.spinningWheelLocIds.forEach { locId ->
            onProtectedEvent<LocEvents.Op1>(locId) { promptSpinning() }
        }

        onPlayerQueueWithArgs<SpinningTask>("queue.crafting_spinning") {
            processSpinning(it.args)
        }
    }

    private suspend fun ProtectedAccess.promptSpinning() {
        val possible = CraftingDefinitions.spinningRecipes.filter { inv.contains(it.input) }
        if (possible.isEmpty()) {
            mes(Constants.dm_default)
            return
        }

        val available = possible.filter { player.craftingLvl >= it.level }
        if (available.isEmpty()) {
            val level = possible.minOf { it.level }
            mesbox("You need a Crafting level of $level to spin this.")
            return
        }

        openSkillMulti(
            SkillMultiConfig(
                actionType = SkillingActionType.MAKE,
                verb = "spin",
                entries =
                    available.map { recipe ->
                        SkillMultiEntry(recipe.output, CraftingDefinitions.spinningMaterials(recipe))
                    },
                maxCountProvider = { inventory, entry ->
                    val recipe = available.firstOrNull { it.output == entry.internal }
                    recipe?.let { inventory.count(it.input) } ?: 0
                },
            ),
        ) { selection ->
            val recipe = available.firstOrNull { it.output == selection.entry.internal } ?: return@openSkillMulti
            weakQueue(
                "queue.crafting_spinning",
                1,
                SpinningTask(recipe, selection.amount, completed = 0),
            )
        }
    }

    private suspend fun ProtectedAccess.processSpinning(task: SpinningTask) {
        val recipe = task.recipe
        if (!canSpin(recipe)) {
            resetAnim()
            return
        }

        anim("seq.human_cutting")
        if (invDel(inv, recipe.input, 1).failure) {
            resetAnim()
            return
        }
        if (invAdd(inv, recipe.output, 1).failure) {
            invAdd(inv, recipe.input, 1)
            mes("You don't have enough inventory space.")
            resetAnim()
            return
        }

        advanceCraftingXp(recipe.xp, xpMods.get(player, "stat.crafting"))
        mes("You spin the fibres.")

        val completed = task.completed + 1
        if (completed < task.amount && canSpin(recipe, showMessages = false)) {
            weakQueue(
                "queue.crafting_spinning",
                3,
                task.copy(completed = completed),
            )
        } else {
            resetAnim()
        }
    }

    private suspend fun ProtectedAccess.canSpin(
        recipe: SpinningRecipe,
        showMessages: Boolean = true,
    ): Boolean {
        if (player.craftingLvl < recipe.level) {
            if (showMessages) {
                mesbox("You need a Crafting level of ${recipe.level} to spin this.")
            }
            return false
        }
        if (!inv.contains(recipe.input)) {
            if (showMessages) {
                mes(Constants.dm_default)
            }
            return false
        }
        return true
    }

    private data class SpinningTask(
        val recipe: SpinningRecipe,
        val amount: Int,
        val completed: Int,
    )
}
