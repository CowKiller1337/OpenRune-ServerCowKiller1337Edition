package org.rsmod.content.skills.hunter

internal data class ImplingCatchRecipe(
    val npc: String,
    val capturedJar: String,
    val level: Int,
    val xp: Double,
    val lowChance: Int,
    val highChance: Int,
    val respawnTicks: Int = 100,
)

internal data class ButterflyCatchRecipe(
    val npc: String,
    val capturedJar: String,
    val level: Int,
    val xp: Double,
    val lowChance: Int,
    val highChance: Int,
    val respawnTicks: Int = 70,
)

internal data class DirectHunterCatchRecipe(
    val npc: String,
    val displayName: String,
    val level: Int,
    val xp: Double,
    val lowChance: Int,
    val highChance: Int,
    val reward: String,
    val rewardCount: Int = 1,
    val requiredTools: List<String>,
    val respawnTicks: Int = 90,
)

internal data class HunterTrapRecipe(
    val tool: String,
    val setupLocId: Int,
    val caughtLocId: Int,
    val failedLocId: Int,
    val level: Int,
    val xp: Double,
    val lowChance: Int,
    val highChance: Int,
    val reward: String,
    val rewardCount: Int = 1,
    val setupTicks: Int = 2,
    val triggerTicks: Int = 8,
    val expireTicks: Int = 100,
    val portable: Boolean = true,
    val returnsTool: Boolean = true,
)

internal data class HunterTrapTarget(
    val npc: String,
    val level: Int,
    val xp: Double,
    val lowChance: Int,
    val highChance: Int,
    val reward: String,
    val rewardCount: Int = 1,
)

internal enum class HunterTrapResult {
    Waiting,
    Caught,
    Failed,
}

internal data class HunterTrapState(
    val recipeIndex: Int,
    val result: HunterTrapResult,
    val reward: String? = null,
    val rewardCount: Int = 0,
    val xp: Double = 0.0,
    val restoreLocId: Int? = null,
)

internal data class HunterTrapTask(
    val recipeIndex: Int,
    val coords: Int,
    val expire: Boolean = false,
)

internal object HunterDefinitions {
    const val BUTTERFLY_NET = "obj.hunting_butterfly_net"
    const val MAGIC_BUTTERFLY_NET = "obj.ii_magic_butterfly_net"
    const val BUTTERFLY_JAR = "obj.butterfly_jar"
    const val IMPLING_JAR = "obj.ii_impling_jar"
    const val INACTIVE_NPC = "npc.butterfly_inactive"
    const val BIRD_SNARE = "obj.hunting_ojibway_bird_snare"
    const val BIRD_SNARE_PACK = "obj.pack_ojibway_bird_snare"
    const val BOX_TRAP = "obj.hunting_box_trap"
    const val BOX_TRAP_PACK = "obj.pack_box_trap"
    const val DEADFALL_LOGS = "obj.logs"
    const val HUNTING_NET = "obj.hunting_snare"
    const val ROPE = "obj.rope"
    const val TEASING_STICK = "obj.hunting_teasing_stick"

    val deadfallBoulderLocIds = listOf(19215, 19216)

    val butterflyCatchRecipes =
        listOf(
            ButterflyCatchRecipe("npc.butterfly_ruby", "obj.butterfly_jar_ruby", 15, 24.0, 112, 255),
            ButterflyCatchRecipe("npc.butterfly_glacialis", "obj.butterfly_jar_glacialis", 25, 34.0, 96, 245),
            ButterflyCatchRecipe("npc.butterfly_snowy", "obj.butterfly_jar_snowy", 35, 44.0, 80, 235),
            ButterflyCatchRecipe("npc.butterfly_warlock", "obj.butterfly_jar_warlock", 45, 54.0, 64, 225),
            ButterflyCatchRecipe("npc.moth_sunlight", "obj.butterfly_jar_sunmoth", 65, 74.0, 20, 296),
            ButterflyCatchRecipe("npc.moth_moonlight", "obj.butterfly_jar_moonmoth", 75, 84.0, 0, 276),
        )

