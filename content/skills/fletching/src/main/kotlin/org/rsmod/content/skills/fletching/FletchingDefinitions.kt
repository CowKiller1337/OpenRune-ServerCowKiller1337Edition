package org.rsmod.content.skills.fletching

import org.rsmod.content.skills.Material

internal data class CarveRecipe(
    val log: String,
    val output: String,
    val outputCount: Int,
    val level: Int,
    val xp: Double,
)

internal data class StringBowRecipe(
    val unstrung: String,
    val output: String,
    val level: Int,
    val xp: Double,
    val string: String = FletchingDefinitions.BOW_STRING,
)

internal data class FletchAmmoRecipe(
    val primary: String,
    val secondary: String,
    val output: String,
    val level: Int,
    val xp: Double,
    val batchSize: Int = 15,
    val primaryCount: Int = 1,
    val secondaryCount: Int = 1,
    val outputCount: Int = 1,
    val tool: String? = null,
)

internal data class CrossbowAssemblyRecipe(
    val stock: String,
    val limbs: String,
    val output: String,
    val level: Int,
    val xp: Double,
)

internal object FletchingDefinitions {

    const val KNIFE = "obj.knife"
    const val CHISEL = "obj.chisel"
    const val HAMMER = "obj.hammer"
    const val BOW_STRING = "obj.bow_string"
    const val CROSSBOW_STRING = "obj.xbows_crossbow_string"
    const val FEATHER = "obj.feather"
    const val ARROW_SHAFT = "obj.arrow_shaft"
    const val HEADLESS_ARROW = "obj.headless_arrow"
    const val JAVELIN_SHAFT = "obj.javelin_shaft"
    const val ACHEY_LOGS = "obj.achey_tree_logs"
    const val OGRE_ARROW_SHAFT = "obj.ogre_arrow_shaft"
    const val OGRE_HEADLESS_ARROW = "obj.ogre_headless_arrow"
    const val WOLF_BONES = "obj.wolf_bones"
    const val WOLFBONE_ARROWHEADS = "obj.wolfbone_arrowheads"

    val carveRecipes =
        listOf(
            CarveRecipe("obj.logs", ARROW_SHAFT, outputCount = 15, level = 1, xp = 5.0),
            CarveRecipe("obj.logs", JAVELIN_SHAFT, outputCount = 15, level = 3, xp = 5.0),
            CarveRecipe("obj.logs", "obj.unstrung_shortbow", outputCount = 1, level = 5, xp = 5.0),
            CarveRecipe(ACHEY_LOGS, OGRE_ARROW_SHAFT, outputCount = 4, level = 5, xp = 1.6),
            CarveRecipe("obj.logs", "obj.unstrung_longbow", outputCount = 1, level = 10, xp = 10.0),
            CarveRecipe("obj.oak_logs", "obj.unstrung_oak_shortbow", outputCount = 1, level = 20, xp = 16.5),
            CarveRecipe("obj.oak_logs", "obj.unstrung_oak_longbow", outputCount = 1, level = 25, xp = 25.0),
            CarveRecipe(ACHEY_LOGS, "obj.unstrung_zogre_bow", outputCount = 1, level = 30, xp = 45.0),
            CarveRecipe("obj.willow_logs", "obj.unstrung_willow_shortbow", outputCount = 1, level = 35, xp = 33.3),
            CarveRecipe("obj.willow_logs", "obj.unstrung_willow_longbow", outputCount = 1, level = 40, xp = 41.5),
            CarveRecipe("obj.maple_logs", "obj.unstrung_maple_shortbow", outputCount = 1, level = 50, xp = 50.0),
            CarveRecipe("obj.maple_logs", "obj.unstrung_maple_longbow", outputCount = 1, level = 55, xp = 58.3),
            CarveRecipe("obj.yew_logs", "obj.unstrung_yew_shortbow", outputCount = 1, level = 65, xp = 67.5),
            CarveRecipe("obj.yew_logs", "obj.unstrung_yew_longbow", outputCount = 1, level = 70, xp = 75.0),
            CarveRecipe("obj.magic_logs", "obj.unstrung_magic_shortbow", outputCount = 1, level = 80, xp = 83.3),
            CarveRecipe("obj.magic_logs", "obj.unstrung_magic_longbow", outputCount = 1, level = 85, xp = 91.5),
            CarveRecipe("obj.magic_logs", "obj.magic_shield", outputCount = 1, level = 87, xp = 183.0),
            CarveRecipe("obj.redwood_logs", "obj.redwood_shield", outputCount = 1, level = 92, xp = 216.0),
            CarveRecipe("obj.logs", "obj.xbows_crossbow_stock_wood", outputCount = 1, level = 9, xp = 6.0),
            CarveRecipe("obj.oak_logs", "obj.xbows_crossbow_stock_oak", outputCount = 1, level = 24, xp = 16.0),
            CarveRecipe("obj.willow_logs", "obj.xbows_crossbow_stock_willow", outputCount = 1, level = 39, xp = 22.0),
            CarveRecipe("obj.teak_logs", "obj.xbows_crossbow_stock_teak", outputCount = 1, level = 46, xp = 27.0),
            CarveRecipe("obj.maple_logs", "obj.xbows_crossbow_stock_maple", outputCount = 1, level = 54, xp = 32.0),
            CarveRecipe("obj.mahogany_logs", "obj.xbows_crossbow_stock_mahogany", outputCount = 1, level = 61, xp = 41.0),
            CarveRecipe("obj.yew_logs", "obj.xbows_crossbow_stock_yew", outputCount = 1, level = 69, xp = 50.0),
            CarveRecipe("obj.magic_logs", "obj.xbows_crossbow_stock_magic", outputCount = 1, level = 78, xp = 70.0),
        )

