package org.rsmod.content.skills.fishing

private const val FISHING_CAST_SOUND = 2600
private const val FISH_SWIM_SOUND = 2601
private const val LAVA_CAST_SOUND = 2602
private const val NET_SOUND = 2603

internal data class FishingMethod(
    val tools: List<String>,
    val toolName: String,
    val level: Int,
    val animation: Int,
    val startMessage: String,
    val baitName: String? = null,
    val catches: List<FishingCatch>,
    val attemptTicks: Int = 4,
    val attemptSound: Int? = FISHING_CAST_SOUND,
    val catchSound: Int? = FISH_SWIM_SOUND,
)

internal data class FishingCatch(
    val obj: String,
    val level: Int,
    val xp: Double,
    val low: Int,
    val high: Int,
    val message: String,
    val baits: List<String> = emptyList(),
    val count: IntRange = 1..1,
    val extraXp: List<FishingExtraXp> = emptyList(),
) {
    constructor(
        obj: String,
        level: Int,
        xp: Double,
        low: Int,
        high: Int,
        message: String,
        bait: String,
    ) : this(obj, level, xp, low, high, message, listOf(bait))
}

internal data class FishingExtraXp(val stat: String, val xp: Double)

internal data class FishingSpotOption(val npc: Int, val op: Int, val method: FishingMethod)

internal object FishingData {
    private const val NET_ANIM = 621
    private const val ROD_ANIM = 622
    private const val HARPOON_ANIM = 618
    private const val CAGE_ANIM = 619
    private const val BIG_NET_ANIM = 620
    private const val KARAMBWAN_ANIM = 1193
    private const val BARBARIAN_ROD_ANIM = 9350
    private const val SLOW_ATTEMPT_TICKS = 5

    private val fishingRodTools =
        listOf("obj.fishing_rod", "obj.fishingrod_pearl")

    private val flyFishingRodTools =
        listOf("obj.fly_fishing_rod", "obj.fishingrod_pearl_fly")

    private val oilyFishingRodTools =
        listOf("obj.oily_fishing_rod", "obj.fishingrod_pearl_oily")

    private val barbarianRodTools =
        listOf("obj.brut_fishing_rod", "obj.fishingrod_pearl_brut")

    private val barbarianBaits =
        listOf(
            "obj.feather",
            "obj.fishing_bait",
            "obj.fish_chunks",
            "obj.brut_roe",
            "obj.brut_caviar",
        )

    private val harpoonTools =
        listOf(
            "obj.harpoon",
            "obj.hunting_barbed_harpoon",
            "obj.dragon_harpoon",
            "obj.infernal_harpoon",
            "obj.infernal_harpoon_empty",
            "obj.crystal_harpoon",
            "obj.crystal_harpoon_inactive",
            "obj.trailblazer_harpoon",
            "obj.league_trailblazer_harpoon",
            "obj.trailblazer_harpoon_empty",
            "obj.trailblazer_harpoon_no_infernal",
            "obj.trailblazer_reloaded_harpoon",
            "obj.trailblazer_reloaded_harpoon_empty",
            "obj.trailblazer_reloaded_harpoon_no_infernal",
        )

    val smallNet =
        FishingMethod(
            tools = listOf("obj.net"),
            toolName = "small fishing net",
            level = 1,
            animation = NET_ANIM,
            startMessage = "You cast out your net...",
            catches =
                listOf(
                    FishingCatch("obj.raw_shrimp", 1, 10.0, 48, 256, "You catch some shrimps."),
                    FishingCatch("obj.raw_anchovies", 15, 40.0, 24, 128, "You catch some anchovies."),
                ),
            attemptSound = NET_SOUND,
        )

    val seaBait =
        FishingMethod(
            tools = fishingRodTools,
            toolName = "fishing rod",
            level = 5,
            animation = ROD_ANIM,
            startMessage = "You attempt to catch a fish.",
            baitName = "fishing bait",
            catches =
                listOf(
                    FishingCatch("obj.raw_sardine", 5, 20.0, 32, 192, "You catch a sardine.", "obj.fishing_bait"),
                    FishingCatch("obj.raw_herring", 10, 30.0, 24, 128, "You catch a herring.", "obj.fishing_bait"),
                ),
        )

