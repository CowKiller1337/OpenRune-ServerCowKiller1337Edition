package org.rsmod.content.other.leagues

import org.rsmod.api.attr.AttributeKey
import org.rsmod.game.entity.Player

private val LEAGUE_PROFILE_ATTR =
    AttributeKey<Map<String, Any>>(persistenceKey = "league_6_profile")

var Player.leagueProfile: LeagueProfile
    get() = attr[LEAGUE_PROFILE_ATTR].decodeLeagueProfile()
    set(value) {
        attr[LEAGUE_PROFILE_ATTR] = value.sanitized().toPersistentMap()
    }

val Player.hasLeagueProfile: Boolean
    get() = attr.has(LEAGUE_PROFILE_ATTR)

fun Player.clearLeagueProfile() {
    attr.remove(LEAGUE_PROFILE_ATTR)
}

data class LeagueProfile(
    val schemaVersion: Int = LeagueConstants.PROFILE_SCHEMA_VERSION,
    val leagueId: String = LeagueConstants.CURRENT_LEAGUE_ID,
    val enabled: Boolean = false,
    val leaguePoints: Int = 0,
    val completedTaskIds: Set<String> = emptySet(),
    val selectedRelics: Map<Int, String> = emptyMap(),
    val unlockedRegionIds: Set<String> = emptySet(),
    val pactPointsEarned: Int = 0,
    val pactPointsSpent: Int = 0,
    val unlockedPactIds: Set<String> = emptySet(),
    val pactResetsAvailable: Int = 0,
    val pactResetsUsed: Int = 0,
    val echoBossKillIds: Set<String> = emptySet(),
    val nameBadge: LeagueNameBadge = LeagueNameBadge.None,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
) {
    val pactPointsAvailable: Int
        get() = (pactPointsEarned - pactPointsSpent).coerceAtLeast(0)

    fun sanitized(): LeagueProfile {
        val earned = pactPointsEarned.coerceIn(0, LeagueConstants.MAX_PACT_POINTS)
        val spent = pactPointsSpent.coerceIn(0, earned)
        val resetsUsed = pactResetsUsed.coerceIn(0, LeagueConstants.MAX_ECHO_BOSS_PACT_RESETS)
        val resetsAvailable =
            pactResetsAvailable.coerceIn(
                0,
                LeagueConstants.MAX_ECHO_BOSS_PACT_RESETS - resetsUsed,
            )

        return copy(
            schemaVersion = LeagueConstants.PROFILE_SCHEMA_VERSION,
            leagueId = leagueId.ifBlank { LeagueConstants.CURRENT_LEAGUE_ID },
            leaguePoints = leaguePoints.coerceAtLeast(0),
            completedTaskIds = completedTaskIds.normalizedIds(),
            selectedRelics =
                selectedRelics
                    .filterKeys { it > 0 }
                    .filterValues { it.isNotBlank() }
                    .toSortedMap(),
            unlockedRegionIds = unlockedRegionIds.normalizedIds(),
            pactPointsEarned = earned,
            pactPointsSpent = spent,
            unlockedPactIds = unlockedPactIds.normalizedIds(),
            pactResetsAvailable = resetsAvailable,
            pactResetsUsed = resetsUsed,
            echoBossKillIds = echoBossKillIds.normalizedIds(),
            createdAtMillis = createdAtMillis.coerceAtLeast(0L),
            updatedAtMillis = updatedAtMillis.coerceAtLeast(0L),
        )
    }
}

private object LeagueProfileKeys {
    const val SCHEMA = "schema"
    const val LEAGUE_ID = "league_id"
    const val ENABLED = "enabled"
    const val LEAGUE_POINTS = "league_points"
    const val COMPLETED_TASKS = "completed_tasks"
    const val SELECTED_RELICS = "selected_relics"
    const val UNLOCKED_REGIONS = "unlocked_regions"
    const val PACT_POINTS_EARNED = "pact_points_earned"
    const val PACT_POINTS_SPENT = "pact_points_spent"
    const val UNLOCKED_PACTS = "unlocked_pacts"
    const val PACT_RESETS_AVAILABLE = "pact_resets_available"
    const val PACT_RESETS_USED = "pact_resets_used"
    const val ECHO_BOSSES_KILLED = "echo_bosses_killed"
    const val NAME_BADGE = "name_badge"
    const val CREATED_AT_MILLIS = "created_at_millis"
    const val UPDATED_AT_MILLIS = "updated_at_millis"
}

