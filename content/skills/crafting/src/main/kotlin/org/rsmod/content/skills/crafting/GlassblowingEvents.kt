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

class GlassblowingEvents @Inject constructor(private val xpMods: XpModifiers) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpHeldU(CraftingDefinitions.GLASSBLOWING_PIPE, CraftingDefinitions.MOLTEN_GLASS) {
            promptGlassblowing()
        }

        onPlayerQueueWithArgs<GlassblowingTask>("queue.crafting_glassblowing") {
            processGlassblowing(it.args)
        }
    }

    private suspend fun ProtectedAccess.promptGlassblowing() {
        if (!inv.contains(CraftingDefinitions.GLASSBLOWING_PIPE) || !inv.contains(CraftingDefinitions.MOLTEN_GLASS)) {
            mes(Constants.dm_default)
            return
        }

        val available = CraftingDefinitions.glassblowingRecipes.filter { player.craftingLvl >= it.level }
        if (available.isEmpty()) {
            val level = CraftingDefinitions.glassblowingRecipes.minOf { it.level }
            mesbox("You need a Crafting level of $level to make anything with this.")
            return
        }

        openSkillMulti(
            SkillMultiConfig(
                actionType = SkillingActionType.MAKE,
                verb = "make",
                entries =
                    available.map { recipe ->
                        SkillMultiEntry(recipe.output, CraftingDefinitions.glassblowingMaterials())
                    },
                maxCountProvider = { inventory, _ -> inventory.count(CraftingDefinitions.MOLTEN_GLASS) },
            ),
        ) { selection ->
            val recipe = available.firstOrNull { it.output == selection.entry.internal } ?: return@openSkillMulti
            weakQueue(
                "queue.crafting_glassblowing",
                1,
                GlassblowingTask(recipe, selection.amount, completed = 0),
            )
        }
    }

    private suspend fun ProtectedAccess.processGlassblowing(task: GlassblowingTask) {
        val recipe = task.recipe
        if (!canMake(recipe)) {
            resetAnim()
            return
        }

        anim("seq.human_cutting")
        if (invDel(inv, CraftingDefinitions.MOLTEN_GLASS, 1).failure) {
            resetAnim()
            return
        }
        if (invAdd(inv, recipe.output, 1).failure) {
            invAdd(inv, CraftingDefinitions.MOLTEN_GLASS, 1)
            mes("You don't have enough inventory space.")
            resetAnim()
            return
        }

        advanceCraftingXp(recipe.xp, xpMods.get(player, "stat.crafting"))
        mes("You make the glass item.")

        val completed = task.completed + 1
        if (completed < task.amount && canMake(recipe, showMessages = false)) {
            weakQueue(
                "queue.crafting_glassblowing",
                3,
                task.copy(completed = completed),
            )
        } else {
            resetAnim()
        }
    }

    private suspend fun ProtectedAccess.canMake(
        recipe: GlassblowingRecipe,
        showMessages: Boolean = true,
    ): Boolean {
        if (player.craftingLvl < recipe.level) {
            if (showMessages) {
                mesbox("You need a Crafting level of ${recipe.level} to make this.")
            }
            return false
        }
        if (!inv.contains(CraftingDefinitions.GLASSBLOWING_PIPE) || !inv.contains(CraftingDefinitions.MOLTEN_GLASS)) {
            if (showMessages) {
                mes(Constants.dm_default)
            }
            return false
        }
        return true
    }

    private data class GlassblowingTask(
        val recipe: GlassblowingRecipe,
        val amount: Int,
        val completed: Int,
    )
}
