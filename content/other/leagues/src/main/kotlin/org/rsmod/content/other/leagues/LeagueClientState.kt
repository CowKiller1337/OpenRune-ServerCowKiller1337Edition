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
        val leaguePoints = if (profile.enabled) profile.leaguePoints else 0
        setVarp(player, LeagueVarps.LEAGUE_POINTS_COMPLETED, leaguePoints)
        setVarp(player, LeagueVarps.LEAGUE_POINTS_CLAIMED, leaguePoints)
        setVarp(player, LeagueVarps.LEAGUE_6_POINTS, leaguePoints)
        setTaskCompletionVars(player, profile)
        setStarterRelicDisplay(player, profile)
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
        setVarp(player, LeagueVarps.TALENT_POINTS_EARNED, profile.pactPointsEarned)
        setVarp(player, LeagueVarps.TALENT_POINTS_SPENT, profile.pactPointsSpent)
        setVarbit(player, LeagueVarbits.TALENT_RESETS_AVAILABLE, profile.pactResetsAvailable)
        setPactUnlockVars(player, profile)
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
        val width = varbit.endBit - varbit.startBit + 1
        val max = if (width >= Int.SIZE_BITS) Int.MAX_VALUE else ((1L shl width) - 1).toInt()
        VarPlayerIntMapSetter.set(player, varbit, value.coerceIn(0, max))
    }

    private fun setTaskCompletionVars(player: Player, profile: LeagueProfile) {
        val packed = IntArray(LeagueVarps.LEAGUE_TASK_COMPLETED.size)
        if (profile.enabled) {
            for (taskId in profile.completedTaskIds) {
                val taskIndex = taskId.toLeagueTaskIndex() ?: continue
                val bucket = taskIndex / Int.SIZE_BITS
                val bit = taskIndex % Int.SIZE_BITS
                if (bucket in packed.indices) {
                    packed[bucket] = packed[bucket] or (1 shl bit)
                }
            }
        }
        LeagueVarps.LEAGUE_TASK_COMPLETED.forEachIndexed { index, varp ->
            setVarp(player, varp, packed[index])
        }
        val completedCount = if (profile.enabled) profile.completedTaskIds.size else 0
        setVarbit(player, LeagueVarbits.LEAGUE_TOTAL_TASKS_COMPLETED, completedCount)
    }

    private fun setStarterRelicDisplay(player: Player, profile: LeagueProfile) {
        val selections = IntArray(LeagueVarbits.LEAGUE_RELIC_SELECTION.size)
        if (profile.enabled) {
            for ((tier, relicId) in profile.selectedRelics) {
                val slot = tier - 1
                val choice = relicId.toRelicChoice() ?: continue
                if (slot in selections.indices) {
                    selections[slot] = choice
                }
            }
            if (selections[0] == 0) {
                selections[0] = LeagueClientFlags.STARTER_RELIC_PROGRESS_PLACEHOLDER
            }
        }
        LeagueVarbits.LEAGUE_RELIC_SELECTION.forEachIndexed { index, varbit ->
            setVarbit(player, varbit, selections[index])
        }
    }

    private fun setPactUnlockVars(player: Player, profile: LeagueProfile) {
        val packed = IntArray(PACT_UNLOCK_VARPS.size)
        for (pactId in profile.unlockedPactIds) {
            val node = pactId.toPactNodeIndex() ?: continue
            val bucket = node / Int.SIZE_BITS
            val bit = node % Int.SIZE_BITS
            if (bucket in packed.indices) {
                packed[bucket] = packed[bucket] or (1 shl bit)
            }
        }
        for (index in PACT_UNLOCK_VARPS.indices) {
            setVarp(player, PACT_UNLOCK_VARPS[index], packed[index])
        }
    }

    private fun String.toPactNodeIndex(): Int? =
        removePrefix("pact_")
            .removePrefix("node_")
            .toIntOrNull()
            ?.takeIf { it >= 0 }

    private fun String.toLeagueTaskIndex(): Int? =
        removePrefix("league_task_")
            .removePrefix("task_")
            .toIntOrNull()
            ?.takeIf { it >= 0 }

    private fun String.toRelicChoice(): Int? =
        removePrefix("relic_")
            .removePrefix("choice_")
            .toIntOrNull()
            ?.takeIf { it > 0 }

    private val PACT_UNLOCK_VARPS =
        intArrayOf(
            LeagueVarps.COMBAT_MASTERY_PERM_0,
            LeagueVarps.COMBAT_MASTERY_PERM_1,
            LeagueVarps.COMBAT_MASTERY_PERM_2,
            LeagueVarps.COMBAT_MASTERY_PERM_3,
            LeagueVarps.COMBAT_MASTERY_PERM_4,
            LeagueVarps.COMBAT_MASTERY_PERM_5,
        )
}