private fun Map<String, Any>.toLeagueProfile(): LeagueProfile =
    LeagueProfile(
        schemaVersion = int(LeagueProfileKeys.SCHEMA, LeagueConstants.PROFILE_SCHEMA_VERSION),
        leagueId = string(LeagueProfileKeys.LEAGUE_ID, LeagueConstants.CURRENT_LEAGUE_ID),
        enabled = bool(LeagueProfileKeys.ENABLED, false),
        leaguePoints = int(LeagueProfileKeys.LEAGUE_POINTS, 0),
        completedTaskIds = stringSet(LeagueProfileKeys.COMPLETED_TASKS),
        selectedRelics = intStringMap(LeagueProfileKeys.SELECTED_RELICS),
        unlockedRegionIds = stringSet(LeagueProfileKeys.UNLOCKED_REGIONS),
        pactPointsEarned = int(LeagueProfileKeys.PACT_POINTS_EARNED, 0),
        pactPointsSpent = int(LeagueProfileKeys.PACT_POINTS_SPENT, 0),
        unlockedPactIds = stringSet(LeagueProfileKeys.UNLOCKED_PACTS),
        pactResetsAvailable = int(LeagueProfileKeys.PACT_RESETS_AVAILABLE, 0),
        pactResetsUsed = int(LeagueProfileKeys.PACT_RESETS_USED, 0),
        echoBossKillIds = stringSet(LeagueProfileKeys.ECHO_BOSSES_KILLED),
        nameBadge = LeagueNameBadge.fromStorageId(stringOrNull(LeagueProfileKeys.NAME_BADGE)),
        createdAtMillis = long(LeagueProfileKeys.CREATED_AT_MILLIS, 0L),
        updatedAtMillis = long(LeagueProfileKeys.UPDATED_AT_MILLIS, 0L),
    )
        .sanitized()

private fun Map<String, Any>?.decodeLeagueProfile(): LeagueProfile =
    this?.toLeagueProfile() ?: LeagueProfile()

private fun LeagueProfile.toPersistentMap(): Map<String, Any> {
    val profile = sanitized()
    return linkedMapOf(
        LeagueProfileKeys.SCHEMA to profile.schemaVersion,
        LeagueProfileKeys.LEAGUE_ID to profile.leagueId,
        LeagueProfileKeys.ENABLED to profile.enabled,
        LeagueProfileKeys.LEAGUE_POINTS to profile.leaguePoints,
        LeagueProfileKeys.COMPLETED_TASKS to profile.completedTaskIds.sorted(),
        LeagueProfileKeys.SELECTED_RELICS to
            profile.selectedRelics.entries.associate { (tier, relic) -> tier.toString() to relic },
        LeagueProfileKeys.UNLOCKED_REGIONS to profile.unlockedRegionIds.sorted(),
        LeagueProfileKeys.PACT_POINTS_EARNED to profile.pactPointsEarned,
        LeagueProfileKeys.PACT_POINTS_SPENT to profile.pactPointsSpent,
        LeagueProfileKeys.UNLOCKED_PACTS to profile.unlockedPactIds.sorted(),
        LeagueProfileKeys.PACT_RESETS_AVAILABLE to profile.pactResetsAvailable,
        LeagueProfileKeys.PACT_RESETS_USED to profile.pactResetsUsed,
        LeagueProfileKeys.ECHO_BOSSES_KILLED to profile.echoBossKillIds.sorted(),
        LeagueProfileKeys.NAME_BADGE to profile.nameBadge.storageId,
        LeagueProfileKeys.CREATED_AT_MILLIS to profile.createdAtMillis,
        LeagueProfileKeys.UPDATED_AT_MILLIS to profile.updatedAtMillis,
    )
}

private fun Set<String>.normalizedIds(): Set<String> =
    mapNotNullTo(linkedSetOf()) { value ->
        value.trim().lowercase().takeIf { it.isNotEmpty() }
    }

private fun Map<String, Any>.int(key: String, default: Int): Int =
    when (val value = this[key]) {
        is Number -> value.toInt()
        is String -> value.toIntOrNull() ?: default
        else -> default
    }

private fun Map<String, Any>.long(key: String, default: Long): Long =
    when (val value = this[key]) {
        is Number -> value.toLong()
        is String -> value.toLongOrNull() ?: default
        else -> default
    }

private fun Map<String, Any>.bool(key: String, default: Boolean): Boolean =
    when (val value = this[key]) {
        is Boolean -> value
        is Number -> value.toInt() != 0
        is String -> value.equals("true", ignoreCase = true) || value == "1"
        else -> default
    }

private fun Map<String, Any>.string(key: String, default: String): String =
    stringOrNull(key)?.takeIf { it.isNotBlank() } ?: default

private fun Map<String, Any>.stringOrNull(key: String): String? = this[key] as? String

private fun Map<String, Any>.stringSet(key: String): Set<String> =
    when (val value = this[key]) {
        is Iterable<*> -> value.mapNotNullTo(linkedSetOf()) { it?.toString() }.normalizedIds()
        is String -> setOf(value).normalizedIds()
        else -> emptySet()
    }

private fun Map<String, Any>.intStringMap(key: String): Map<Int, String> {
    val value = this[key] as? Map<*, *> ?: return emptyMap()
    return value.entries
        .mapNotNull { (rawKey, rawValue) ->
            val tier = rawKey?.toString()?.toIntOrNull() ?: return@mapNotNull null
            val id = rawValue?.toString()?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
                ?: return@mapNotNull null
            tier to id
        }
        .toMap()
}