    val implingCatchRecipes =
        listOf(
            ImplingCatchRecipe("npc.ii_impling_type_1", "obj.ii_captured_impling_1", 17, 20.0, 96, 255),
            ImplingCatchRecipe("npc.ii_impling_type_2", "obj.ii_captured_impling_2", 22, 48.0, 84, 248),
            ImplingCatchRecipe("npc.ii_impling_type_3", "obj.ii_captured_impling_3", 28, 82.0, 72, 240),
            ImplingCatchRecipe("npc.ii_impling_type_4", "obj.ii_captured_impling_4", 36, 126.0, 60, 232),
            ImplingCatchRecipe("npc.ii_impling_type_5", "obj.ii_captured_impling_5", 42, 160.0, 52, 224),
            ImplingCatchRecipe("npc.ii_impling_type_6", "obj.ii_captured_impling_6", 50, 205.0, 44, 216),
            ImplingCatchRecipe("npc.ii_impling_type_7", "obj.ii_captured_impling_7", 58, 250.0, 36, 208),
            ImplingCatchRecipe("npc.ii_impling_type_8", "obj.ii_captured_impling_8", 65, 289.0, 30, 200),
            ImplingCatchRecipe("npc.ii_impling_type_9", "obj.ii_captured_impling_9", 74, 339.0, 24, 192),
            ImplingCatchRecipe("npc.ii_impling_type_10", "obj.ii_captured_impling_10", 83, 390.0, 18, 184),
            ImplingCatchRecipe("npc.ii_impling_type_11", "obj.ii_captured_impling_11", 80, 280.0, 20, 188),
        ) + mazeImplings() + luckyImplings()

    val directCatchRecipes =
        listOf(
            DirectHunterCatchRecipe(
                npc = "npc.salamander_green",
                displayName = "green salamander",
                level = 29,
                xp = 152.0,
                lowChance = 92,
                highChance = 245,
                reward = "obj.green_salamander",
                requiredTools = listOf(HUNTING_NET, ROPE),
            ),
            DirectHunterCatchRecipe(
                npc = "npc.salamander_orange",
                displayName = "orange salamander",
                level = 47,
                xp = 224.0,
                lowChance = 76,
                highChance = 235,
                reward = "obj.orange_salamander",
                requiredTools = listOf(HUNTING_NET, ROPE),
            ),
            DirectHunterCatchRecipe(
                npc = "npc.salamander_red",
                displayName = "red salamander",
                level = 59,
                xp = 272.0,
                lowChance = 64,
                highChance = 225,
                reward = "obj.red_salamander",
                requiredTools = listOf(HUNTING_NET, ROPE),
            ),
            DirectHunterCatchRecipe(
                npc = "npc.salamander_mountain",
                displayName = "mountain salamander",
                level = 59,
                xp = 272.0,
                lowChance = 64,
                highChance = 225,
                reward = "obj.mountain_salamander",
                requiredTools = listOf(HUNTING_NET, ROPE),
            ),
            DirectHunterCatchRecipe(
                npc = "npc.salamander_black",
                displayName = "black salamander",
                level = 67,
                xp = 319.0,
                lowChance = 52,
                highChance = 215,
                reward = "obj.black_salamander",
                requiredTools = listOf(HUNTING_NET, ROPE),
            ),
            DirectHunterCatchRecipe(
                npc = "npc.hunting_jaguar",
                displayName = "pitfall jaguar",
                level = 31,
                xp = 180.0,
                lowChance = 84,
                highChance = 235,
                reward = "obj.hunting_fur_jaguar_shabby",
                requiredTools = listOf(TEASING_STICK),
            ),
            DirectHunterCatchRecipe(
                npc = "npc.hunting_leopard",
                displayName = "pitfall leopard",
                level = 41,
                xp = 240.0,
                lowChance = 72,
                highChance = 225,
                reward = "obj.hunting_fur_leopard_shabby",
                requiredTools = listOf(TEASING_STICK),
            ),
            DirectHunterCatchRecipe(
                npc = "npc.hunting_snow_tiger",
                displayName = "pitfall tiger",
                level = 55,
                xp = 300.0,
                lowChance = 60,
                highChance = 215,
                reward = "obj.hunting_fur_tiger_shabby",
                requiredTools = listOf(TEASING_STICK),
            ),
            DirectHunterCatchRecipe(
                npc = "npc.varlamore_fennecfox",
                displayName = "fennec fox",
                level = 67,
                xp = 350.0,
                lowChance = 56,
                highChance = 220,
                reward = "obj.hunting_fennecfox_fur",
                requiredTools = listOf(TEASING_STICK),
            ),
            DirectHunterCatchRecipe(
                npc = "npc.varlamore_jaguar",
                displayName = "jaguar",
                level = 74,
                xp = 450.0,
                lowChance = 48,
                highChance = 210,
                reward = "obj.varlamore_jaguar_fur",
                requiredTools = listOf(TEASING_STICK),
            ),
            DirectHunterCatchRecipe(
                npc = "npc.sunlight_antelope",
                displayName = "sunlight antelope",
                level = 72,
                xp = 450.0,
                lowChance = 50,
                highChance = 210,
                reward = "obj.hunting_antelopesun_fur",
                requiredTools = listOf(TEASING_STICK),
            ),
            DirectHunterCatchRecipe(
                npc = "npc.moonlight_antelope",
                displayName = "moonlight antelope",
                level = 91,
                xp = 750.0,
                lowChance = 24,
                highChance = 190,
                reward = "obj.hunting_antelopemoon_fur",
                requiredTools = listOf(TEASING_STICK),
            ),
        )