    val lure =
        FishingMethod(
            tools = flyFishingRodTools,
            toolName = "fly fishing rod",
            level = 20,
            animation = ROD_ANIM,
            startMessage = "You attempt to catch a fish.",
            baitName = "feathers",
            catches =
                listOf(
                    FishingCatch("obj.raw_trout", 20, 50.0, 32, 192, "You catch a trout.", "obj.feather"),
                    FishingCatch("obj.raw_salmon", 30, 70.0, 16, 96, "You catch a salmon.", "obj.feather"),
                    FishingCatch(
                        "obj.hunting_raw_fish_special",
                        38,
                        80.0,
                        28,
                        64,
                        "You catch a rainbow fish.",
                        "obj.hunting_stripy_bird_feather",
                    ),
                ),
        )

    val pikeBait =
        FishingMethod(
            tools = fishingRodTools,
            toolName = "fishing rod",
            level = 25,
            animation = ROD_ANIM,
            startMessage = "You attempt to catch a fish.",
            baitName = "fishing bait",
            catches = listOf(FishingCatch("obj.raw_pike", 25, 60.0, 16, 96, "You catch a pike.", "obj.fishing_bait")),
        )

    val lobsterCage =
        FishingMethod(
            tools = listOf("obj.lobster_pot"),
            toolName = "lobster pot",
            level = 40,
            animation = CAGE_ANIM,
            startMessage = "You attempt to catch a lobster.",
            catches = listOf(FishingCatch("obj.raw_lobster", 40, 90.0, 6, 95, "You catch a lobster.")),
        )

    val tunaSwordfish =
        FishingMethod(
            tools = harpoonTools,
            toolName = "harpoon",
            level = 35,
            animation = HARPOON_ANIM,
            startMessage = "You attempt to catch a fish.",
            catches =
                listOf(
                    FishingCatch("obj.raw_tuna", 35, 80.0, 8, 64, "You catch a tuna."),
                    FishingCatch("obj.raw_swordfish", 50, 100.0, 4, 48, "You catch a swordfish."),
                ),
            attemptTicks = SLOW_ATTEMPT_TICKS,
        )

    val darkCrabCage =
        FishingMethod(
            tools = listOf("obj.lobster_pot"),
            toolName = "lobster pot",
            level = 85,
            animation = CAGE_ANIM,
            startMessage = "You attempt to catch a dark crab.",
            baitName = "dark fishing bait",
            catches =
                listOf(
                    FishingCatch(
                        "obj.raw_dark_crab",
                        85,
                        130.0,
                        16,
                        32,
                        "You catch a dark crab.",
                        "obj.wilderness_fishing_bait",
                    )
                ),
            attemptTicks = SLOW_ATTEMPT_TICKS,
        )

    val bigNet =
        FishingMethod(
            tools = listOf("obj.big_net"),
            toolName = "big fishing net",
            level = 16,
            animation = BIG_NET_ANIM,
            startMessage = "You cast out your net...",
            catches =
                listOf(
                    FishingCatch("obj.raw_mackerel", 16, 20.0, 5, 65, "You catch a mackerel."),
                    FishingCatch("obj.raw_cod", 23, 45.0, 4, 55, "You catch a cod."),
                    FishingCatch("obj.raw_bass", 46, 100.0, 3, 40, "You catch a bass."),
                    FishingCatch("obj.seaweed", 16, 1.0, 10, 10, "You catch some seaweed."),
                    FishingCatch("obj.leather_boots", 16, 1.0, 10, 10, "You fish up some leather boots."),
                    FishingCatch("obj.leather_gloves", 16, 1.0, 10, 10, "You fish up some leather gloves."),
                    FishingCatch("obj.oystershell", 16, 10.0, 3, 7, "You catch an oyster."),
                    FishingCatch("obj.casket", 16, 0.0, 1, 2, "You fish up a casket."),
                ),
            attemptSound = NET_SOUND,
        )

