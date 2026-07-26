package org.rsmod.content.skills.construction

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.constructionLvl
import org.rsmod.api.player.stat.statAdvance

internal suspend fun ProtectedAccess.meetsConstructionLevel(
    level: Int,
    action: String = "build this",
): Boolean {
    if (player.constructionLvl >= level) {
        return true
    }
    mesbox("You need a Construction level of $level to $action.")
    return false
}

internal fun ProtectedAccess.advanceConstructionXp(baseXp: Double, multiplier: Double) {
    statAdvance("stat.construction", baseXp * multiplier)
}
