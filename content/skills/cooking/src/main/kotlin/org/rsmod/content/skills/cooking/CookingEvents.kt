package org.rsmod.content.skills.cooking

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.cookingLvl
import org.rsmod.api.script.onOpContentLoc1
import org.rsmod.api.script.onOpContentMixedLocU
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.api.table.FiremakingColoredLogsRow
import org.rsmod.api.table.cooking.CookingFoodsRow
import org.rsmod.content.skills.Material
import org.rsmod.content.skills.SkillMultiConfig
import org.rsmod.content.skills.SkillMultiEntry
import org.rsmod.content.skills.openSkillMulti
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import skillSuccess

class CookingEvents @Inject constructor(
    private val xpMods: XpModifiers,
) : PluginScript() {

    private val coloredLogRows = FiremakingColoredLogsRow.all()

    private val rangeOnlyFoods = setOf(
        "obj.bread_dough",
        "obj.uncooked_redberry_pie",
        "obj.uncooked_meat_pie",
        "obj.uncooked_apple_pie",
        "obj.uncooked_pizza",
        "obj.uncooked_mud_pie",
        "obj.uncooked_garden_pie",
        "obj.uncooked_fish_pie",
        "obj.uncooked_botanical_pie",
        "obj.uncooked_mushroom_pie",
        "obj.uncooked_admiral_pie",
        "obj.uncooked_dragonfruit_pie",
        "obj.uncooked_wild_pie",
        "obj.uncooked_summer_pie",
        "obj.uncooked_cake",
    )
    private val leftoverByRaw = mapOf(
        "obj.uncooked_cake" to "obj.cake_tin",
    )
    private val extraFoods = listOf(
        CookableFood(
            raw = "obj.potato",
            rawName = "potato",
            cooked = "obj.potato_baked",
            burnt = "obj.potato_burnt",
            level = 7,
            xp = 15,
            stopBurnFire = 34,
            stopBurnRange = 34,
            low = 108,
            high = 472,
            rangeOnly = true,
        ),
        CookableFood(
            raw = "obj.sweetcorn",
            rawName = "sweetcorn",
            cooked = "obj.sweetcorn_cooked",
            burnt = "obj.sweetcorn_burnt",
            level = 28,
            xp = 104,
            stopBurnFire = 58,
            stopBurnRange = 58,
            low = 78,
            high = 412,
        ),
        CookableFood(
            raw = "obj.uncooked_pitta_bread",
            rawName = "pitta dough",
            cooked = "obj.pitta_bread",
            burnt = "obj.burnt_pitta_bread",
            level = 58,
            xp = 40,
            stopBurnFire = 99,
            stopBurnRange = 99,
            low = 118,
            high = 492,
            rangeOnly = true,
        ),
        CookableFood(
            raw = "obj.uncooked_curry",
            rawName = "curry",
            cooked = "obj.curry",
            burnt = "obj.burnt_curry",
            level = 60,
            xp = 280,
            stopBurnFire = 90,
            stopBurnRange = 90,
            low = 38,
            high = 332,
        ),
        CookableFood(
            raw = "obj.raw_ugthanki_meat",
            rawName = "raw ugthanki meat",
            cooked = "obj.cooked_ugthanki_meat",
            burnt = "obj.burnt_meat",
            level = 1,
            xp = 40,
            stopBurnFire = 34,
            stopBurnRange = 34,
            low = 40,
            high = 252,
        ),
        CookableFood(
            raw = "obj.yak_meat_raw",
            rawName = "raw yak meat",
            cooked = "obj.cooked_meat",
            burnt = "obj.burnt_meat",
            level = 1,
            xp = 40,
            stopBurnFire = 34,
            stopBurnRange = 34,
            low = 128,
            high = 512,
        ),
        CookableFood(
            raw = "obj.raw_chompy",
            rawName = "raw chompy",
            cooked = "obj.cooked_chompy",
            burnt = "obj.ruined_chompy",
            level = 30,
            xp = 100,
            stopBurnFire = 99,
            stopBurnRange = 99,
            low = 200,
            high = 255,
        ),
        CookableFood(
            raw = "obj.spit_skewered_chompy",
            rawName = "skewered chompy",
            cooked = "obj.cooked_chompy",
            burnt = "obj.ruined_chompy",
            level = 30,
            xp = 100,
            stopBurnFire = 99,
            stopBurnRange = 99,
            low = 200,
            high = 255,
            leftover = "obj.spit_iron",
        ),
        CookableFood(
            raw = "obj.100_jubbly_meat_raw",
            rawName = "raw jubbly",
            cooked = "obj.100_jubbly_meat_cooked",
            burnt = "obj.100_jubbly_meat_burned",
            level = 41,
            xp = 160,
            stopBurnFire = 99,
            stopBurnRange = 99,
            low = 195,
            high = 250,
        ),
        CookableFood(
            raw = "obj.snail_corpse1",
            rawName = "thin snail",
            cooked = "obj.snail_corpse_cooked1",
            burnt = "obj.burnt_snail",
            level = 12,
            xp = 70,
            stopBurnFire = 58,
            stopBurnRange = 58,
            low = 93,
            high = 444,
        ),
        CookableFood(
            raw = "obj.snail_corpse2",
            rawName = "lean snail",
            cooked = "obj.snail_corpse_cooked2",
            burnt = "obj.burnt_snail",
            level = 17,
            xp = 80,
            stopBurnFire = 58,
            stopBurnRange = 58,
            low = 85,
            high = 428,
        ),
        CookableFood(
            raw = "obj.snail_corpse3",
            rawName = "fat snail",
            cooked = "obj.snail_corpse_cooked3",
            burnt = "obj.burnt_snail",
            level = 22,
            xp = 95,
            stopBurnFire = 58,
            stopBurnRange = 58,
            low = 73,
            high = 402,
        ),
        CookableFood(
            raw = "obj.mort_slimey_eel",
            rawName = "slimy eel",
            cooked = "obj.mort_slimey_eel_cooked",
            burnt = "obj.burnt_eel",
            level = 28,
            xp = 95,
            stopBurnFire = 74,
            stopBurnRange = 74,
            low = 63,
            high = 382,
        ),
        CookableFood(
            raw = "obj.seaweed",
            rawName = "seaweed",
            cooked = "obj.soda_ash",
            burnt = null,
            level = 1,
            xp = 0,
            stopBurnFire = 1,
            stopBurnRange = 1,
            low = 256,
            high = 256,
            successMessage = "You burn the seaweed into soda ash.",
        ),
        CookableFood(
            raw = "obj.barley",
            rawName = "barley",
            cooked = "obj.barley_malt",
            burnt = null,
            level = 1,
            xp = 0,
            stopBurnFire = 1,
            stopBurnRange = 1,
            low = 256,
            high = 256,
            rangeOnly = true,
        ),
    )
    private val foods = CookingFoodsRow.all().map { it.toCookableFood() } + extraFoods

    private val fires = listOf("loc.fire") + coloredLogRows.map { it.fireObject.internalName }
    private val campfires = listOf("loc.forestry_fire") + coloredLogRows.map { it.campfireObject.internalName }

    enum class RangeType(val burnReduction: Int) {
        STANDARD(0),
        LUMBRIDGE(5),
        HOSIDIUS(5),
    }

    private val rangeTypeByContent = mapOf(
        "content.cooking_range_standard" to RangeType.STANDARD,
        "content.cooking_range_lumbridge" to RangeType.LUMBRIDGE,
        "content.cooking_range_hosidius" to RangeType.HOSIDIUS
    )

    private sealed class CookingSurface {
        data class Fire(val locInternal: String) : CookingSurface()

        data class Range(val rangeType: RangeType, val locInternal: String) : CookingSurface()
    }

    override fun ScriptContext.startup() {
        val fireAndCamp = fires + campfires
        foods.forEach { food ->
            fireAndCamp.forEach { loc ->
                onOpLocU(loc, food.raw) {
                    useFoodOnSurface(food, CookingSurface.Fire(loc))
                }
            }
        }
        rangeTypeByContent.forEach { (content, rangeType) ->
            foods.forEach { food ->
                onOpContentMixedLocU(content, food.raw) {
                    useFoodOnSurface(food, CookingSurface.Range(rangeType, it.type.internalName))
                }
            }
        }

        fires.forEach { loc ->
            onOpLoc1(loc) {
                openCookingMenu(CookingSurface.Fire(it.type.internalName))
            }
        }
        rangeTypeByContent.forEach { (content, rangeType) ->
            onOpContentLoc1(content) {
                openCookingMenu(CookingSurface.Range(rangeType, it.type.internalName))
            }
        }

        onPlayerQueueWithArgs<CookTask>("queue.cooking_cook") { processCookTick(it.args) }
    }

    private fun ProtectedAccess.burnReduction(surface: CookingSurface, food: CookableFood): Int {
        var reduction =
            when (surface) {
                is CookingSurface.Range -> surface.rangeType.burnReduction
                is CookingSurface.Fire -> 0
            }

        if (food.supportsGauntlet && "obj.gauntlets_of_cooking" in player.worn) {
            reduction += 6
        }

        return reduction
    }

    private fun ProtectedAccess.isBurned(food: CookableFood, surface: CookingSurface): Boolean {
        if (food.burnt == null) return false
        if (hasCookingCape()) return false

        val isRange = surface is CookingSurface.Range
        val baseStop = if (isRange) food.stopBurnRange else food.stopBurnFire
        val stopBurn = baseStop - burnReduction(surface, food)

        if (player.cookingLvl >= stopBurn) return false
        val (low, high) = food.low to food.high
        return !skillSuccess(low, high, player.cookingLvl)
    }

    private fun ProtectedAccess.hasCookingCape(): Boolean {
        return "obj.skillcape_cooking" in player.worn ||
            "obj.skillcape_cooking_trimmed" in player.worn
    }

    private fun cookAnim(surface: CookingSurface) =
        when (surface) {
            is CookingSurface.Range -> "seq.human_cooking"
            is CookingSurface.Fire -> "seq.human_firecooking"
        }

    private suspend fun ProtectedAccess.openCookingMenu(surface: CookingSurface) {
        val cookable =
            foods.filter {
                inv.contains(it.raw) && player.cookingLvl >= it.level && it.canCookOn(surface)
            }

        if (cookable.isEmpty()) {
            mes("You have nothing to cook on this ${if (surface is CookingSurface.Range) "range" else "fire"}.")
            return
        }

        if (cookable.size == 1 && inv.count(cookable.first().raw) == 1) {
            cookInstant(cookable.first(), surface)
            return
        }

        if (cookable.size == 1) {
            val food = cookable.first()
            openSkillMulti(SkillMultiConfig(
                verb = "cook",
                entries = listOf(SkillMultiEntry(food.cooked, listOf(
                    Material(food.raw),
                ))),
            )) { selection ->
                cookFood(food, surface, selection.amount)
            }
            return
        }

        openSkillMulti(SkillMultiConfig(
            verb = "cook",
            entries = cookable.map { food ->
                SkillMultiEntry(food.cooked, listOf(
                    Material(food.raw),
                ))
            },
        )) { selection ->
            val food = cookable.firstOrNull {
                it.cooked == selection.entry.internal
            } ?: return@openSkillMulti
            cookFood(food, surface, selection.amount)
        }
    }

    private suspend fun ProtectedAccess.useFoodOnSurface(food: CookableFood, surface: CookingSurface) {
        if (!food.canCookOn(surface)) {
            mes("You need to cook this on a range.")
            return
        }
        if (player.cookingLvl < food.level) {
            mes("You need a Cooking level of ${food.level} to cook ${food.cookingName()}.")
            return
        }

        val amount = inv.count(food.raw)
        if (amount <= 1) {
            cookInstant(food, surface)
            return
        }

        openSkillMulti(
            SkillMultiConfig(
                verb = "cook",
                entries = listOf(
                    SkillMultiEntry(
                        food.cooked,
                        listOf(Material(food.raw)),
                    ),
                ),
                maxCountProvider = { inventory, _ -> inventory.count(food.raw) },
            ),
        ) { selection ->
            cookFood(food, surface, selection.amount)
        }
    }

    private fun ProtectedAccess.cookInstant(food: CookableFood, surface: CookingSurface) {
        if (!canStartCook(food, surface)) {
            return
        }
        anim(cookAnim(surface))
        applyCook(food, surface)
    }

    private fun ProtectedAccess.cookFood(food: CookableFood, surface: CookingSurface, amount: Int = 1) {
        if (!canStartCook(food, surface)) {
            return
        }
        anim(cookAnim(surface))
        weakQueue("queue.cooking_cook", 4, CookTask(food, surface, amount, 0))
    }

    private fun ProtectedAccess.processCookTick(task: CookTask) {
        val food = task.food

        if (!canStartCook(food, task.surface)) {
            resetAnim()
            return
        }

        applyCook(food, task.surface)

        val cooked = task.cooked + 1
        if (cooked < task.amount && inv.contains(food.raw)) {
            anim(cookAnim(task.surface))
            weakQueue("queue.cooking_cook", 4, CookTask(food, task.surface, task.amount, cooked))
        } else {
            resetAnim()
        }
    }

    private fun ProtectedAccess.applyCook(food: CookableFood, surface: CookingSurface) {
        val burned = isBurned(food, surface)
        if (invDel(inv, food.raw, 1).failure) {
            resetAnim()
            return
        }

        if (burned) {
            val burnt = food.burnt ?: return
            if (invAdd(inv, burnt, 1).failure) {
                invAdd(inv, food.raw, 1)
                resetAnim()
                return
            }
            mes(food.burnMessage ?: "You accidentally burn the ${food.cookingName()}.")
        } else {
            if (invAdd(inv, food.cooked, 1).failure) {
                invAdd(inv, food.raw, 1)
                resetAnim()
                return
            }
            val xpModifier = xpMods.get(player, "stat.cooking")
            statAdvance("stat.cooking", food.xp.toDouble() * xpModifier)
            val successMessage = food.successMessage ?: "You successfully cook the ${food.cookingName()}."
            if (successMessage.isNotEmpty()) {
                mes(successMessage)
            }
        }

        food.leftover?.let { leftover ->
            invAdd(inv, leftover, 1)
        }
        soundSynth(COOKING_SOUND)
    }

    private fun ProtectedAccess.canStartCook(food: CookableFood, surface: CookingSurface): Boolean {
        if (!food.canCookOn(surface)) {
            mes("You need to cook this on a range.")
            return false
        }
        if (!inv.contains(food.raw)) {
            return false
        }
        if (player.cookingLvl < food.level) {
            mes("You need a Cooking level of ${food.level} to cook ${food.cookingName()}.")
            return false
        }
        if (food.leftover != null && inv.freeSpace() < 1) {
            mes("You need at least one free inventory space to cook this.")
            return false
        }
        return true
    }

    private fun CookingFoodsRow.toCookableFood(): CookableFood =
        CookableFood(
            raw = raw.internalName,
            rawName = raw.name,
            cooked = cooked.internalName,
            burnt = burnt.internalName,
            level = level,
            xp = xp,
            stopBurnFire = stopBurnFire,
            stopBurnRange = stopBurnRange,
            low = low,
            high = high,
            supportsGauntlet = supportsGauntlet == true,
            rangeOnly = raw.internalName in rangeOnlyFoods,
            leftover = leftoverByRaw[raw.internalName],
        )

    private data class CookableFood(
        val raw: String,
        val rawName: String,
        val cooked: String,
        val burnt: String?,
        val level: Int,
        val xp: Int,
        val stopBurnFire: Int,
        val stopBurnRange: Int,
        val low: Int,
        val high: Int,
        val supportsGauntlet: Boolean = false,
        val rangeOnly: Boolean = false,
        val leftover: String? = null,
        val successMessage: String? = null,
        val burnMessage: String? = null,
    ) {
        fun canCookOn(surface: CookingSurface): Boolean =
            !rangeOnly || surface is CookingSurface.Range

        fun cookingName(): String {
            val cleanName =
                rawName
                    .removePrefix("Raw ")
                    .removePrefix("raw ")
                    .removePrefix("Uncooked ")
                    .removePrefix("uncooked ")
            return cleanName.replaceFirstChar { it.lowercaseChar() }
        }
    }

    private data class CookTask(
        val food: CookableFood,
        val surface: CookingSurface,
        val amount: Int,
        val cooked: Int,
    )

    private companion object {
        private const val COOKING_SOUND = 2577
    }
}