    val stringBowRecipes =
        listOf(
            StringBowRecipe("obj.unstrung_shortbow", "obj.shortbow", level = 5, xp = 5.0),
            StringBowRecipe("obj.unstrung_longbow", "obj.longbow", level = 10, xp = 10.0),
            StringBowRecipe("obj.unstrung_oak_shortbow", "obj.oak_shortbow", level = 20, xp = 16.5),
            StringBowRecipe("obj.unstrung_oak_longbow", "obj.oak_longbow", level = 25, xp = 25.0),
            StringBowRecipe("obj.unstrung_willow_shortbow", "obj.willow_shortbow", level = 35, xp = 33.3),
            StringBowRecipe("obj.unstrung_willow_longbow", "obj.willow_longbow", level = 40, xp = 41.5),
            StringBowRecipe("obj.unstrung_maple_shortbow", "obj.maple_shortbow", level = 50, xp = 50.0),
            StringBowRecipe("obj.unstrung_maple_longbow", "obj.maple_longbow", level = 55, xp = 58.3),
            StringBowRecipe("obj.unstrung_yew_shortbow", "obj.yew_shortbow", level = 65, xp = 67.5),
            StringBowRecipe("obj.unstrung_yew_longbow", "obj.yew_longbow", level = 70, xp = 75.0),
            StringBowRecipe("obj.unstrung_magic_shortbow", "obj.magic_shortbow", level = 80, xp = 83.3),
            StringBowRecipe("obj.unstrung_magic_longbow", "obj.magic_longbow", level = 85, xp = 91.5),
            StringBowRecipe("obj.unstrung_zogre_bow", "obj.zogre_bow", level = 30, xp = 45.0),
            StringBowRecipe(
                "obj.xbows_crossbow_unstrung_bronze",
                "obj.xbows_crossbow_bronze",
                level = 9,
                xp = 6.0,
                string = CROSSBOW_STRING,
            ),
            StringBowRecipe(
                "obj.xbows_crossbow_unstrung_blurite",
                "obj.xbows_crossbow_blurite",
                level = 24,
                xp = 16.0,
                string = CROSSBOW_STRING,
            ),
            StringBowRecipe(
                "obj.xbows_crossbow_unstrung_iron",
                "obj.xbows_crossbow_iron",
                level = 39,
                xp = 22.0,
                string = CROSSBOW_STRING,
            ),
            StringBowRecipe(
                "obj.xbows_crossbow_unstrung_steel",
                "obj.xbows_crossbow_steel",
                level = 46,
                xp = 27.0,
                string = CROSSBOW_STRING,
            ),
            StringBowRecipe(
                "obj.xbows_crossbow_unstrung_mithril",
                "obj.xbows_crossbow_mithril",
                level = 54,
                xp = 32.0,
                string = CROSSBOW_STRING,
            ),
            StringBowRecipe(
                "obj.xbows_crossbow_unstrung_adamantite",
                "obj.xbows_crossbow_adamantite",
                level = 61,
                xp = 41.0,
                string = CROSSBOW_STRING,
            ),
            StringBowRecipe(
                "obj.xbows_crossbow_unstrung_runite",
                "obj.xbows_crossbow_runite",
                level = 69,
                xp = 50.0,
                string = CROSSBOW_STRING,
            ),
            StringBowRecipe(
                "obj.xbows_crossbow_unstrung_dragon",
                "obj.xbows_crossbow_dragon",
                level = 78,
                xp = 70.0,
                string = CROSSBOW_STRING,
            ),
        )

