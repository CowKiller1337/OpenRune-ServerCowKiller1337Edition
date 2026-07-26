package org.rsmod.content.skills.crafting

import jakarta.inject.Inject
import org.rsmod.api.config.Constants
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.skills.Material
import org.rsmod.content.skills.SkillMultiConfig
import org.rsmod.content.skills.SkillMultiEntry
import org.rsmod.content.skills.SkillingActionType
import org.rsmod.content.skills.openSkillMulti
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class GemCuttingEvents @Inject constructor(private val xpMods: XpModifiers) : PluginScript() {

    override fun ScriptContext.startup() {
        CraftingDefinitions.gemCuttingRecipes.forEach { recipe ->
            onOpHeldU(CraftingDefinitions.CHISEL, recipe.uncut) { promptGemCutting(recipe) }
        }

        onPlayerQueueWithArgs<GemCuttingTask>("queue.crafting_gem_cut") {
            processGemCutting(it.args)
        }
    }

    private suspend fun ProtectedAccess.promptGemCutting(recipe: GemCuttingRecipe) {
        if (!meetsCraftingLevel(recipe.level, "cut this gem")) {
            return
        }
        if (!inv.contains(CraftingDefinitions.CHISEL) || !inv.contains(recipe.uncut)) {
            mes(Constants.dm_default)
            return
        }

        openSkillMulti(
            SkillMultiConfig(
                actionType = SkillingActionType.CUT,
                verb = "cut",
                entries = listOf(
                    SkillMultiEntry(
                        recipe.cut,
                        listOf(Material(recipe.uncut)),
                    ),
                ),
            ),
        ) { selection ->
            weakQueue(
                "queue.crafting_gem_cut",
                1,
                GemCuttingTask(recipe, selection.amount, completed = 0),
            )
        }
    }

    private suspend fun ProtectedAccess.processGemCutting(task: GemCuttingTask) {
        val recipe = task.recipe
        if (!meetsCraftingLevel(recipe.level, "cut this gem")) {
            resetAnim()
            return
        }
        if (!inv.contains(CraftingDefinitions.CHISEL) || !inv.contains(recipe.uncut)) {
            resetAnim()
            return
        }

        anim("seq.human_cutting")

        if (invDel(inv, recipe.uncut, 1).failure) {
            resetAnim()
            return
        }
        if (invAdd(inv, recipe.cut, 1).failure) {
            invAdd(inv, recipe.uncut, 1)
            mes("You don't have enough inventory space.")
            resetAnim()
            return
        }

        advanceCraftingXp(recipe.xp, xpMods.get(player, "stat.crafting"))
        mes("You cut the gem.")

        val completed = task.completed + 1
        if (completed < task.amount && inv.contains(recipe.uncut)) {
            weakQueue(
                "queue.crafting_gem_cut",
                3,
                task.copy(completed = completed),
            )
        } else {
            resetAnim()
        }
    }

    private data class GemCuttingTask(
        val recipe: GemCuttingRecipe,
        val amount: Int,
        val completed: Int,
    )
}