    val barbarianRod =
        FishingMethod(
            tools = barbarianRodTools,
            toolName = "barbarian rod",
            level = 48,
            animation = BARBARIAN_ROD_ANIM,
            startMessage = "You attempt to catch a fish.",
            baitName = "feathers or fishing bait",
            catches =
                listOf(
                    FishingCatch(
                        obj = "obj.brut_spawning_trout",
                        level = 48,
                        xp = 50.0,
                        low = 32,
                        high = 192,
                        message = "You catch a leaping trout.",
                        baits = barbarianBaits,
                        extraXp =
                            listOf(
                                FishingExtraXp("stat.strength", 5.0),
                                FishingExtraXp("stat.agility", 5.0),
                            ),
                    ),
                    FishingCatch(
                        obj = "obj.brut_spawning_salmon",
                        level = 58,
                        xp = 70.0,
                        low = 16,
                        high = 96,
                        message = "You catch a leaping salmon.",
                        baits = barbarianBaits,
                        extraXp =
                            listOf(
                                FishingExtraXp("stat.strength", 6.0),
                                FishingExtraXp("stat.agility", 6.0),
                            ),
                    ),
                    FishingCatch(
                        obj = "obj.brut_sturgeon",
                        level = 70,
                        xp = 80.0,
                        low = 28,
                        high = 64,
                        message = "You catch a leaping sturgeon.",
                        baits = barbarianBaits,
                        extraXp =
                            listOf(
                                FishingExtraXp("stat.strength", 7.0),
                                FishingExtraXp("stat.agility", 7.0),
                            ),
                    ),
                ),
        )

    val sharkHarpoon =
        FishingMethod(
            tools = harpoonTools,
            toolName = "harpoon",
            level = 76,
            animation = HARPOON_ANIM,
            startMessage = "You attempt to catch a fish.",
            catches = listOf(FishingCatch("obj.raw_shark", 76, 110.0, 3, 40, "You catch a shark.")),
            attemptTicks = SLOW_ATTEMPT_TICKS,
        )

    val monkfishNet =
        FishingMethod(
            tools = listOf("obj.net"),
            toolName = "small fishing net",
            level = 62,
            animation = NET_ANIM,
            startMessage = "You cast out your net...",
            catches = listOf(FishingCatch("obj.raw_monkfish", 62, 120.0, 74, 90, "You catch a monkfish.")),
            attemptTicks = SLOW_ATTEMPT_TICKS,
            attemptSound = NET_SOUND,
        )

    val swampNet =
        FishingMethod(
            tools = listOf("obj.net"),
            toolName = "small fishing net",
            level = 33,
            animation = NET_ANIM,
            startMessage = "You cast out your net...",
            catches = listOf(FishingCatch("obj.giant_frogspawn", 33, 75.0, 16, 96, "You catch some frog spawn.")),
            attemptSound = NET_SOUND,
        )

    val swampBait =
        FishingMethod(
            tools = fishingRodTools,
            toolName = "fishing rod",
            level = 28,
            animation = ROD_ANIM,
            startMessage = "You attempt to catch a fish.",
            baitName = "fishing bait",
            catches =
                listOf(
                    FishingCatch("obj.mort_slimey_eel", 28, 65.0, 10, 80, "You catch a slimy eel.", "obj.fishing_bait"),
                    FishingCatch("obj.raw_cave_eel", 38, 80.0, 37, 80, "You catch a cave eel.", "obj.fishing_bait"),
                ),
        )

    val karambwanjiNet =
        FishingMethod(
            tools = listOf("obj.net"),
            toolName = "small fishing net",
            level = 5,
            animation = NET_ANIM,
            startMessage = "You cast out your net...",
            catches = listOf(FishingCatch("obj.tbwt_raw_karambwanji", 5, 5.0, 100, 250, "You catch a raw karambwanji.")),
            attemptSound = NET_SOUND,
        )

    val karambwan =
        FishingMethod(
            tools = listOf("obj.tbwt_karambwan_vessel"),
            toolName = "karambwan vessel",
            level = 65,
            animation = KARAMBWAN_ANIM,
            startMessage = "You attempt to catch a karambwan.",
            baitName = "raw karambwanji",
            catches =
                listOf(
                    FishingCatch(
                        "obj.tbwt_raw_karambwan",
                        65,
                        105.0,
                        105,
                        160,
                        "You catch a raw karambwan.",
                        "obj.tbwt_raw_karambwanji",
                    )
                ),
            attemptSound = NET_SOUND,
        )

