package org.rsmod.content.other.leagues

enum class LeagueNameBadge(
    val storageId: String,
    val displayName: String,
    val namePrefix: String?,
    val publicChatIcon: Int?,
) {
    None(
        storageId = "none",
        displayName = "None",
        namePrefix = null,
        publicChatIcon = null,
    ),
    League6(
        storageId = "league_6",
        displayName = "League VI",
        namePrefix = "<img=${LeagueIconIds.LEAGUE_6_NAME_BADGE}> ",
        publicChatIcon = LeagueIconIds.LEAGUE_6_NAME_BADGE,
    ),
    ;

    companion object {
        fun fromStorageId(storageId: String?): LeagueNameBadge =
            values().firstOrNull { it.storageId == storageId } ?: None

        fun knownNamePrefixes(): Set<String> =
            values().mapNotNullTo(mutableSetOf()) { it.namePrefix }
    }
}
