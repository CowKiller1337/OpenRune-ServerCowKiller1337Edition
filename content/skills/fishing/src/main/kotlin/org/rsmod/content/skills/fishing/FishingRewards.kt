package org.rsmod.content.skills.fishing

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.entity.Player

internal object FishingRewards {
    private val radaBlessings =
        mapOf(
            "obj.zeah_blessing_easy" to 2,
            "obj.zeah_blessing_medium" to 4,
            "obj.zeah_blessing_hard" to 6,
            "obj.zeah_blessing_elite" to 8,
        )

    private val clueBottles =
        listOf(
            "obj.fishing_clue_bottle_easy",
            "obj.fishing_clue_bottle_medium",
            "obj.fishing_clue_bottle_hard",
            "obj.fishing_clue_bottle_elite",
            "obj.fishing_clue_bottle_beginner",
        )

    private val infernalHarpoons =
        setOf(
            "obj.infernal_harpoon",
            "obj.trailblazer_harpoon",
            "obj.league_trailblazer_harpoon",
            "obj.trailblazer_reloaded_harpoon",
        )

    fun finalCatchObj(player: Player, catch: FishingCatch): String {
        if (!player.hasInfernalHarpoon()) {
            return catch.obj
        }
        return FishingData.rawToCookedFish[catch.obj] ?: catch.obj
    }

    fun ProtectedAccess.extraCatchCount(method: FishingMethod, catch: FishingCatch, baseCount: Int): Int {
        if (!catch.bonusEligible) {
            return 0
        }
        var extra = 0
        if (method.allowRadasBlessing && rollRadaDoubleFish()) {
            extra += baseCount
        }
        if ("obj.spirit_flakes" in inv && random.of(maxExclusive = 100) < 50) {
            if (invDel(inv, "obj.spirit_flakes", ignoreVirtualStorage = true).success) {
                extra += baseCount
            }
        }
        return extra
    }

    fun ProtectedAccess.rollTertiaryRewards(catch: FishingCatch) {
        if (catch.obj in clueBottleRollSources) {
            rollClueBottle()
        }
        if (catch.obj in heronRollSources && random.of(heronChance(catch.obj)) == 0) {
            if (invAdd(inv, "obj.skillpetfish", strict = false).success) {
                mes("<col=ff0000>You have a funny feeling like you're being followed.</col>")
            }
        }
    }

    private fun ProtectedAccess.rollClueBottle() {
        val bottle =
            when {
                random.of(maxExclusive = 2_500) == 0 -> clueBottles[0]
                random.of(maxExclusive = 5_000) == 0 -> clueBottles[1]
                random.of(maxExclusive = 10_000) == 0 -> clueBottles[2]
                random.of(maxExclusive = 20_000) == 0 -> clueBottles[3]
                random.of(maxExclusive = 10_000) == 0 -> clueBottles[4]
                else -> return
            }
        if (invAdd(inv, bottle, strict = false).success) {
            mes("A clue bottle washes up in your catch.")
        }
    }

    private fun heronChance(obj: String): Int =
        when (obj) {
            "obj.minnow" -> 977_778
            "obj.infernal_eel" -> 426_954
            "obj.raw_dark_crab" -> 149_434
            "obj.raw_anglerfish" -> 202_252
            "obj.raw_shark" -> 82_243
            "obj.raw_monkfish" -> 138_583
            "obj.raw_swordfish" -> 82_243
            "obj.raw_lobster" -> 116_129
            else -> 426_954
        }

    private fun Player.hasInfernalHarpoon(): Boolean =
        infernalHarpoons.any { it in inv || it in worn }

    private fun ProtectedAccess.rollRadaDoubleFish(): Boolean {
        val chance = radaBlessings.entries.firstOrNull { (obj, _) -> obj in worn }?.value ?: return false
        return random.of(maxExclusive = 100) < chance
    }

    private val clueBottleRollSources =
        setOf(
            "obj.raw_shrimp",
            "obj.raw_anchovies",
            "obj.raw_sardine",
            "obj.raw_herring",
            "obj.raw_trout",
            "obj.raw_salmon",
            "obj.raw_pike",
            "obj.raw_lobster",
            "obj.raw_tuna",
            "obj.raw_swordfish",
            "obj.raw_shark",
            "obj.raw_mackerel",
            "obj.raw_cod",
            "obj.raw_bass",
            "obj.raw_monkfish",
            "obj.raw_cave_eel",
            "obj.raw_dark_crab",
            "obj.raw_anglerfish",
        )

    private val heronRollSources = clueBottleRollSources + "obj.minnow" + "obj.infernal_eel"
}
