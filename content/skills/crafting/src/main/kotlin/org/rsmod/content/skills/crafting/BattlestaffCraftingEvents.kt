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

class BattlestaffCraftingEvents @Inject constructor(private val xpMods: XpModifiers) : PluginScript() {

    override fun ScriptContext.startup() {
        CraftingDefinitions.battlestaffRecipes.forEach { recipe ->
            onOpHeldU(CraftingDefinitions.BATTLESTAFF, recipe.orb) { promptBattlestaff(recipe) }
        }

        onPlayerQueueWithArgs<BattlestaffTask>("queue.crafting_battlestaff") {
            processBattlestaff(it.args)
        }
    }

    private suspend fun ProtectedAccess.promptBattlestaff(recipe: BattlestaffRecipe) {
        if (!canMake(recipe)) {
            return
        }

        openSkillMulti(
            SkillMultiConfig(
                actionType = SkillingActionType.MAKE,
                verb = "make",
                entries =
                    listOf(
                        SkillMultiEntry(
                            recipe.output,
                            CraftingDefinitions.battlestaffMaterials(recipe),
                        ),
                    ),
                maxCountProvider = { inventory, _ ->
                    minOf(inventory.count(CraftingDefinitions.BATTLESTAFF), inventory.count(recipe.orb))
                },
            ),
        ) { selection ->
            weakQueue(
                "queue.crafting_battlestaff",
                1,
                BattlestaffTask(recipe, selection.amount, completed = 0),
            )
        }
    }

    private suspend fun ProtectedAccess.processBattlestaff(task: BattlestaffTask) {
        val recipe = task.recipe
        if (!canMake(recipe)) {
            resetAnim()
            return
        }

        anim("seq.human_cutting")
        if (invDel(inv, CraftingDefinitions.BATTLESTAFF, 1).failure) {
            resetAnim()
            return
        }
        if (invDel(inv, recipe.orb, 1).failure) {
            invAdd(inv, CraftingDefinitions.BATTLESTAFF, 1)
            resetAnim()
            return
        }
        if (invAdd(inv, recipe.output, 1).failure) {
            invAdd(inv, CraftingDefinitions.BATTLESTAFF, 1)
            invAdd(inv, recipe.orb, 1)
            mes("You don't have enough inventory space.")
            resetAnim()
            return
        }

        advanceCraftingXp(recipe.xp, xpMods.get(player, "stat.crafting"))
        mes("You attach the orb to the battlestaff.")

        val completed = task.completed + 1
        if (completed < task.amount && canMake(recipe, showMessages = false)) {
            weakQueue(
                "queue.crafting_battlestaff",
                3,
                task.copy(completed = completed),
            )
        } else {
            resetAnim()
        }
    }

    private suspend fun ProtectedAccess.canMake(
        recipe: BattlestaffRecipe,
        showMessages: Boolean = true,
    ): Boolean {
        if (player.craftingLvl < recipe.level) {
            if (showMessages) {
                mesbox("You need a Crafting level of ${recipe.level} to make this.")
            }
            return false
        }
        if (!inv.contains(CraftingDefinitions.BATTLESTAFF) || !inv.contains(recipe.orb)) {
            if (showMessages) {
                mes(Constants.dm_default)
            }
            return false
        }
        return true
    }

    private data class BattlestaffTask(
        val recipe: BattlestaffRecipe,
        val amount: Int,
        val completed: Int,
    )
}
