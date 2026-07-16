package org.rsmod.content.other.leagues

object LeagueConstants {
    const val CURRENT_LEAGUE_ID = "league_6_demonic_pacts"
    const val CURRENT_LEAGUE_NAME = "Leagues VI: Demonic Pacts"
    const val CURRENT_LEAGUE_TYPE = 6
    const val PROFILE_SCHEMA_VERSION = 1
    const val MAX_PACT_POINTS = 40
    const val MAX_ECHO_BOSS_PACT_RESETS = 3
}

object LeagueIconIds {
    const val LEAGUE_6_NAME_BADGE = 59
}

object LeagueInterfaces {
    const val INFO = 654
    const val RELICS = 655
    const val SIDE_PANEL = 656
    const val TASKS = 657
    const val TALENT_TREE = 647
    const val COMBAT_MASTERY = 311
    const val TRAILBLAZER_AREAS = 512
}

object LeagueVarps {
    const val LEAGUE_GENERAL = 2606
    const val MAP_FLAGS_CACHED = 3717
    const val LEAGUE_POINTS_COMPLETED = 2614
    const val LEAGUE_POINTS_CLAIMED = 2615
    const val LEAGUE_6_POINTS = 5507
    const val LEAGUE_6_REWARDS = 5582
    const val LEAGUE_6_TASKS_1 = 5530
    const val LEAGUE_6_TASKS_2 = 5531
    const val LEAGUE_6_TASKS_3 = 5532
    const val LEAGUE_6_TEMP_1 = 5533
    const val LEAGUE_RELICS = 2632
    const val LEAGUE_RELICS_OTHER = 4558
    const val EVENT_LEAGUE_RELICS = 4910
    const val LEAGUE_COMBAT_MASTERY_PATHS = 4566
    const val LEAGUE_COMBAT_MASTERY_POINTS = 4567
    const val LEAGUE_COMBAT_MASTERY_POINTS_EXTRA = 4568
    const val LEAGUE_COMBAT_MASTERY_TEMP = 4569
    const val LEAGUE_COMBAT_MASTERY_INTERFACE = 4570
    const val TALENT_POINTS_EARNED = 5484
    const val TALENT_POINTS_SPENT = 5486
    const val COMBAT_MASTERY_PERM_0 = 5498
    const val COMBAT_MASTERY_PERM_1 = 5499
    const val COMBAT_MASTERY_PERM_2 = 5500
    const val COMBAT_MASTERY_PERM_3 = 5501
    const val COMBAT_MASTERY_PERM_4 = 5502
    const val COMBAT_MASTERY_PERM_5 = 5503

    val LEAGUE_TASK_COMPLETED =
        intArrayOf(
            2616,
            2617,
            2618,
            2619,
            2620,
            2621,
            2622,
            2623,
            2624,
            2625,
            2626,
            2627,
            2628,
            2629,
            2630,
            2631,
            2808,
            2809,
            2810,
            2811,
            2812,
            2813,
            2814,
            2815,
            2816,
            2817,
            2818,
            2819,
            2820,
            2821,
            2822,
            2823,
            2824,
            2825,
            2826,
            2827,
            2828,
            2829,
            2830,
            2831,
            2832,
            2833,
            2834,
            2835,
            3339,
            3340,
            3341,
            3342,
            4036,
            4037,
            4038,
            4039,
            4040,
            4041,
            4042,
            4043,
            4044,
            4045,
            4046,
            4047,
            4048,
            4049,
        )
}

object LeagueVarbits {
    const val LEAGUE_ACCOUNT = 10031
    const val LEAGUE_TYPE = 10032
    const val LEAGUE_TUTORIAL_COMPLETED = 10037
    const val LEAGUE_TOTAL_TASKS_COMPLETED = 10038
    const val LEAGUE_COMBAT_MASTERY_POINTS_TO_SPEND = 11583
    const val LEAGUE_COMBAT_MASTERY_POINTS_EARNED = 11584
    const val LEAGUE_COMBAT_MASTERY_TUTORIAL = 17553
    const val TALENT_RESETS_AVAILABLE = 20260

    val LEAGUE_RELIC_SELECTION =
        intArrayOf(
            10049,
            10050,
            10051,
            10052,
            10053,
            11696,
            17301,
            17302,
        )
}

object LeagueClientFlags {
    const val LEAGUE_TUTORIAL_COMPLETE_STAGE = 14
    const val STARTER_RELIC_PROGRESS_PLACEHOLDER = 7
    const val LEAGUE_WORLD_MAP_FLAG = 1 shl 30
    const val DEADMAN_WORLD_MAP_FLAG = 1 shl 29
}
