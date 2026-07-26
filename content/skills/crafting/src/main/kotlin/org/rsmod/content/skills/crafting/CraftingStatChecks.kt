package org.rsmod.content.skills.crafting

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.craftingLvl
import org.rsmod.api.player.stat.statAdvance

internal suspend fun ProtectedAccess.meetsCraftingLevel(
    level: Int,
    action: String = "make this",
): Boolean {
    if (player.craftingLvl >= level) {
        return true
    }
    mesbox("You need a Crafting level of $level to $action.")
    return false
}

internal fun ProtectedAccess.advanceCraftingXp(baseXp: Double, multiplier: Double) {
    statAdvance("stat.crafting", baseXp * multiplier)
}