    val crossbowAssemblyRecipes =
        listOf(
            CrossbowAssemblyRecipe(
                "obj.xbows_crossbow_stock_wood",
                "obj.xbows_crossbow_limbs_bronze",
                "obj.xbows_crossbow_unstrung_bronze",
                level = 9,
                xp = 12.0,
            ),
            CrossbowAssemblyRecipe(
                "obj.xbows_crossbow_stock_oak",
                "obj.xbows_crossbow_limbs_blurite",
                "obj.xbows_crossbow_unstrung_blurite",
                level = 24,
                xp = 32.0,
            ),
            CrossbowAssemblyRecipe(
                "obj.xbows_crossbow_stock_willow",
                "obj.xbows_crossbow_limbs_iron",
                "obj.xbows_crossbow_unstrung_iron",
                level = 39,
                xp = 44.0,
            ),
            CrossbowAssemblyRecipe(
                "obj.xbows_crossbow_stock_teak",
                "obj.xbows_crossbow_limbs_steel",
                "obj.xbows_crossbow_unstrung_steel",
                level = 46,
                xp = 54.0,
            ),
            CrossbowAssemblyRecipe(
                "obj.xbows_crossbow_stock_maple",
                "obj.xbows_crossbow_limbs_mithril",
                "obj.xbows_crossbow_unstrung_mithril",
                level = 54,
                xp = 64.0,
            ),
            CrossbowAssemblyRecipe(
                "obj.xbows_crossbow_stock_mahogany",
                "obj.xbows_crossbow_limbs_adamantite",
                "obj.xbows_crossbow_unstrung_adamantite",
                level = 61,
                xp = 82.0,
            ),
            CrossbowAssemblyRecipe(
                "obj.xbows_crossbow_stock_yew",
                "obj.xbows_crossbow_limbs_runite",
                "obj.xbows_crossbow_unstrung_runite",
                level = 69,
                xp = 100.0,
            ),
            CrossbowAssemblyRecipe(
                "obj.xbows_crossbow_stock_magic",
                "obj.xbows_crossbow_limbs_dragon",
                "obj.xbows_crossbow_unstrung_dragon",
                level = 78,
                xp = 135.0,
            ),
        )

