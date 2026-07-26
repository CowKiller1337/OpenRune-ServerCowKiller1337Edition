package org.rsmod.content.skills.fletching

import jakarta.inject.Inject
import org.rsmod.api.config.Constants
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.fletchingLvl
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.skills.SkillMultiConfig
import org.rsmod.content.skills.SkillMultiEntry
import org.rsmod.content.skills.SkillingActionType
import org.rsmod.content.skills.openSkillMulti
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FletchingEvents @Inject constructor(private val xpMods: XpModifiers) : PluginScript() {

    override fun ScriptContext.startup() {
        FletchingDefinitions.logTypes.forEach { log ->
            onOpHeldU(FletchingDefinitions.KNIFE, log) { promptCarving(log) }
        }

        FletchingDefinitions.stringBowRecipes.forEach { recipe ->
            onOpHeldU(recipe.string, recipe.unstrung) { promptStringing(recipe) }
        }

        FletchingDefinitions.crossbowAssemblyRecipes.forEach { recipe ->
            onOpHeldU(recipe.stock, recipe.limbs) { promptCrossbowAssembly(recipe) }
        }

        FletchingDefinitions.ammoRecipes.forEach { recipe ->
            onOpHeldU(recipe.primary, recipe.secondary) { startAmmo(recipe) }
        }
        onOpHeldU(FletchingDefinitions.CHISEL, FletchingDefinitions.WOLF_BONES) {
            promptWolfboneArrowheads()
        }

        onPlayerQueueWithArgs<CarveTask>("queue.fletching_carve") { processCarving(it.args) }
        onPlayerQueueWithArgs<StringBowTask>("queue.fletching_string_bow") { processStringing(it.args) }
        onPlayerQueueWithArgs<CrossbowAssemblyTask>("queue.fletching_crossbow_assembly") {
            processCrossbowAssembly(it.args)
        }
        onPlayerQueueWithArgs<AmmoTask>("queue.fletching_ammo") { processAmmo(it.args) }
        onPlayerQueueWithArgs<WolfboneArrowheadsTask>("queue.fletching_wolfbone_arrowheads") {
            processWolfboneArrowheads(it.args)
        }
    }

    private suspend fun ProtectedAccess.promptCarving(log: String) {
        if (!inv.contains(FletchingDefinitions.KNIFE) || !inv.contains(log)) {
            mes(Constants.dm_default)
            return
        }

        val recipes = FletchingDefinitions.carveRecipesFor(log)
        val available = recipes.filter { player.fletchingLvl >= it.level }
        if (available.isEmpty()) {
            val level = recipes.minOfOrNull { it.level } ?: return
            mesbox("You need a Fletching level of $level to make anything with this.")
            return
        }

        openSkillMulti(
            SkillMultiConfig(
                actionType = SkillingActionType.CUT,
                verb = "make",
                entries =
                    available.map { recipe ->
                        SkillMultiEntry(
                            recipe.output,
                            FletchingDefinitions.materials(recipe),
                        )
                    },
            ),
        ) { selection ->
            val recipe = available.firstOrNull { it.output == selection.entry.internal } ?: return@openSkillMulti
            weakQueue("queue.fletching_carve", 1, CarveTask(recipe, selection.amount, completed = 0))
        }
    }

    private suspend fun ProtectedAccess.promptStringing(recipe: StringBowRecipe) {
        if (!meetsFletchingLevel(recipe.level, "string this bow")) {
            return
        }
        if (!inv.contains(recipe.string) || !inv.contains(recipe.unstrung)) {
            mes(Constants.dm_default)
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
                            FletchingDefinitions.materials(recipe),
                        ),
                    ),
            ),
        ) { selection ->
            weakQueue(
                "queue.fletching_string_bow",
                1,
                StringBowTask(recipe, selection.amount, completed = 0),
            )
        }
    }

    private suspend fun ProtectedAccess.promptCrossbowAssembly(recipe: CrossbowAssemblyRecipe) {
        if (!canAssembleCrossbow(recipe)) {
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
                            FletchingDefinitions.materials(recipe),
                        ),
                    ),
                maxCountProvider = { inventory, _ ->
                    minOf(inventory.count(recipe.stock), inventory.count(recipe.limbs))
                },
            ),
        ) { selection ->
            weakQueue(
                "queue.fletching_crossbow_assembly",
                1,
                CrossbowAssemblyTask(recipe, selection.amount, completed = 0),
            )
        }
    }

    private suspend fun ProtectedAccess.startAmmo(recipe: FletchAmmoRecipe) {
        if (!meetsFletchingLevel(recipe.level)) {
            return
        }
        if (recipe.tool != null && !inv.contains(recipe.tool)) {
            mes(Constants.dm_default)
            return
        }
        val batches = maxBatches(recipe)
        if (batches <= 0) {
            mes(Constants.dm_default)
            return
        }
        weakQueue("queue.fletching_ammo", 1, AmmoTask(recipe, batches, completed = 0))
    }

    private suspend fun ProtectedAccess.promptWolfboneArrowheads() {
        if (!meetsFletchingLevel(WOLFBONE_ARROWHEAD_LEVEL)) {
            return
        }
        if (!inv.contains(FletchingDefinitions.CHISEL) || !inv.contains(FletchingDefinitions.WOLF_BONES)) {
            mes(Constants.dm_default)
            return
        }

        openSkillMulti(
            SkillMultiConfig(
                actionType = SkillingActionType.CUT,
                verb = "make",
                entries =
                    listOf(
                        SkillMultiEntry(
                            FletchingDefinitions.WOLFBONE_ARROWHEADS,
                            FletchingDefinitions.wolfboneArrowheadMaterials(),
                        ),
                    ),
            ),
        ) { selection ->
            weakQueue(
                "queue.fletching_wolfbone_arrowheads",
                1,
                WolfboneArrowheadsTask(selection.amount, completed = 0),
            )
        }
    }

    private suspend fun ProtectedAccess.processCarving(task: CarveTask) {
        val recipe = task.recipe
        if (!meetsFletchingLevel(recipe.level)) {
            resetAnim()
            return
        }
        if (!inv.contains(FletchingDefinitions.KNIFE) || !inv.contains(recipe.log)) {
            resetAnim()
            return
        }

        anim("seq.human_cutting")
        if (invDel(inv, recipe.log, 1).failure) {
            resetAnim()
            return
        }
        if (invAdd(inv, recipe.output, recipe.outputCount).failure) {
            invAdd(inv, recipe.log, 1)
            mes("You don't have enough inventory space.")
            resetAnim()
            return
        }

        advanceFletchingXp(recipe.xp, xpMods.get(player, "stat.fletching"))
        mes("You carefully cut the wood.")

        val completed = task.completed + 1
        if (completed < task.amount && inv.contains(recipe.log)) {
            weakQueue("queue.fletching_carve", 3, task.copy(completed = completed))
        } else {
            resetAnim()
        }
    }

    private suspend fun ProtectedAccess.processStringing(task: StringBowTask) {
        val recipe = task.recipe
        if (!meetsFletchingLevel(recipe.level, "string this bow")) {
            resetAnim()
            return
        }
        if (!inv.contains(recipe.string) || !inv.contains(recipe.unstrung)) {
            resetAnim()
            return
        }

        anim("seq.human_string_bow")
        if (invDel(inv, recipe.unstrung, 1).failure) {
            resetAnim()
            return
        }
        if (invDel(inv, recipe.string, 1).failure) {
            invAdd(inv, recipe.unstrung, 1)
            resetAnim()
            return
        }
        if (invAdd(inv, recipe.output, 1).failure) {
            invAdd(inv, recipe.unstrung, 1)
            invAdd(inv, recipe.string, 1)
            mes("You don't have enough inventory space.")
            resetAnim()
            return
        }

        advanceFletchingXp(recipe.xp, xpMods.get(player, "stat.fletching"))
        mes("You add a string to the bow.")

        val completed = task.completed + 1
        if (
            completed < task.amount &&
            inv.contains(recipe.unstrung) &&
            inv.contains(recipe.string)
        ) {
            weakQueue("queue.fletching_string_bow", 3, task.copy(completed = completed))
        } else {
            resetAnim()
        }
    }

    private suspend fun ProtectedAccess.processCrossbowAssembly(task: CrossbowAssemblyTask) {
        val recipe = task.recipe
        if (!canAssembleCrossbow(recipe)) {
            resetAnim()
            return
        }

        anim("seq.human_cutting")
        if (invDel(inv, recipe.stock, 1).failure) {
            resetAnim()
            return
        }
        if (invDel(inv, recipe.limbs, 1).failure) {
            invAdd(inv, recipe.stock, 1)
            resetAnim()
            return
        }
        if (invAdd(inv, recipe.output, 1).failure) {
            invAdd(inv, recipe.stock, 1)
            invAdd(inv, recipe.limbs, 1)
            mes("You don't have enough inventory space.")
            resetAnim()
            return
        }

        advanceFletchingXp(recipe.xp, xpMods.get(player, "stat.fletching"))
        mes("You attach the limbs to the stock.")

        val completed = task.completed + 1
        if (completed < task.amount && canAssembleCrossbow(recipe, showMessages = false)) {
            weakQueue("queue.fletching_crossbow_assembly", 3, task.copy(completed = completed))
        } else {
            resetAnim()
        }
    }

    private suspend fun ProtectedAccess.processAmmo(task: AmmoTask) {
        val recipe = task.recipe
        if (!meetsFletchingLevel(recipe.level)) {
            resetAnim()
            return
        }
        if (recipe.tool != null && !inv.contains(recipe.tool)) {
            resetAnim()
            return
        }
        val amount = batchAmount(recipe)
        if (amount <= 0) {
            resetAnim()
            return
        }

        anim("seq.human_cutting")
        val primaryCount = amount * recipe.primaryCount
        val secondaryCount = amount * recipe.secondaryCount
        val outputCount = amount * recipe.outputCount
        if (invDel(inv, recipe.primary, primaryCount).failure) {
            resetAnim()
            return
        }
        if (invDel(inv, recipe.secondary, secondaryCount).failure) {
            invAdd(inv, recipe.primary, primaryCount)
            resetAnim()
            return
        }
        if (invAdd(inv, recipe.output, outputCount).failure) {
            invAdd(inv, recipe.primary, primaryCount)
            invAdd(inv, recipe.secondary, secondaryCount)
            mes("You don't have enough inventory space.")
            resetAnim()
            return
        }

        advanceFletchingXp(recipe.xp * outputCount, xpMods.get(player, "stat.fletching"))
        mes("You fletch some ammunition.")

        val completed = task.completed + 1
        if (completed < task.batches && maxBatches(recipe) > 0) {
            weakQueue("queue.fletching_ammo", 2, task.copy(completed = completed))
        } else {
            resetAnim()
        }
    }

    private suspend fun ProtectedAccess.processWolfboneArrowheads(task: WolfboneArrowheadsTask) {
        if (!meetsFletchingLevel(WOLFBONE_ARROWHEAD_LEVEL)) {
            resetAnim()
            return
        }
        if (!inv.contains(FletchingDefinitions.CHISEL) || !inv.contains(FletchingDefinitions.WOLF_BONES)) {
            resetAnim()
            return
        }

        anim("seq.human_cutting")
        if (invDel(inv, FletchingDefinitions.WOLF_BONES, 1).failure) {
            resetAnim()
            return
        }
        if (invAdd(inv, FletchingDefinitions.WOLFBONE_ARROWHEADS, WOLFBONE_ARROWHEADS_PER_BONE).failure) {
            invAdd(inv, FletchingDefinitions.WOLF_BONES, 1)
            mes("You don't have enough inventory space.")
            resetAnim()
            return
        }

        advanceFletchingXp(WOLFBONE_ARROWHEAD_XP, xpMods.get(player, "stat.fletching"))
        mes("You carefully cut the bones.")

        val completed = task.completed + 1
        if (completed < task.amount && inv.contains(FletchingDefinitions.WOLF_BONES)) {
            weakQueue("queue.fletching_wolfbone_arrowheads", 3, task.copy(completed = completed))
        } else {
            resetAnim()
        }
    }

    private fun ProtectedAccess.batchAmount(recipe: FletchAmmoRecipe): Int =
        minOf(recipe.batchSize, availableAmmoOutputs(recipe))

    private fun ProtectedAccess.availableAmmoOutputs(recipe: FletchAmmoRecipe): Int =
        minOf(
            inv.count(recipe.primary) / recipe.primaryCount,
            inv.count(recipe.secondary) / recipe.secondaryCount,
        )

    private fun ProtectedAccess.maxBatches(recipe: FletchAmmoRecipe): Int {
        val outputs = availableAmmoOutputs(recipe)
        return if (outputs == 0) {
            0
        } else {
            (outputs + recipe.batchSize - 1) / recipe.batchSize
        }
    }

    private suspend fun ProtectedAccess.canAssembleCrossbow(
        recipe: CrossbowAssemblyRecipe,
        showMessages: Boolean = true,
    ): Boolean {
        if (!meetsFletchingLevel(recipe.level, "make this crossbow")) {
            return false
        }
        if (!inv.contains(recipe.stock) || !inv.contains(recipe.limbs)) {
            if (showMessages) {
                mes(Constants.dm_default)
            }
            return false
        }
        return true
    }

    private data class CarveTask(
        val recipe: CarveRecipe,
        val amount: Int,
        val completed: Int,
    )

    private data class StringBowTask(
        val recipe: StringBowRecipe,
        val amount: Int,
        val completed: Int,
    )

    private data class CrossbowAssemblyTask(
        val recipe: CrossbowAssemblyRecipe,
        val amount: Int,
        val completed: Int,
    )

    private data class AmmoTask(
        val recipe: FletchAmmoRecipe,
        val batches: Int,
        val completed: Int,
    )

    private data class WolfboneArrowheadsTask(
        val amount: Int,
        val completed: Int,
    )

    private companion object {
        const val WOLFBONE_ARROWHEAD_LEVEL = 5
        const val WOLFBONE_ARROWHEADS_PER_BONE = 6
        const val WOLFBONE_ARROWHEAD_XP = 2.5
    }
}
