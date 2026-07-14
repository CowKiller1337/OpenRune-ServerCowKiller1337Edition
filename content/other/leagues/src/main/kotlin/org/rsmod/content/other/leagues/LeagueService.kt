package org.rsmod.content.other.leagues

import org.rsmod.game.entity.Player

class LeagueService {
    fun applySavedState(player: Player) {
        val profile = player.leagueProfile
        applyNameBadge(player, profile)
        LeagueClientState.sync(player, profile)
    }

    fun enable(player: Player, badge: LeagueNameBadge = LeagueNameBadge.League6): LeagueProfile {
        val now = System.currentTimeMillis()
        return update(player) { profile ->
            profile.copy(
                enabled = true,
                nameBadge = badge,
                createdAtMillis = if (profile.createdAtMillis == 0L) now else profile.createdAtMillis,
            )
        }
    }

    fun disable(player: Player, clearBadge: Boolean = false): LeagueProfile =
        update(player) { profile ->
            profile.copy(
                enabled = false,
                nameBadge = if (clearBadge) LeagueNameBadge.None else profile.nameBadge,
            )
        }

    fun setLeaguePoints(player: Player, points: Int): LeagueProfile =
        update(player) { profile ->
            profile.copy(leaguePoints = points.coerceAtLeast(0))
        }

    fun addLeaguePoints(player: Player, points: Int): LeagueProfile {
        require(points >= 0) { "League point additions must be non-negative." }
        return update(player) { profile ->
            profile.copy(leaguePoints = profile.leaguePoints + points)
        }
    }

    fun completeTask(player: Player, taskId: String, points: Int = 0): Boolean {
        val normalizedTaskId = taskId.normalizedLeagueId()
        if (normalizedTaskId == null) {
            return false
        }

        var completed = false
        update(player) { profile ->
            if (normalizedTaskId in profile.completedTaskIds) {
                profile
            } else {
                completed = true
                profile.copy(
                    completedTaskIds = profile.completedTaskIds + normalizedTaskId,
                    leaguePoints = profile.leaguePoints + points.coerceAtLeast(0),
                )
            }
        }
        return completed
    }

    fun unlockRegion(player: Player, regionId: String): Boolean {
        val normalizedRegionId = regionId.normalizedLeagueId() ?: return false

        var unlocked = false
        update(player) { profile ->
            if (normalizedRegionId in profile.unlockedRegionIds) {
                profile
            } else {
                unlocked = true
                profile.copy(unlockedRegionIds = profile.unlockedRegionIds + normalizedRegionId)
            }
        }
        return unlocked
    }

    fun selectRelic(player: Player, tier: Int, relicId: String): Boolean {
        require(tier > 0) { "Relic tier must be greater than zero." }
        val normalizedRelicId = relicId.normalizedLeagueId() ?: return false

        var selected = false
        update(player) { profile ->
            if (tier in profile.selectedRelics) {
                profile
            } else {
                selected = true
                profile.copy(selectedRelics = profile.selectedRelics + (tier to normalizedRelicId))
            }
        }
        return selected
    }

    fun setPactPointsEarned(player: Player, points: Int): LeagueProfile =
        update(player) { profile ->
            val earned = points.coerceIn(0, LeagueConstants.MAX_PACT_POINTS)
            profile.copy(
                pactPointsEarned = earned,
                pactPointsSpent = profile.pactPointsSpent.coerceAtMost(earned),
            )
        }

    fun addPactPoints(player: Player, points: Int): LeagueProfile {
        require(points >= 0) { "Pact point additions must be non-negative." }
        return update(player) { profile ->
            profile.copy(
                pactPointsEarned =
                    (profile.pactPointsEarned + points).coerceAtMost(LeagueConstants.MAX_PACT_POINTS),
            )
        }
    }

    fun unlockPact(player: Player, pactId: String): Boolean {
        val normalizedPactId = pactId.normalizedLeagueId() ?: return false

        var unlocked = false
        update(player) { profile ->
            if (normalizedPactId in profile.unlockedPactIds || profile.pactPointsAvailable <= 0) {
                profile
            } else {
                unlocked = true
                profile.copy(
                    unlockedPactIds = profile.unlockedPactIds + normalizedPactId,
                    pactPointsSpent = profile.pactPointsSpent + 1,
                )
            }
        }
        return unlocked
    }

    fun grantPactResetForEchoBoss(player: Player, bossId: String): Boolean {
        val normalizedBossId = bossId.normalizedLeagueId() ?: return false

        var granted = false
        update(player) { profile ->
            if (normalizedBossId in profile.echoBossKillIds) {
                profile
            } else {
                val totalGranted = profile.pactResetsAvailable + profile.pactResetsUsed
                val grantReset = totalGranted < LeagueConstants.MAX_ECHO_BOSS_PACT_RESETS
                granted = grantReset
                profile.copy(
                    echoBossKillIds = profile.echoBossKillIds + normalizedBossId,
                    pactResetsAvailable =
                        if (grantReset) profile.pactResetsAvailable + 1 else profile.pactResetsAvailable,
                )
            }
        }
        return granted
    }

    fun resetPacts(player: Player): Boolean {
        var reset = false
        update(player) { profile ->
            if (profile.pactResetsAvailable <= 0) {
                profile
            } else {
                reset = true
                profile.copy(
                    unlockedPactIds = emptySet(),
                    pactPointsSpent = 0,
                    pactResetsAvailable = profile.pactResetsAvailable - 1,
                    pactResetsUsed = profile.pactResetsUsed + 1,
                )
            }
        }
        return reset
    }

    fun setNameBadge(player: Player, badge: LeagueNameBadge): LeagueProfile =
        update(player) { profile ->
            profile.copy(nameBadge = badge)
        }

    fun clear(player: Player) {
        player.clearLeagueProfile()
        applyNameBadge(player, LeagueProfile())
        LeagueClientState.sync(player, LeagueProfile())
    }

    private fun update(player: Player, transform: (LeagueProfile) -> LeagueProfile): LeagueProfile {
        val now = System.currentTimeMillis()
        val updated =
            transform(player.leagueProfile)
                .copy(updatedAtMillis = now)
                .sanitized()

        player.leagueProfile = updated
        applyNameBadge(player, updated)
        LeagueClientState.sync(player, updated)
        return updated
    }

    private fun applyNameBadge(player: Player, profile: LeagueProfile) {
        val leaguePrefixes = LeagueNameBadge.knownNamePrefixes()
        val prefix = if (profile.enabled) profile.nameBadge.namePrefix else null
        if (prefix != null || player.appearance.namePrefix in leaguePrefixes) {
            player.appearance.namePrefix = prefix
        }
    }

    private fun String.normalizedLeagueId(): String? =
        trim()
            .lowercase()
            .replace(' ', '_')
            .takeIf { it.isNotEmpty() }
}