    val ammoRecipes =
        listOf(
            FletchAmmoRecipe(ARROW_SHAFT, FEATHER, HEADLESS_ARROW, level = 1, xp = 1.0),
            FletchAmmoRecipe(HEADLESS_ARROW, "obj.bronze_arrowheads", "obj.bronze_arrow", level = 1, xp = 1.3),
            FletchAmmoRecipe(HEADLESS_ARROW, "obj.iron_arrowheads", "obj.iron_arrow", level = 15, xp = 2.5),
            FletchAmmoRecipe(HEADLESS_ARROW, "obj.steel_arrowheads", "obj.steel_arrow", level = 30, xp = 5.0),
            FletchAmmoRecipe(HEADLESS_ARROW, "obj.mithril_arrowheads", "obj.mithril_arrow", level = 45, xp = 7.5),
            FletchAmmoRecipe(HEADLESS_ARROW, "obj.adamant_arrowheads", "obj.adamant_arrow", level = 60, xp = 10.0),
            FletchAmmoRecipe(HEADLESS_ARROW, "obj.rune_arrowheads", "obj.rune_arrow", level = 75, xp = 12.5),
            FletchAmmoRecipe(HEADLESS_ARROW, "obj.amethyst_arrowheads", "obj.amethyst_arrow", level = 82, xp = 13.5),
            FletchAmmoRecipe(HEADLESS_ARROW, "obj.dragon_arrowheads", "obj.dragon_arrow", level = 90, xp = 15.0),
            FletchAmmoRecipe(JAVELIN_SHAFT, "obj.bronze_javelin_head", "obj.bronze_javelin", level = 3, xp = 1.0),
            FletchAmmoRecipe(JAVELIN_SHAFT, "obj.iron_javelin_head", "obj.iron_javelin", level = 17, xp = 2.0),
            FletchAmmoRecipe(JAVELIN_SHAFT, "obj.steel_javelin_head", "obj.steel_javelin", level = 32, xp = 5.0),
            FletchAmmoRecipe(JAVELIN_SHAFT, "obj.mithril_javelin_head", "obj.mithril_javelin", level = 47, xp = 8.0),
            FletchAmmoRecipe(JAVELIN_SHAFT, "obj.adamant_javelin_head", "obj.adamant_javelin", level = 62, xp = 10.0),
            FletchAmmoRecipe(JAVELIN_SHAFT, "obj.rune_javelin_head", "obj.rune_javelin", level = 77, xp = 12.4),
            FletchAmmoRecipe(JAVELIN_SHAFT, "obj.amethyst_javelin_head", "obj.amethyst_javelin", level = 84, xp = 13.5),
            FletchAmmoRecipe(JAVELIN_SHAFT, "obj.dragon_javelin_head", "obj.dragon_javelin", level = 92, xp = 15.0),
            FletchAmmoRecipe(
                OGRE_ARROW_SHAFT,
                FEATHER,
                OGRE_HEADLESS_ARROW,
                level = 5,
                xp = 0.9,
                secondaryCount = 4,
            ),
            FletchAmmoRecipe(OGRE_HEADLESS_ARROW, WOLFBONE_ARROWHEADS, "obj.ogre_arrow", level = 5, xp = 1.0),
            FletchAmmoRecipe(OGRE_HEADLESS_ARROW, "obj.nails_bronze", "obj.zogre_brutal_bronze", level = 7, xp = 1.4, tool = HAMMER),
            FletchAmmoRecipe(OGRE_HEADLESS_ARROW, "obj.nails_iron", "obj.zogre_brutal_iron", level = 18, xp = 2.6, tool = HAMMER),
            FletchAmmoRecipe(OGRE_HEADLESS_ARROW, "obj.nails", "obj.zogre_brutal_steel", level = 33, xp = 5.1, tool = HAMMER),
            FletchAmmoRecipe(OGRE_HEADLESS_ARROW, "obj.nails_black", "obj.zogre_brutal_black", level = 38, xp = 6.4, tool = HAMMER),
            FletchAmmoRecipe(OGRE_HEADLESS_ARROW, "obj.nails_mithril", "obj.zogre_brutal_mithril", level = 49, xp = 7.5, tool = HAMMER),
            FletchAmmoRecipe(OGRE_HEADLESS_ARROW, "obj.nails_adamant", "obj.zogre_brutal_adamant", level = 62, xp = 10.1, tool = HAMMER),
            FletchAmmoRecipe(OGRE_HEADLESS_ARROW, "obj.nails_rune", "obj.zogre_brutal_rune", level = 77, xp = 12.5, tool = HAMMER),
            FletchAmmoRecipe(HEADLESS_ARROW, "obj.slayer_broad_arrowhead", "obj.slayer_broad_arrows", level = 52, xp = 10.0),
            FletchAmmoRecipe("obj.slayer_broad_bolt_unfinished", FEATHER, "obj.slayer_broad_bolt", level = 55, xp = 3.0, batchSize = 10),
            FletchAmmoRecipe(
                "obj.slayer_broad_bolt",
                "obj.xbows_bolt_tips_amethyst",
                "obj.slayer_broad_bolt_amethyst",
                level = 76,
                xp = 10.6,
                batchSize = 10,
            ),
            FletchAmmoRecipe("obj.bronze_dart_tip", FEATHER, "obj.bronze_dart", level = 10, xp = 1.8, batchSize = 10),
            FletchAmmoRecipe("obj.iron_dart_tip", FEATHER, "obj.iron_dart", level = 22, xp = 3.8, batchSize = 10),
            FletchAmmoRecipe("obj.steel_dart_tip", FEATHER, "obj.steel_dart", level = 37, xp = 7.5, batchSize = 10),
            FletchAmmoRecipe("obj.mithril_dart_tip", FEATHER, "obj.mithril_dart", level = 52, xp = 11.2, batchSize = 10),
            FletchAmmoRecipe("obj.adamant_dart_tip", FEATHER, "obj.adamant_dart", level = 67, xp = 15.0, batchSize = 10),
            FletchAmmoRecipe("obj.rune_dart_tip", FEATHER, "obj.rune_dart", level = 81, xp = 18.8, batchSize = 10),
            FletchAmmoRecipe("obj.amethyst_dart_tip", FEATHER, "obj.amethyst_dart", level = 90, xp = 21.0, batchSize = 10),
            FletchAmmoRecipe("obj.dragon_dart_tip", FEATHER, "obj.dragon_dart", level = 95, xp = 25.0, batchSize = 10),
            FletchAmmoRecipe(
                "obj.xbows_crossbow_bolts_bronze_unfeathered",
                FEATHER,
                "obj.bolts",
                level = 9,
                xp = 0.5,
                batchSize = 10,
            ),
            FletchAmmoRecipe(
                "obj.xbows_crossbow_bolts_blurite_unfeathered",
                FEATHER,
                "obj.xbows_crossbow_bolts_blurite",
                level = 24,
                xp = 1.0,
                batchSize = 10,
            ),
            FletchAmmoRecipe(
                "obj.xbows_crossbow_bolts_iron_unfeathered",
                FEATHER,
                "obj.xbows_crossbow_bolts_iron",
                level = 39,
                xp = 1.5,
                batchSize = 10,
            ),
            FletchAmmoRecipe(
                "obj.xbows_crossbow_bolts_steel_unfeathered",
                FEATHER,
                "obj.xbows_crossbow_bolts_steel",
                level = 46,
                xp = 3.5,
                batchSize = 10,
            ),
            FletchAmmoRecipe(
                "obj.xbows_crossbow_bolts_mithril_unfeathered",
                FEATHER,
                "obj.xbows_crossbow_bolts_mithril",
                level = 54,
                xp = 5.0,
                batchSize = 10,
            ),
            FletchAmmoRecipe(
                "obj.xbows_crossbow_bolts_adamantite_unfeathered",
                FEATHER,
                "obj.xbows_crossbow_bolts_adamantite",
                level = 61,
                xp = 7.0,
                batchSize = 10,
            ),
            FletchAmmoRecipe(
                "obj.xbows_crossbow_bolts_runite_unfeathered",
                FEATHER,
                "obj.xbows_crossbow_bolts_runite",
                level = 69,
                xp = 10.0,
                batchSize = 10,
            ),
            FletchAmmoRecipe(
                "obj.xbows_crossbow_bolts_silver_unfeathered",
                FEATHER,
                "obj.xbows_crossbow_bolts_silver",
                level = 43,
                xp = 2.5,
                batchSize = 10,
            ),
            FletchAmmoRecipe(
                "obj.dragon_bolts_unfeathered",
                FEATHER,
                "obj.dragon_bolts",
                level = 84,
                xp = 12.0,
                batchSize = 10,
            ),
            FletchAmmoRecipe("obj.bolts", "obj.opal_bolttips", "obj.opal_bolt", level = 11, xp = 1.6, batchSize = 10),
            FletchAmmoRecipe(
                "obj.xbows_crossbow_bolts_blurite",
                "obj.xbows_bolt_tips_jade",
                "obj.xbows_crossbow_bolts_blurite_tipped_jade",
                level = 26,
                xp = 2.4,
                batchSize = 10,
            ),
            FletchAmmoRecipe(
                "obj.xbows_crossbow_bolts_iron",
                "obj.pearl_bolttips",
                "obj.pearl_bolt",
                level = 41,
                xp = 3.2,
                batchSize = 10,
            ),
            FletchAmmoRecipe(
                "obj.xbows_crossbow_bolts_steel",
                "obj.xbows_bolt_tips_redtopaz",
                "obj.xbows_crossbow_bolts_steel_tipped_redtopaz",
                level = 48,
                xp = 3.9,
                batchSize = 10,
            ),
            FletchAmmoRecipe(
                "obj.xbows_crossbow_bolts_mithril",
                "obj.xbows_bolt_tips_sapphire",
                "obj.xbows_crossbow_bolts_mithril_tipped_sapphire",
                level = 56,
                xp = 4.7,
                batchSize = 10,
            ),
            FletchAmmoRecipe(
                "obj.xbows_crossbow_bolts_mithril",
                "obj.xbows_bolt_tips_emerald",
                "obj.xbows_crossbow_bolts_mithril_tipped_emerald",
                level = 58,
                xp = 5.5,
                batchSize = 10,
            ),
            FletchAmmoRecipe(
                "obj.xbows_crossbow_bolts_adamantite",
                "obj.xbows_bolt_tips_ruby",
                "obj.xbows_crossbow_bolts_adamantite_tipped_ruby",
                level = 63,
                xp = 6.3,
                batchSize = 10,
            ),
            FletchAmmoRecipe(
                "obj.xbows_crossbow_bolts_adamantite",
                "obj.xbows_bolt_tips_diamond",
                "obj.xbows_crossbow_bolts_adamantite_tipped_diamond",
                level = 65,
                xp = 7.0,
                batchSize = 10,
            ),
            FletchAmmoRecipe(
                "obj.xbows_crossbow_bolts_runite",
                "obj.xbows_bolt_tips_dragonstone",
                "obj.xbows_crossbow_bolts_runite_tipped_dragonstone",
                level = 71,
                xp = 8.2,
                batchSize = 10,
            ),
            FletchAmmoRecipe(
                "obj.xbows_crossbow_bolts_runite",
                "obj.xbows_bolt_tips_onyx",
                "obj.xbows_crossbow_bolts_runite_tipped_onyx",
                level = 73,
                xp = 9.4,
                batchSize = 10,
            ),
        )

