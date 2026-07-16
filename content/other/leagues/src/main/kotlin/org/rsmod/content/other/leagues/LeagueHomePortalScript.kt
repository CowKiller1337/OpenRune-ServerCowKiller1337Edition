package org.rsmod.content.other.leagues

import dev.openrune.ServerCacheManager
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class LeagueHomePortalScript : PluginScript() {
    override fun ScriptContext.startup() {
        for (portalId in LEAGUE_HOME_EXIT_PORTALS) {
            val portal = ServerCacheManager.getObject(portalId) ?: error("Missing loc: $portalId")
            onOpLoc1(portal) { leaveLeagueHome() }
        }
    }

    private fun ProtectedAccess.leaveLeagueHome() {
        telejump(PREVIOUS_HOME_SPAWN)
    }

    private companion object {
        private val LEAGUE_HOME_EXIT_PORTALS = intArrayOf(11761, 12092)
        private val PREVIOUS_HOME_SPAWN = CoordGrid(1667, 3138, 0)
    }
}
