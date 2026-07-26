package org.rsmod.content.skills.crafting

import org.rsmod.content.skills.Material

internal data class GemCuttingRecipe(
    val uncut: String,
    val cut: String,
    val level: Int,
    val xp: Double,
)

internal data class LeatherCraftingRecipe(
    val material: String,
    val output: String,
    val materialCount: Int,
    val level: Int,
    val xp: Double,
)

internal data class GlassblowingRecipe(
    val output: String,
    val level: Int,
    val xp: Double,
)

internal data class SpinningRecipe(
    val input: String,
    val output: String,
    val level: Int,
    val xp: Double,
)

internal data class BattlestaffRecipe(
    val orb: String,
    val output: String,
    val level: Int,
    val xp: Double,
)

internal data class PotteryRecipe(
    val unfired: String,
    val fired: String,
    val level: Int,
    val shapeXp: Double,
    val fireXp: Double,
)

internal data class AmethystCraftingRecipe(
    val output: String,
    val outputCount: Int,
    val level: Int,
    val xp: Double,
)

internal data class TanningRecipe(
    val input: String,
    val output: String,
    val coinCost: Int,
)

internal data class JewelleryCraftingRecipe(
    val bar: String,
    val mould: String,
    val output: String,
    val gem: String? = null,
    val level: Int,
    val xp: Double,
)

internal data class JewelleryStringingRecipe(
    val input: String,
    val output: String,
    val level: Int,
    val xp: Double,
)

internal object CraftingDefinitions {

    const val CHISEL = "obj.chisel"
    const val NEEDLE = "obj.needle"
    const val THREAD = "obj.thread"
    const val GLASSBLOWING_PIPE = "obj.glassblowingpipe"
    const val MOLTEN_GLASS = "obj.molten_glass"
    const val SOFT_CLAY = "obj.softclay"
    const val AMETHYST = "obj.amethyst"
    const val BATTLESTAFF = "obj.battlestaff"
    const val GOLD_BAR = "obj.gold_bar"
    const val SILVER_BAR = "obj.silver_bar"
    const val BALL_OF_WOOL = "obj.ball_of_wool"
    const val COINS = "obj.coins"

    const val RING_MOULD = "obj.ring_mould"
    const val NECKLACE_MOULD = "obj.necklace_mould"
    const val AMULET_MOULD = "obj.amulet_mould"
    const val BRACELET_MOULD = "obj.jewl_bracelet_mould"
    const val TIARA_MOULD = "obj.tiara_mould"
    const val HOLY_SYMBOL_MOULD = "obj.holy_symbol_mould"
    const val UNHOLY_SYMBOL_MOULD = "obj.unholy_symbol_mould"
    const val SICKLE_MOULD = "obj.sickle_mould"

    val spinningWheelLocIds =
        listOf(
            4309,
            8748,
            14889,
            20365,
            21304,
            25824,
            26143,
            40735,
            55330,
            56964,
        )

    val potteryWheelLocIds = listOf(2642)
    val potteryOvenLocIds = listOf(2643)

    val tannerNpcs =
        listOf(
            "npc.ellis_tanner",
            "npc.tanner",
            "npc.werewolftanner",
            "npc.auburn_tanner",
            "npc.ranging_guild_leatherworker",
            "npc.prif_citizen_mahtan",
        )

    val tanningRecipes =
        listOf(
            TanningRecipe("obj.cow_hide", "obj.leather", coinCost = 1),
            TanningRecipe("obj.cow_hide", "obj.hard_leather", coinCost = 3),
            TanningRecipe("obj.dragonhide_green", "obj.dragon_leather", coinCost = 20),
            TanningRecipe("obj.dragonhide_blue", "obj.dragon_leather_blue", coinCost = 20),
            TanningRecipe("obj.dragonhide_red", "obj.dragon_leather_red", coinCost = 20),
            TanningRecipe("obj.dragonhide_black", "obj.dragon_leather_black", coinCost = 20),
            TanningRecipe("obj.village_snake_hide", "obj.village_snake_skin", coinCost = 15),
            TanningRecipe("obj.suqka_hide_untanned", "obj.suqka_hide", coinCost = 100),
            TanningRecipe("obj.yak_hide", "obj.yak_hide_cured", coinCost = 5),
        )

