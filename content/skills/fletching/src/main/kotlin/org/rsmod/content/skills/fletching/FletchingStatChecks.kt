package org.rsmod.content.skills.fletching

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.fletchingLvl
import org.rsmod.api.player.stat.statAdvance

internal suspend fun ProtectedAccess.meetsFletchingLevel(
    level: Int,
    action: String = "make this",
): Boolean {
    if (player.fletchingLvl >= level) {
        return true
    }
    mesbox("You need a Fletching level of $level to $action.")
    return false
}

internal fun ProtectedAccess.advanceFletchingXp(baseXp: Double, multiplier: Double) {
    statAdvance("stat.fletching", baseXp * multiplier)
}
