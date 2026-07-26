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

class LeatherCraftingEvents @Inject constructor(private val xpMods: XpModifiers) : PluginScript() {

    override fun ScriptContext.startup() {
        CraftingDefinitions.leatherMaterials.forEach { material ->
            onOpHeldU(CraftingDefinitions.NEEDLE, material) { promptLeatherCrafting(material) }
        }

        onPlayerQueueWithArgs<LeatherCraftingTask>("queue.crafting_leather") {
            processLeatherCrafting(it.args)
        }
    }

    private suspend fun ProtectedAccess.promptLeatherCrafting(material: String) {
        if (!inv.contains(CraftingDefinitions.NEEDLE)) {
            mes("You need a needle to make this.")
            return
        }
        if (!inv.contains(CraftingDefinitions.THREAD)) {
            mes("You need some thread to make this.")
            return
        }

        val recipes =
            CraftingDefinitions
                .leatherRecipesFor(material)
                .filter { canMake(it) }

        if (recipes.isEmpty()) {
            val lowestLevel =
                CraftingDefinitions
                    .leatherRecipesFor(material)
                    .minOfOrNull { it.level }
            if (lowestLevel != null && player.craftingLvl < lowestLevel) {
                mesbox("You need a Crafting level of $lowestLevel to make anything with this.")
            } else {
                mes(Constants.dm_default)
            }
            return
        }

        openSkillMulti(
            SkillMultiConfig(
                actionType = SkillingActionType.MAKE,
                verb = "make",
                entries =
                    recipes.map { recipe ->
                        SkillMultiEntry(
                            recipe.output,
                            CraftingDefinitions.leatherMaterials(recipe),
                        )
                    },
                maxCountProvider = { inventory, entry ->
                    val recipe = recipes.firstOrNull { it.output == entry.internal }
                    if (recipe == null) {
                        0
                    } else {
                        recipe.maxAmount(
                            inventory.count(recipe.material),
                            inventory.count(CraftingDefinitions.THREAD),
                        )
                    }
                },
            ),
        ) { selection ->
            val recipe = recipes.firstOrNull { it.output == selection.entry.internal } ?: return@openSkillMulti
            weakQueue(
                "queue.crafting_leather",
                1,
                LeatherCraftingTask(recipe, selection.amount, completed = 0),
            )
        }
    }

    private suspend fun ProtectedAccess.processLeatherCrafting(task: LeatherCraftingTask) {
        val recipe = task.recipe
        if (!canMake(recipe, showMessages = true)) {
            resetAnim()
            return
        }

        anim("seq.human_cutting")

        if (invDel(inv, recipe.material, recipe.materialCount).failure) {
            resetAnim()
            return
        }
        if (invDel(inv, CraftingDefinitions.THREAD, 1).failure) {
            invAdd(inv, recipe.material, recipe.materialCount)
            resetAnim()
            return
        }
        if (invAdd(inv, recipe.output, 1).failure) {
            invAdd(inv, recipe.material, recipe.materialCount)
            invAdd(inv, CraftingDefinitions.THREAD, 1)
            mes("You don't have enough inventory space.")
            resetAnim()
            return
        }

        advanceCraftingXp(recipe.xp, xpMods.get(player, "stat.crafting"))
        mes("You make the item.")

        val completed = task.completed + 1
        if (completed < task.amount && canMake(recipe)) {
            weakQueue(
                "queue.crafting_leather",
                3,
                task.copy(completed = completed),
            )
        } else {
            resetAnim()
        }
    }

    private suspend fun ProtectedAccess.canMake(
        recipe: LeatherCraftingRecipe,
        showMessages: Boolean = false,
    ): Boolean {
        if (player.craftingLvl < recipe.level) {
            if (showMessages) {
                mesbox("You need a Crafting level of ${recipe.level} to make this.")
            }
            return false
        }
        if (!inv.contains(CraftingDefinitions.NEEDLE)) {
            if (showMessages) {
                mes("You need a needle to make this.")
            }
            return false
        }
        if (inv.count(recipe.material) < recipe.materialCount) {
            if (showMessages) {
                mes(Constants.dm_default)
            }
            return false
        }
        if (!inv.contains(CraftingDefinitions.THREAD)) {
            if (showMessages) {
                mes("You need some thread to make this.")
            }
            return false
        }
        return true
    }

    private fun LeatherCraftingRecipe.maxAmount(materials: Int, thread: Int): Int =
        minOf(materials / materialCount, thread)

    private data class LeatherCraftingTask(
        val recipe: LeatherCraftingRecipe,
        val amount: Int,
        val completed: Int,
    )
}