    val potteryRecipes =
        listOf(
            PotteryRecipe("obj.pot_unfired", "obj.pot_empty", level = 1, shapeXp = 6.3, fireXp = 6.3),
            PotteryRecipe("obj.piedish_unfired", "obj.piedish", level = 7, shapeXp = 15.0, fireXp = 10.0),
            PotteryRecipe("obj.bowl_unfired", "obj.bowl_empty", level = 8, shapeXp = 18.0, fireXp = 15.0),
            PotteryRecipe("obj.plantpot_unfired", "obj.plantpot_empty", level = 19, shapeXp = 20.0, fireXp = 17.5),
            PotteryRecipe("obj.potlid_unfired", "obj.potlid", level = 25, shapeXp = 20.0, fireXp = 20.0),
        )

    val amethystRecipes =
        listOf(
            AmethystCraftingRecipe("obj.xbows_bolt_tips_amethyst", outputCount = 15, level = 83, xp = 60.0),
            AmethystCraftingRecipe("obj.amethyst_arrowheads", outputCount = 15, level = 85, xp = 60.0),
            AmethystCraftingRecipe("obj.amethyst_javelin_head", outputCount = 5, level = 87, xp = 60.0),
            AmethystCraftingRecipe("obj.amethyst_dart_tip", outputCount = 8, level = 89, xp = 60.0),
        )

    val gemCuttingRecipes =
        listOf(
            GemCuttingRecipe("obj.uncut_opal", "obj.opal", level = 1, xp = 15.0),
            GemCuttingRecipe("obj.uncut_jade", "obj.jade", level = 13, xp = 20.0),
            GemCuttingRecipe("obj.uncut_red_topaz", "obj.red_topaz", level = 16, xp = 25.0),
            GemCuttingRecipe("obj.uncut_sapphire", "obj.sapphire", level = 20, xp = 50.0),
            GemCuttingRecipe("obj.uncut_emerald", "obj.emerald", level = 27, xp = 67.5),
            GemCuttingRecipe("obj.uncut_ruby", "obj.ruby", level = 34, xp = 85.0),
            GemCuttingRecipe("obj.uncut_diamond", "obj.diamond", level = 43, xp = 107.5),
            GemCuttingRecipe("obj.uncut_dragonstone", "obj.dragonstone", level = 55, xp = 137.5),
            GemCuttingRecipe("obj.uncut_onyx", "obj.onyx", level = 67, xp = 167.5),
            GemCuttingRecipe("obj.uncut_zenyte", "obj.zenyte", level = 89, xp = 200.0),
        )

