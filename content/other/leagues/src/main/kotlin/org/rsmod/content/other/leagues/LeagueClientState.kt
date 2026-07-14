package org.rsmod.content.other.leagues

import dev.openrune.ServerCacheManager
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player

object LeagueClientState {
    fun sync(player: Player, profile: LeagueProfile = player.leagueProfile) {
        setLeagueWorld(player, profile.enabled)
        setVarbit(player, LeagueVarbits.LEAGUE_ACCOUNT, if (profile.enabled) 1 else 0)
        setVarbit(
            player,
            LeagueVarbits.LEAGUE_TYPE,
            if (profile.enabled) LeagueConstants.CURRENT_LEAGUE_TYPE else 0,
        )
        setVarbit(
            player,
            LeagueVarbits.LEAGUE_TUTORIAL_COMPLETED,
            if (profile.enabled) LeagueClientFlags.LEAGUE_TUTORIAL_COMPLETE_STAGE else 0,
        )
        setVarp(player, LeagueVarps.LEAGUE_6_POINTS, profile.leaguePoints)
        setVarbit(
            player,
            LeagueVarbits.LEAGUE_COMBAT_MASTERY_POINTS_TO_SPEND,
            profile.pactPointsAvailable,
        )
        setVarbit(
            player,
            LeagueVarbits.LEAGUE_COMBAT_MASTERY_POINTS_EARNED,
            profile.pactPointsEarned,
        )
    }

    private fun setVarp(player: Player, id: Int, value: Int) {
        val varp = ServerCacheManager.getVarp(id) ?: return
        VarPlayerIntMapSetter.set(player, varp, value)
    }

    private fun setLeagueWorld(player: Player, enabled: Boolean) {
        val varp = ServerCacheManager.getVarp(LeagueVarps.MAP_FLAGS_CACHED) ?: return
        val current = player.vars[varp]
        val next =
            if (enabled) {
                (current or LeagueClientFlags.LEAGUE_WORLD_MAP_FLAG) and
                    LeagueClientFlags.DEADMAN_WORLD_MAP_FLAG.inv()
            } else {
                current and LeagueClientFlags.LEAGUE_WORLD_MAP_FLAG.inv()
            }
        VarPlayerIntMapSetter.set(player, varp, next)
    }

    private fun setVarbit(player: Player, id: Int, value: Int) {
        val varbit = ServerCacheManager.getVarbit(id) ?: return
        VarPlayerIntMapSetter.set(player, varbit, value)
    }
}
