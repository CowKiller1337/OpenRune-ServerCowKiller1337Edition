package org.rsmod.content.skills.crafting

import jakarta.inject.Inject
import org.rsmod.api.config.Constants
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.craftingLvl
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpLocCategoryU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.skills.SkillMultiConfig
import org.rsmod.content.skills.SkillMultiEntry
import org.rsmod.content.skills.SkillingActionType
import org.rsmod.content.skills.openSkillMulti
import org.rsmod.game.inv.Inventory
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class JewelleryCraftingEvents @Inject constructor(private val xpMods: XpModifiers) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpLocCategoryU("category.furnace", CraftingDefinitions.GOLD_BAR) {
            promptJewellery(bar = CraftingDefinitions.GOLD_BAR)
        }
        onOpLocCategoryU("category.furnace", CraftingDefinitions.SILVER_BAR) {
            promptJewellery(bar = CraftingDefinitions.SILVER_BAR)
        }

        CraftingDefinitions.jewelleryMoulds.forEach { mould ->
            onOpLocCategoryU("category.furnace", mould) { promptJewellery(mould = mould) }
        }

        CraftingDefinitions.jewelleryStringingRecipes.forEach { recipe ->
            onOpHeldU(CraftingDefinitions.BALL_OF_WOOL, recipe.input) {
                promptStringing(recipe)
            }
        }

        onPlayerQueueWithArgs<JewelleryCraftingTask>("queue.crafting_jewellery") {
            processJewellery(it.args)
        }
        onPlayerQueueWithArgs<JewelleryStringingTask>("queue.crafting_jewellery_stringing") {
            processStringing(it.args)
        }
    }

    private suspend fun ProtectedAccess.promptJewellery(
        mould: String? = null,
        bar: String? = null,
    ) {
        val recipes = CraftingDefinitions.jewelleryRecipesFor(mould, bar)
        val possible = recipes.filter { hasMaterials(it) }
        if (possible.isEmpty()) {
            mes(Constants.dm_default)
            return
        }

        val available = possible.filter { player.craftingLvl >= it.level }
        if (available.isEmpty()) {
            val level = possible.minOf { it.level }
            mesbox("You need a Crafting level of $level to make this.")
            return
        }

        openSkillMulti(
            SkillMultiConfig(
                actionType = SkillingActionType.MAKE,
                verb = "make",
                entries =
                    available.map { recipe ->
                        SkillMultiEntry(
                            recipe.output,
                            CraftingDefinitions.jewelleryMaterials(recipe),
                        )
                    },
                maxCountProvider = { inventory, entry ->
                    val recipe = available.firstOrNull { it.output == entry.internal }
                    recipe?.let { maxJewelleryCount(inventory, it) } ?: 0
                },
            ),
        ) { selection ->
            val recipe = available.firstOrNull { it.output == selection.entry.internal }
                ?: return@openSkillMulti
            weakQueue(
                "queue.crafting_jewellery",
                1,
                JewelleryCraftingTask(recipe, selection.amount, completed = 0),
            )
        }
    }

    private suspend fun ProtectedAccess.processJewellery(task: JewelleryCraftingTask) {
        val recipe = task.recipe
        if (!canCraft(recipe)) {
            resetAnim()
            return
        }

        anim("seq.human_furnace")
        soundSynth("synth.furnace")
        delay(2)

        if (invDel(inv, recipe.bar, 1).failure) {
            resetAnim()
            return
        }
        if (recipe.gem != null && invDel(inv, recipe.gem, 1).failure) {
            invAdd(inv, recipe.bar, 1)
            resetAnim()
            return
        }
        if (invAdd(inv, recipe.output, 1).failure) {
            invAdd(inv, recipe.bar, 1)
            recipe.gem?.let { invAdd(inv, it, 1) }
            mes("You don't have enough inventory space.")
            resetAnim()
            return
        }

        advanceCraftingXp(recipe.xp, xpMods.get(player, "stat.crafting"))
        mes("You craft the jewellery.")

        val completed = task.completed + 1
        if (completed < task.amount && canCraft(recipe, showMessages = false)) {
            weakQueue(
                "queue.crafting_jewellery",
                3,
                task.copy(completed = completed),
            )
        } else {
            resetAnim()
        }
    }

    private suspend fun ProtectedAccess.promptStringing(recipe: JewelleryStringingRecipe) {
        if (!canString(recipe)) {
            return
        }

        openSkillMulti(
            SkillMultiConfig(
                actionType = SkillingActionType.STRING,
                verb = "string",
                entries =
                    listOf(
                        SkillMultiEntry(
                            recipe.output,
                            CraftingDefinitions.jewelleryStringingMaterials(recipe),
                        ),
                    ),
                maxCountProvider = { inventory, _ ->
                    minOf(
                        inventory.count(recipe.input),
                        inventory.count(CraftingDefinitions.BALL_OF_WOOL),
                    )
                },
            ),
        ) { selection ->
            weakQueue(
                "queue.crafting_jewellery_stringing",
                1,
                JewelleryStringingTask(recipe, selection.amount, completed = 0),
            )
        }
    }

    private suspend fun ProtectedAccess.processStringing(task: JewelleryStringingTask) {
        val recipe = task.recipe
        if (!canString(recipe)) {
            resetAnim()
            return
        }

        anim("seq.human_string_bow")
        if (invDel(inv, recipe.input, 1).failure) {
            resetAnim()
            return
        }
        if (invDel(inv, CraftingDefinitions.BALL_OF_WOOL, 1).failure) {
            invAdd(inv, recipe.input, 1)
            resetAnim()
            return
        }
        if (invAdd(inv, recipe.output, 1).failure) {
            invAdd(inv, recipe.input, 1)
            invAdd(inv, CraftingDefinitions.BALL_OF_WOOL, 1)
            mes("You don't have enough inventory space.")
            resetAnim()
            return
        }

        advanceCraftingXp(recipe.xp, xpMods.get(player, "stat.crafting"))
        mes("You string the jewellery.")

        val completed = task.completed + 1
        if (completed < task.amount && canString(recipe, showMessages = false)) {
            weakQueue(
                "queue.crafting_jewellery_stringing",
                3,
                task.copy(completed = completed),
            )
        } else {
            resetAnim()
        }
    }

    private suspend fun ProtectedAccess.canCraft(
        recipe: JewelleryCraftingRecipe,
        showMessages: Boolean = true,
    ): Boolean {
        if (player.craftingLvl < recipe.level) {
            if (showMessages) {
                mesbox("You need a Crafting level of ${recipe.level} to make this.")
            }
            return false
        }
        if (!inv.contains(recipe.mould)) {
            if (showMessages) {
                mesbox("You need the correct mould to make this.")
            }
            return false
        }
        if (!hasMaterials(recipe)) {
            if (showMessages) {
                mes(Constants.dm_default)
            }
            return false
        }
        return true
    }

    private suspend fun ProtectedAccess.canString(
        recipe: JewelleryStringingRecipe,
        showMessages: Boolean = true,
    ): Boolean {
        if (player.craftingLvl < recipe.level) {
            if (showMessages) {
                mesbox("You need a Crafting level of ${recipe.level} to string this.")
            }
            return false
        }
        if (!inv.contains(recipe.input) || !inv.contains(CraftingDefinitions.BALL_OF_WOOL)) {
            if (showMessages) {
                mes(Constants.dm_default)
            }
            return false
        }
        return true
    }

    private fun ProtectedAccess.hasMaterials(recipe: JewelleryCraftingRecipe): Boolean {
        return inv.contains(recipe.mould) &&
            inv.contains(recipe.bar) &&
            (recipe.gem == null || inv.contains(recipe.gem))
    }

    private fun maxJewelleryCount(inventory: Inventory, recipe: JewelleryCraftingRecipe): Int {
        if (inventory.count(recipe.mould) <= 0) {
            return 0
        }
        val gemCount = recipe.gem?.let { inventory.count(it) } ?: Int.MAX_VALUE
        return minOf(inventory.count(recipe.bar), gemCount)
    }

    private data class JewelleryCraftingTask(
        val recipe: JewelleryCraftingRecipe,
        val amount: Int,
        val completed: Int,
    )

    private data class JewelleryStringingTask(
        val recipe: JewelleryStringingRecipe,
        val amount: Int,
        val completed: Int,
    )
}