    private fun mazeImplings(): List<ImplingCatchRecipe> =
        listOf(
            ImplingCatchRecipe("npc.ii_impling_type_1_maze", "obj.ii_captured_impling_1", 17, 20.0, 96, 255),
            ImplingCatchRecipe("npc.ii_impling_type_2_maze", "obj.ii_captured_impling_2", 22, 48.0, 84, 248),
            ImplingCatchRecipe("npc.ii_impling_type_3_maze", "obj.ii_captured_impling_3", 28, 82.0, 72, 240),
            ImplingCatchRecipe("npc.ii_impling_type_4_maze", "obj.ii_captured_impling_4", 36, 126.0, 60, 232),
            ImplingCatchRecipe("npc.ii_impling_type_5_maze", "obj.ii_captured_impling_5", 42, 160.0, 52, 224),
            ImplingCatchRecipe("npc.ii_impling_type_6_maze", "obj.ii_captured_impling_6", 50, 205.0, 44, 216),
            ImplingCatchRecipe("npc.ii_impling_type_7_maze", "obj.ii_captured_impling_7", 58, 250.0, 36, 208),
            ImplingCatchRecipe("npc.ii_impling_type_8_maze", "obj.ii_captured_impling_8", 65, 289.0, 30, 200),
            ImplingCatchRecipe("npc.ii_impling_type_9_maze", "obj.ii_captured_impling_9", 74, 339.0, 24, 192),
            ImplingCatchRecipe("npc.ii_impling_type_10_maze", "obj.ii_captured_impling_10", 83, 390.0, 18, 184),
            ImplingCatchRecipe("npc.ii_impling_type_11_maze", "obj.ii_captured_impling_11", 80, 280.0, 20, 188),
        )

    private fun luckyImplings(): List<ImplingCatchRecipe> {
        val variants =
            listOf(
                "andy",
                "damo",
                "hingy",
                "ian",
                "jamie",
                "joey",
                "johnny",
                "junior",
                "matty",
                "neil",
                "stace",
                "steveo",
                "stewie",
                "trouble",
                "xander",
                "yanny",
                "zolty",
            )
        return variants.map { variant ->
            ImplingCatchRecipe(
                npc = "npc.ii_impling_type_12_$variant",
                capturedJar = "obj.ii_captured_impling_12",
                level = 89,
                xp = 380.0,
                lowChance = 12,
                highChance = 176,
                respawnTicks = 140,
            )
        }
    }