    val leatherCraftingRecipes =
        listOf(
            LeatherCraftingRecipe("obj.leather", "obj.leather_gloves", 1, level = 1, xp = 13.8),
            LeatherCraftingRecipe("obj.leather", "obj.leather_boots", 1, level = 7, xp = 16.3),
            LeatherCraftingRecipe("obj.leather", "obj.leather_cowl", 1, level = 9, xp = 18.5),
            LeatherCraftingRecipe("obj.leather", "obj.leather_vambraces", 1, level = 11, xp = 22.0),
            LeatherCraftingRecipe("obj.leather", "obj.leather_armour", 1, level = 14, xp = 25.0),
            LeatherCraftingRecipe("obj.leather", "obj.leather_chaps", 1, level = 18, xp = 27.0),
            LeatherCraftingRecipe("obj.leather", "obj.coif", 1, level = 38, xp = 37.0),
            LeatherCraftingRecipe("obj.hard_leather", "obj.hardleather_body", 1, level = 28, xp = 35.0),
            LeatherCraftingRecipe("obj.dragonhide_green", "obj.dragon_vambraces", 1, level = 57, xp = 62.0),
            LeatherCraftingRecipe("obj.dragonhide_green", "obj.dragonhide_chaps", 2, level = 60, xp = 124.0),
            LeatherCraftingRecipe("obj.dragonhide_green", "obj.dragonhide_body", 3, level = 63, xp = 186.0),
            LeatherCraftingRecipe("obj.dragonhide_blue", "obj.blue_dragon_vambraces", 1, level = 66, xp = 70.0),
            LeatherCraftingRecipe("obj.dragonhide_blue", "obj.blue_dragonhide_chaps", 2, level = 68, xp = 140.0),
            LeatherCraftingRecipe("obj.dragonhide_blue", "obj.blue_dragonhide_body", 3, level = 71, xp = 210.0),
            LeatherCraftingRecipe("obj.dragonhide_red", "obj.red_dragon_vambraces", 1, level = 73, xp = 78.0),
            LeatherCraftingRecipe("obj.dragonhide_red", "obj.red_dragonhide_chaps", 2, level = 75, xp = 156.0),
            LeatherCraftingRecipe("obj.dragonhide_red", "obj.red_dragonhide_body", 3, level = 77, xp = 234.0),
            LeatherCraftingRecipe("obj.dragonhide_black", "obj.black_dragon_vambraces", 1, level = 79, xp = 86.0),
            LeatherCraftingRecipe("obj.dragonhide_black", "obj.black_dragonhide_chaps", 2, level = 82, xp = 172.0),
            LeatherCraftingRecipe("obj.dragonhide_black", "obj.black_dragonhide_body", 3, level = 84, xp = 258.0),
        )

    val glassblowingRecipes =
        listOf(
            GlassblowingRecipe("obj.beer_glass", level = 1, xp = 17.5),
            GlassblowingRecipe("obj.candle_lantern_empty", level = 4, xp = 19.0),
            GlassblowingRecipe("obj.oil_lamp_empty", level = 12, xp = 25.0),
            GlassblowingRecipe("obj.vial_empty", level = 33, xp = 35.0),
            GlassblowingRecipe("obj.fishbowl_empty", level = 42, xp = 42.5),
            GlassblowingRecipe("obj.stafforb", level = 46, xp = 52.5),
            GlassblowingRecipe("obj.bullseye_lantern_lens", level = 49, xp = 55.0),
        )

    val spinningRecipes =
        listOf(
            SpinningRecipe("obj.wool", "obj.ball_of_wool", level = 1, xp = 2.5),
            SpinningRecipe("obj.flax", "obj.bow_string", level = 1, xp = 15.0),
            SpinningRecipe("obj.xbows_sinew", "obj.xbows_crossbow_string", level = 10, xp = 15.0),
            SpinningRecipe("obj.magic_roots", "obj.magic_string", level = 19, xp = 30.0),
        )

    val battlestaffRecipes =
        listOf(
            BattlestaffRecipe("obj.water_orb", "obj.water_battlestaff", level = 54, xp = 100.0),
            BattlestaffRecipe("obj.earth_orb", "obj.earth_battlestaff", level = 58, xp = 112.5),
            BattlestaffRecipe("obj.fire_orb", "obj.fire_battlestaff", level = 62, xp = 125.0),
            BattlestaffRecipe("obj.air_orb", "obj.air_battlestaff", level = 66, xp = 137.5),
        )

    val jewelleryMoulds =
        listOf(
            RING_MOULD,
            NECKLACE_MOULD,
            AMULET_MOULD,
            BRACELET_MOULD,
            TIARA_MOULD,
            HOLY_SYMBOL_MOULD,
            UNHOLY_SYMBOL_MOULD,
            SICKLE_MOULD,
        )

