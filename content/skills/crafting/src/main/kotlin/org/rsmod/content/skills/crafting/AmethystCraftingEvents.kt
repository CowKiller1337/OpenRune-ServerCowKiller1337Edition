package org.rsmod.content.skills.crafting

import jakarta.inject.Inject
import org.rsmod.api.config.Constants
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.craftingLvl
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.skills.SkillMultiConfig
import org.rsmod.content.skills.SkillMultiEntry
import org.rsmod.content.skills.SkillingActionType
import org.rsmod.content.skills.openSkillMulti
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class AmethystCraftingEvents @Inject constructor(private val xpMods: XpModifiers) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpHeldU(CraftingDefinitions.CHISEL, CraftingDefinitions.AMETHYST) {
            promptAmethystCrafting()
        }

        onPlayerQueueWithArgs<AmethystCraftingTask>("queue.crafting_amethyst") {
            processAmethystCrafting(it.args)
        }
    }

    private suspend fun ProtectedAccess.promptAmethystCrafting() {
        if (!inv.contains(CraftingDefinitions.CHISEL) || !inv.contains(CraftingDefinitions.AMETHYST)) {
            mes(Constants.dm_default)
            return
        }

        val available = CraftingDefinitions.amethystRecipes.filter { player.craftingLvl >= it.level }
        if (available.isEmpty()) {
            val level = CraftingDefinitions.amethystRecipes.minOf { it.level }
            mesbox("You need a Crafting level of $level to cut this amethyst.")
            return
        }

        openSkillMulti(
            SkillMultiConfig(
                actionType = SkillingActionType.CUT,
                verb = "cut",
                entries =
                    available.map { recipe ->
                        SkillMultiEntry(
                            recipe.output,
                            CraftingDefinitions.amethystMaterials(),
                        )
                    },
            ),
        ) { selection ->
            val recipe = available.firstOrNull { it.output == selection.entry.internal }
                ?: return@openSkillMulti
            weakQueue("queue.crafting_amethyst", 1, AmethystCraftingTask(recipe, selection.amount, completed = 0))
        }
    }

    private suspend fun ProtectedAccess.processAmethystCrafting(task: AmethystCraftingTask) {
        val recipe = task.recipe
        if (!meetsCraftingLevel(recipe.level, "cut this amethyst")) {
            resetAnim()
            return
        }
        if (!inv.contains(CraftingDefinitions.CHISEL) || !inv.contains(CraftingDefinitions.AMETHYST)) {
            resetAnim()
            return
        }

        anim("seq.human_cutting")

        if (invDel(inv, CraftingDefinitions.AMETHYST, 1).failure) {
            resetAnim()
            return
        }
        if (invAdd(inv, recipe.output, recipe.outputCount).failure) {
            invAdd(inv, CraftingDefinitions.AMETHYST, 1)
            mes("You don't have enough inventory space.")
            resetAnim()
            return
        }

        advanceCraftingXp(recipe.xp, xpMods.get(player, "stat.crafting"))
        mes("You cut the amethyst.")

        val completed = task.completed + 1
        if (completed < task.amount && inv.contains(CraftingDefinitions.AMETHYST)) {
            weakQueue("queue.crafting_amethyst", 3, task.copy(completed = completed))
        } else {
            resetAnim()
        }
    }

    private data class AmethystCraftingTask(
        val recipe: AmethystCraftingRecipe,
        val amount: Int,
        val completed: Int,
    )
}