    val lavaEel =
        FishingMethod(
            tools = oilyFishingRodTools,
            toolName = "oily fishing rod",
            level = 53,
            animation = ROD_ANIM,
            startMessage = "You attempt to catch a fish.",
            baitName = "fishing bait",
            catches = listOf(FishingCatch("obj.raw_lava_eel", 53, 60.0, 16, 96, "You catch a lava eel.", "obj.fishing_bait")),
            attemptTicks = SLOW_ATTEMPT_TICKS,
            attemptSound = LAVA_CAST_SOUND,
        )

    val infernalEel =
        FishingMethod(
            tools = oilyFishingRodTools,
            toolName = "oily fishing rod",
            level = 80,
            animation = ROD_ANIM,
            startMessage = "You attempt to catch a fish.",
            baitName = "fishing bait",
            catches =
                listOf(
                    FishingCatch(
                        "obj.infernal_eel",
                        80,
                        95.0,
                        28,
                        55,
                        "You catch an infernal eel.",
                        "obj.fishing_bait",
                    )
                ),
            attemptTicks = SLOW_ATTEMPT_TICKS,
            attemptSound = LAVA_CAST_SOUND,
        )

    val minnowNet =
        FishingMethod(
            tools = listOf("obj.net"),
            toolName = "small fishing net",
            level = 82,
            animation = NET_ANIM,
            startMessage = "You cast out your net...",
            catches =
                listOf(
                    FishingCatch(
                        obj = "obj.minnow",
                        level = 82,
                        xp = 26.1,
                        low = 75,
                        high = 95,
                        message = "You catch some minnows.",
                        count = 10..14,
                    )
                ),
            attemptSound = NET_SOUND,
        )

    val temporossHarpoonfish =
        FishingMethod(
            tools = harpoonTools,
            toolName = "harpoon",
            level = 35,
            animation = HARPOON_ANIM,
            startMessage = "You attempt to catch a harpoonfish.",
            catches =
                listOf(
                    FishingCatch(
                        "obj.tempoross_raw_harpoonfish",
                        35,
                        55.0,
                        45,
                        95,
                        "You catch a raw harpoonfish.",
                    )
                ),
            attemptTicks = SLOW_ATTEMPT_TICKS,
        )

    val anglerfish =
        FishingMethod(
            tools = fishingRodTools,
            toolName = "fishing rod",
            level = 82,
            animation = ROD_ANIM,
            startMessage = "You attempt to catch a fish.",
            baitName = "sandworms",
            catches =
                listOf(
                    FishingCatch(
                        "obj.raw_anglerfish",
                        82,
                        120.0,
                        25,
                        40,
                        "You catch an anglerfish.",
                        "obj.piscarilius_sandworms",
                    )
                ),
        )