    val jewelleryRecipes =
        listOf(
            JewelleryCraftingRecipe(GOLD_BAR, RING_MOULD, "obj.gold_ring", level = 5, xp = 15.0),
            JewelleryCraftingRecipe(GOLD_BAR, NECKLACE_MOULD, "obj.gold_necklace", level = 6, xp = 20.0),
            JewelleryCraftingRecipe(GOLD_BAR, BRACELET_MOULD, "obj.jewl_gold_bracelet", level = 7, xp = 25.0),
            JewelleryCraftingRecipe(GOLD_BAR, AMULET_MOULD, "obj.unstrung_gold_amulet", level = 8, xp = 30.0),

            JewelleryCraftingRecipe(SILVER_BAR, RING_MOULD, "obj.opal_ring", "obj.opal", level = 1, xp = 10.0),
            JewelleryCraftingRecipe(SILVER_BAR, RING_MOULD, "obj.jade_ring", "obj.jade", level = 13, xp = 32.0),
            JewelleryCraftingRecipe(SILVER_BAR, RING_MOULD, "obj.topaz_ring", "obj.red_topaz", level = 16, xp = 35.0),
            JewelleryCraftingRecipe(SILVER_BAR, NECKLACE_MOULD, "obj.opal_necklace", "obj.opal", level = 16, xp = 35.0),
            JewelleryCraftingRecipe(SILVER_BAR, NECKLACE_MOULD, "obj.jade_necklace", "obj.jade", level = 25, xp = 54.0),
            JewelleryCraftingRecipe(SILVER_BAR, NECKLACE_MOULD, "obj.topaz_necklace", "obj.red_topaz", level = 32, xp = 70.0),
            JewelleryCraftingRecipe(SILVER_BAR, BRACELET_MOULD, "obj.opal_bracelet", "obj.opal", level = 22, xp = 45.0),
            JewelleryCraftingRecipe(SILVER_BAR, BRACELET_MOULD, "obj.jade_bracelet", "obj.jade", level = 29, xp = 60.0),
            JewelleryCraftingRecipe(SILVER_BAR, BRACELET_MOULD, "obj.topaz_bracelet", "obj.red_topaz", level = 38, xp = 75.0),
            JewelleryCraftingRecipe(SILVER_BAR, AMULET_MOULD, "obj.unstrung_opal_amulet", "obj.opal", level = 27, xp = 55.0),
            JewelleryCraftingRecipe(SILVER_BAR, AMULET_MOULD, "obj.unstrung_jade_amulet", "obj.jade", level = 34, xp = 70.0),
            JewelleryCraftingRecipe(SILVER_BAR, AMULET_MOULD, "obj.unstrung_topaz_amulet", "obj.red_topaz", level = 45, xp = 80.0),
            JewelleryCraftingRecipe(SILVER_BAR, HOLY_SYMBOL_MOULD, "obj.nostringstar", level = 16, xp = 50.0),
            JewelleryCraftingRecipe(SILVER_BAR, UNHOLY_SYMBOL_MOULD, "obj.nostringsnake", level = 17, xp = 50.0),
            JewelleryCraftingRecipe(SILVER_BAR, SICKLE_MOULD, "obj.silver_sickle", level = 18, xp = 50.0),
            JewelleryCraftingRecipe(SILVER_BAR, TIARA_MOULD, "obj.tiara", level = 23, xp = 52.5),

            JewelleryCraftingRecipe(GOLD_BAR, RING_MOULD, "obj.sapphire_ring", "obj.sapphire", level = 20, xp = 40.0),
            JewelleryCraftingRecipe(GOLD_BAR, BRACELET_MOULD, "obj.jewl_sapphire_bracelet", "obj.sapphire", level = 23, xp = 60.0),
            JewelleryCraftingRecipe(GOLD_BAR, AMULET_MOULD, "obj.unstrung_sapphire_amulet", "obj.sapphire", level = 24, xp = 65.0),
            JewelleryCraftingRecipe(GOLD_BAR, NECKLACE_MOULD, "obj.sapphire_necklace", "obj.sapphire", level = 22, xp = 55.0),

            JewelleryCraftingRecipe(GOLD_BAR, RING_MOULD, "obj.emerald_ring", "obj.emerald", level = 27, xp = 55.0),
            JewelleryCraftingRecipe(GOLD_BAR, NECKLACE_MOULD, "obj.emerald_necklace", "obj.emerald", level = 29, xp = 60.0),
            JewelleryCraftingRecipe(GOLD_BAR, BRACELET_MOULD, "obj.jewl_emerald_bracelet", "obj.emerald", level = 30, xp = 65.0),
            JewelleryCraftingRecipe(GOLD_BAR, AMULET_MOULD, "obj.unstrung_emerald_amulet", "obj.emerald", level = 31, xp = 70.0),

            JewelleryCraftingRecipe(GOLD_BAR, RING_MOULD, "obj.ruby_ring", "obj.ruby", level = 34, xp = 70.0),
            JewelleryCraftingRecipe(GOLD_BAR, NECKLACE_MOULD, "obj.ruby_necklace", "obj.ruby", level = 40, xp = 75.0),
            JewelleryCraftingRecipe(GOLD_BAR, BRACELET_MOULD, "obj.jewl_ruby_bracelet", "obj.ruby", level = 42, xp = 80.0),
            JewelleryCraftingRecipe(GOLD_BAR, AMULET_MOULD, "obj.unstrung_ruby_amulet", "obj.ruby", level = 50, xp = 85.0),

            JewelleryCraftingRecipe(GOLD_BAR, RING_MOULD, "obj.diamond_ring", "obj.diamond", level = 43, xp = 85.0),
            JewelleryCraftingRecipe(GOLD_BAR, NECKLACE_MOULD, "obj.diamond_necklace", "obj.diamond", level = 56, xp = 90.0),
            JewelleryCraftingRecipe(GOLD_BAR, BRACELET_MOULD, "obj.jewl_diamond_bracelet", "obj.diamond", level = 58, xp = 95.0),
            JewelleryCraftingRecipe(GOLD_BAR, AMULET_MOULD, "obj.unstrung_diamond_amulet", "obj.diamond", level = 70, xp = 100.0),

            JewelleryCraftingRecipe(GOLD_BAR, RING_MOULD, "obj.dragonstone_ring", "obj.dragonstone", level = 55, xp = 100.0),
            JewelleryCraftingRecipe(GOLD_BAR, NECKLACE_MOULD, "obj.dragonstone_necklace", "obj.dragonstone", level = 72, xp = 105.0),
            JewelleryCraftingRecipe(GOLD_BAR, BRACELET_MOULD, "obj.jewl_dragonstone_bracelet", "obj.dragonstone", level = 74, xp = 110.0),
            JewelleryCraftingRecipe(GOLD_BAR, AMULET_MOULD, "obj.unstrung_dragonstone_amulet", "obj.dragonstone", level = 80, xp = 150.0),

            JewelleryCraftingRecipe(GOLD_BAR, RING_MOULD, "obj.onyx_ring", "obj.onyx", level = 67, xp = 115.0),
            JewelleryCraftingRecipe(GOLD_BAR, NECKLACE_MOULD, "obj.onyx_necklace", "obj.onyx", level = 82, xp = 120.0),
            JewelleryCraftingRecipe(GOLD_BAR, BRACELET_MOULD, "obj.jewl_onyx_bracelet", "obj.onyx", level = 84, xp = 125.0),
            JewelleryCraftingRecipe(GOLD_BAR, AMULET_MOULD, "obj.unstrung_onyx_amulet", "obj.onyx", level = 90, xp = 165.0),

            JewelleryCraftingRecipe(GOLD_BAR, RING_MOULD, "obj.zenyte_ring", "obj.zenyte", level = 89, xp = 150.0),
            JewelleryCraftingRecipe(GOLD_BAR, NECKLACE_MOULD, "obj.zenyte_necklace", "obj.zenyte", level = 92, xp = 165.0),
            JewelleryCraftingRecipe(GOLD_BAR, BRACELET_MOULD, "obj.zenyte_bracelet", "obj.zenyte", level = 95, xp = 180.0),
            JewelleryCraftingRecipe(GOLD_BAR, AMULET_MOULD, "obj.unstrung_zenyte_amulet", "obj.zenyte", level = 98, xp = 200.0),
        )