    val logTypes: Set<String> = carveRecipes.mapTo(mutableSetOf()) { it.log }
    val unstrungBows: Set<String> = stringBowRecipes.mapTo(mutableSetOf()) { it.unstrung }
    val crossbowAssemblyParts: Set<String> =
        crossbowAssemblyRecipes.flatMapTo(mutableSetOf()) { listOf(it.stock, it.limbs) }

    fun carveRecipesFor(log: String): List<CarveRecipe> = carveRecipes.filter { it.log == log }

    fun stringRecipeFor(unstrung: String): StringBowRecipe? =
        stringBowRecipes.firstOrNull { it.unstrung == unstrung }

    fun crossbowAssemblyRecipeFor(first: String, second: String): CrossbowAssemblyRecipe? =
        crossbowAssemblyRecipes.firstOrNull {
            (it.stock == first && it.limbs == second) || (it.stock == second && it.limbs == first)
        }

    fun materials(recipe: CarveRecipe): List<Material> = listOf(Material(recipe.log))

    fun materials(recipe: StringBowRecipe): List<Material> =
        listOf(Material(recipe.unstrung), Material(recipe.string))

    fun materials(recipe: CrossbowAssemblyRecipe): List<Material> =
        listOf(Material(recipe.stock), Material(recipe.limbs))

    fun wolfboneArrowheadMaterials(): List<Material> = listOf(Material(WOLF_BONES))
}