    private val freshwaterSpots =
        listOf(
            394,
            1506,
            1507,
            1508,
            1509,
            1512,
            1513,
            1515,
            1516,
            1526,
            1527,
            1529,
            1531,
            3417,
            3418,
            7463,
            7464,
            7468,
            8524,
            12774,
            14036,
            14521,
            14522,
            14525,
            14526,
            14527,
            14528,
            15072,
            15073,
        )
    private val seaNetBaitSpots =
        listOf(
            1514,
            1517,
            1518,
            1521,
            1523,
            1524,
            1525,
            1528,
            1530,
            1532,
            1544,
            3913,
            7155,
            7459,
            7462,
            7467,
            7469,
            7947,
            10513,
            12778,
            14038,
            14040,
            14041,
            14524,
            15066,
        )
    private val caveNetBaitSpots = listOf(1497, 1498, 1499, 1500, 10653)
    private val cageHarpoonSpots =
        listOf(
            1510,
            1519,
            1522,
            1533,
            2146,
            3657,
            3914,
            5820,
            7199,
            7460,
            7465,
            7470,
            7946,
            9173,
            9174,
            10515,
            10635,
            12777,
            14039,
            15070,
            15071,
            15075,
            15076,
            15079,
            15084,
            15086,
        )
    private val bigNetHarpoonSpots =
        listOf(
            1511,
            1520,
            1534,
            3419,
            3915,
            4476,
            4477,
            5233,
            5234,
            5821,
            7200,
            7461,
            7466,
            8525,
            8526,
            8527,
            9171,
            9172,
            10514,
            12775,
            12776,
            14037,
            14523,
            15067,
            15068,
            15069,
            15077,
            15080,
            15082,
            15083,
            15087,
        )
    private val monkfishSpots = listOf(4316)
    private val darkCrabSpots = listOf(1535, 1536)
    private val barbarianSpots = listOf(1542, 7323)
    private val karambwanjiSpots = listOf(4710, 4711)
    private val karambwanSpots = listOf(4712, 4713, 4714)
    private val lavaEelSpots = listOf(4928, 6784, 15384)
    private val infernalEelSpots = listOf(7676)
    private val minnowSpots = listOf(7730, 7731, 7732, 7733)
    private val temporossHarpoonfishSpots = listOf(10565, 10568, 10569, 10571)
    private val slimyEelSpots = listOf(2653, 2654, 2655)
    private val anglerfishSpots = listOf(6825)
    private val newPlayerNetSpots = listOf(3317, 9478)
    private val unsupportedStationarySpots =
        listOf(
            4079,
            4080,
            4081,
            4082,
            6488,
            6731,
            8523,
            10686,
            10687,
            10688,
            12267,
            13329,
            13912,
            14035,
            15074,
            15078,
            15081,
            15085,
            15383,
        )

    val spotOptions: List<FishingSpotOption> =
        buildList {
            freshwaterSpots.forEach {
                add(FishingSpotOption(it, op = 1, lure))
                add(FishingSpotOption(it, op = 3, pikeBait))
            }
            seaNetBaitSpots.forEach {
                add(FishingSpotOption(it, op = 1, smallNet))
                add(FishingSpotOption(it, op = 3, seaBait))
            }
            caveNetBaitSpots.forEach {
                add(FishingSpotOption(it, op = 1, swampNet))
                add(FishingSpotOption(it, op = 3, swampBait))
            }
            cageHarpoonSpots.forEach {
                add(FishingSpotOption(it, op = 1, lobsterCage))
                add(FishingSpotOption(it, op = 3, tunaSwordfish))
            }
            bigNetHarpoonSpots.forEach {
                add(FishingSpotOption(it, op = 1, bigNet))
                add(FishingSpotOption(it, op = 3, sharkHarpoon))
            }
            monkfishSpots.forEach {
                add(FishingSpotOption(it, op = 1, monkfishNet))
                add(FishingSpotOption(it, op = 3, tunaSwordfish))
            }
            darkCrabSpots.forEach { add(FishingSpotOption(it, op = 1, darkCrabCage)) }
            barbarianSpots.forEach { add(FishingSpotOption(it, op = 1, barbarianRod)) }
            karambwanjiSpots.forEach { add(FishingSpotOption(it, op = 1, karambwanjiNet)) }
            karambwanSpots.forEach { add(FishingSpotOption(it, op = 1, karambwan)) }
            lavaEelSpots.forEach { add(FishingSpotOption(it, op = 1, lavaEel)) }
            infernalEelSpots.forEach { add(FishingSpotOption(it, op = 1, infernalEel)) }
            minnowSpots.forEach { add(FishingSpotOption(it, op = 1, minnowNet)) }
            temporossHarpoonfishSpots.forEach {
                add(FishingSpotOption(it, op = 1, temporossHarpoonfish))
            }
            slimyEelSpots.forEach { add(FishingSpotOption(it, op = 1, swampBait)) }
            anglerfishSpots.forEach { add(FishingSpotOption(it, op = 1, anglerfish)) }
            newPlayerNetSpots.forEach { add(FishingSpotOption(it, op = 1, smallNet)) }
        }

    val stationarySpotIds: Set<Int> = (spotOptions.map { it.npc } + unsupportedStationarySpots).toSet()
}