    val trapRecipes =
        listOf(
            HunterTrapRecipe(
                tool = BIRD_SNARE,
                setupLocId = 9345,
                caughtLocId = 9349,
                failedLocId = 9344,
                level = 1,
                xp = 34.0,
                lowChance = 112,
                highChance = 255,
                reward = "obj.feather",
                rewardCount = 5,
            ),
            HunterTrapRecipe(
                tool = BOX_TRAP,
                setupLocId = 9380,
                caughtLocId = 9382,
                failedLocId = 9385,
                level = 53,
                xp = 198.4,
                lowChance = 72,
                highChance = 240,
                reward = "obj.chinchompa_captured",
            ),
            HunterTrapRecipe(
                tool = DEADFALL_LOGS,
                setupLocId = 19217,
                caughtLocId = 28830,
                failedLocId = 19219,
                level = 23,
                xp = 128.0,
                lowChance = 80,
                highChance = 235,
                reward = "obj.huntingbeast_claws",
                triggerTicks = 8,
                expireTicks = 100,
                portable = false,
                returnsTool = false,
            ),
        )

    val trapByTool: Map<String, HunterTrapRecipe> = trapRecipes.associateBy(HunterTrapRecipe::tool)
    val trapBySetupLocId: Map<Int, HunterTrapRecipe> = trapRecipes.associateBy(HunterTrapRecipe::setupLocId)
    val trapByCaughtLocId: Map<Int, HunterTrapRecipe> = trapRecipes.associateBy(HunterTrapRecipe::caughtLocId)
    val trapByFailedLocId: Map<Int, HunterTrapRecipe> = trapRecipes.associateBy(HunterTrapRecipe::failedLocId)
    val trapIndex: Map<HunterTrapRecipe, Int> = trapRecipes.withIndex().associate { it.value to it.index }

    val birdSnareTargets =
        listOf(
            HunterTrapTarget("npc.hunting_bird_woodland", 1, 34.0, 112, 255, "obj.feather", 5),
            HunterTrapTarget("npc.hunting_bird_jungle", 5, 47.0, 104, 250, "obj.feather", 5),
            HunterTrapTarget("npc.hunting_bird_desert", 19, 95.0, 88, 240, "obj.feather", 5),
            HunterTrapTarget("npc.hunting_bird_polar", 39, 167.0, 72, 230, "obj.feather", 5),
        )

    val boxTrapTargets =
        listOf(
            HunterTrapTarget("npc.hunting_ferret", 27, 115.0, 88, 245, "obj.hunting_ferret"),
            HunterTrapTarget("npc.hunting_chinchompa", 53, 198.4, 72, 240, "obj.chinchompa_captured"),
            HunterTrapTarget("npc.hunting_chinchompa_big", 63, 265.0, 60, 230, "obj.chinchompa_big_captured"),
            HunterTrapTarget("npc.hunting_chinchompa_black", 73, 315.0, 48, 220, "obj.chinchompa_black"),
        )

    val deadfallTargets =
        listOf(
            HunterTrapTarget("npc.huntingbeast_claws", 23, 128.0, 80, 235, "obj.huntingbeast_claws"),
            HunterTrapTarget("npc.huntingbeast_barbedtail", 33, 168.0, 72, 230, "obj.hunting_barbed_harpoon"),
            HunterTrapTarget("npc.huntingbeast_spiky", 37, 204.0, 68, 226, "obj.huntingbeast_spike"),
            HunterTrapTarget("npc.huntingbeast_sabreteeth", 51, 200.0, 56, 216, "obj.huntingbeast_sabreteeth"),
        )
}