    val jewelleryStringingRecipes =
        listOf(
            JewelleryStringingRecipe("obj.unstrung_gold_amulet", "obj.strung_gold_amulet", level = 8, xp = 4.0),
            JewelleryStringingRecipe("obj.unstrung_opal_amulet", "obj.strung_opal_amulet", level = 27, xp = 4.0),
            JewelleryStringingRecipe("obj.unstrung_jade_amulet", "obj.strung_jade_amulet", level = 34, xp = 4.0),
            JewelleryStringingRecipe("obj.unstrung_topaz_amulet", "obj.strung_topaz_amulet", level = 45, xp = 4.0),
            JewelleryStringingRecipe("obj.unstrung_sapphire_amulet", "obj.strung_sapphire_amulet", level = 24, xp = 4.0),
            JewelleryStringingRecipe("obj.unstrung_emerald_amulet", "obj.strung_emerald_amulet", level = 31, xp = 4.0),
            JewelleryStringingRecipe("obj.unstrung_ruby_amulet", "obj.strung_ruby_amulet", level = 50, xp = 4.0),
            JewelleryStringingRecipe("obj.unstrung_diamond_amulet", "obj.strung_diamond_amulet", level = 70, xp = 4.0),
            JewelleryStringingRecipe("obj.unstrung_dragonstone_amulet", "obj.strung_dragonstone_amulet", level = 80, xp = 4.0),
            JewelleryStringingRecipe("obj.unstrung_onyx_amulet", "obj.strung_onyx_amulet", level = 90, xp = 4.0),
            JewelleryStringingRecipe("obj.unstrung_zenyte_amulet", "obj.zenyte_amulet", level = 98, xp = 4.0),
            JewelleryStringingRecipe("obj.nostringstar", "obj.stringstar", level = 1, xp = 4.0),
            JewelleryStringingRecipe("obj.nostringsnake", "obj.stringsnake", level = 1, xp = 4.0),
        )

