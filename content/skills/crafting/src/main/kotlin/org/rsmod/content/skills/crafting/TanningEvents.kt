package org.rsmod.content.skills.crafting

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc2
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.content.skills.SkillMultiConfig
import org.rsmod.content.skills.SkillMultiEntry
import org.rsmod.content.skills.SkillingActionType
import org.rsmod.content.skills.openSkillMulti
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class TanningEvents : PluginScript() {

    override fun ScriptContext.startup() {
        CraftingDefinitions.tannerNpcs.forEach { npc ->
            onOpNpc2(npc) { promptTanning() }
            onOpNpc3(npc) { promptTanning() }
        }

        onPlayerQueueWithArgs<TanningTask>("queue.crafting_tanning") {
            processTanning(it.args)
        }
    }

    private suspend fun ProtectedAccess.promptTanning() {
        val recipesWithHides = CraftingDefinitions.tanningRecipes.filter { inv.contains(it.input) }
        if (recipesWithHides.isEmpty()) {
            mes("You don't have any hides to tan.")
            return
        }

        val affordable = recipesWithHides.filter { inv.count(CraftingDefinitions.COINS) >= it.coinCost }
        if (affordable.isEmpty()) {
            mes("You don't have enough coins to tan that.")
            return
        }

        openSkillMulti(
            SkillMultiConfig(
                actionType = SkillingActionType.MAKE,
                verb = "tan",
                entries =
                    affordable.map { recipe ->
                        SkillMultiEntry(
                            recipe.output,
                            CraftingDefinitions.tanningMaterials(recipe),
                        )
                    },
                maxCountProvider = { inventory, entry ->
                    val recipe = affordable.firstOrNull { it.output == entry.internal }
                    if (recipe == null) {
                        0
                    } else {
                        minOf(
                            inventory.count(recipe.input),
                            inventory.count(CraftingDefinitions.COINS) / recipe.coinCost,
                        )
                    }
                },
            ),
        ) { selection ->
            val recipe = affordable.firstOrNull { it.output == selection.entry.internal }
                ?: return@openSkillMulti
            weakQueue("queue.crafting_tanning", 1, TanningTask(recipe, selection.amount, completed = 0))
        }
    }

    private suspend fun ProtectedAccess.processTanning(task: TanningTask) {
        val recipe = task.recipe
        if (inv.count(recipe.input) < 1) {
            resetAnim()
            return
        }
        if (inv.count(CraftingDefinitions.COINS) < recipe.coinCost) {
            mes("You don't have enough coins to tan that.")
            resetAnim()
            return
        }

        if (invDel(inv, recipe.input, 1).failure) {
            resetAnim()
            return
        }
        if (invDel(inv, CraftingDefinitions.COINS, recipe.coinCost).failure) {
            invAdd(inv, recipe.input, 1)
            resetAnim()
            return
        }
        if (invAdd(inv, recipe.output, 1).failure) {
            invAdd(inv, recipe.input, 1)
            invAdd(inv, CraftingDefinitions.COINS, recipe.coinCost)
            mes("You don't have enough inventory space.")
            resetAnim()
            return
        }

        if (task.completed == 0) {
            mes("The tanner tans the hide.")
        }

        val completed = task.completed + 1
        if (
            completed < task.amount &&
            inv.count(recipe.input) >= 1 &&
            inv.count(CraftingDefinitions.COINS) >= recipe.coinCost
        ) {
            weakQueue("queue.crafting_tanning", 1, task.copy(completed = completed))
        } else {
            resetAnim()
        }
    }

    private data class TanningTask(
        val recipe: TanningRecipe,
        val amount: Int,
        val completed: Int,
    )
}