    val leatherMaterials: Set<String> = leatherCraftingRecipes.mapTo(mutableSetOf()) { it.material }

    fun leatherRecipesFor(material: String): List<LeatherCraftingRecipe> =
        leatherCraftingRecipes.filter { it.material == material }

    fun leatherMaterials(recipe: LeatherCraftingRecipe): List<Material> =
        listOf(Material(recipe.material, recipe.materialCount), Material(THREAD))

    fun glassblowingMaterials(): List<Material> = listOf(Material(MOLTEN_GLASS))

    fun spinningMaterials(recipe: SpinningRecipe): List<Material> = listOf(Material(recipe.input))

    fun battlestaffMaterials(recipe: BattlestaffRecipe): List<Material> =
        listOf(Material(BATTLESTAFF), Material(recipe.orb))

    fun potteryShapeMaterials(): List<Material> = listOf(Material(SOFT_CLAY))

    fun potteryFireMaterials(recipe: PotteryRecipe): List<Material> = listOf(Material(recipe.unfired))

    fun amethystMaterials(): List<Material> = listOf(Material(AMETHYST))

    fun tanningMaterials(recipe: TanningRecipe): List<Material> =
        listOf(Material(recipe.input), Material(COINS, recipe.coinCost))

    fun jewelleryRecipesFor(mould: String? = null, bar: String? = null): List<JewelleryCraftingRecipe> =
        jewelleryRecipes.filter { recipe ->
            (mould == null || recipe.mould == mould) && (bar == null || recipe.bar == bar)
        }

    fun jewelleryMaterials(recipe: JewelleryCraftingRecipe): List<Material> =
        buildList {
            add(Material(recipe.bar))
            recipe.gem?.let { add(Material(it)) }
        }

    fun jewelleryStringingMaterials(recipe: JewelleryStringingRecipe): List<Material> =
        listOf(Material(recipe.input), Material(BALL_OF_WOOL))
}
