package org.rsmod.content.skills.woodcutting

import jakarta.inject.Inject
import org.rsmod.api.repo.player.PlayerRepository
import org.rsmod.api.stats.levelmod.InvisibleLevelMod
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.map.zone.ZoneKey

class WoodcuttingLevelBoosts
@Inject
constructor(private val playerRepo: PlayerRepository, private val mapClock: MapClock) :
    InvisibleLevelMod("stat.woodcutting") {
    override fun Player.calculateBoost(): Int {
        val guildBoost = if (isInWoodcuttingGuild()) WOODCUTTING_GUILD_BOOST else 0
        val groupBoost = calculateGroupBoost()
        return guildBoost + groupBoost
    }

    private fun Player.calculateGroupBoost(): Int {
        if (attr[CURRENT_WOODCUTTING_GROUP_BOOST_ATTR] != true) {
            return 0
        }
        val tree = attr[CURRENT_WOODCUTTING_TREE_ATTR] ?: return 0
        if (actionDelay < mapClock.cycle) {
            return 0
        }
        val choppers =
            playerRepo.findAll(ZoneKey.from(tree), zoneRadius = 1).count { other ->
                other.attr[CURRENT_WOODCUTTING_GROUP_BOOST_ATTR] == true &&
                    other.attr[CURRENT_WOODCUTTING_TREE_ATTR] == tree &&
                    other.actionDelay >= mapClock.cycle
            }
        return (choppers - 1).coerceIn(0, FORESTRY_GROUP_BOOST_CAP)
    }

    private fun Player.isInWoodcuttingGuild(): Boolean {
        return coords.x in WOODCUTTING_GUILD_X_RANGE && coords.z in WOODCUTTING_GUILD_Z_RANGE
    }

    private companion object {
        private const val WOODCUTTING_GUILD_BOOST: Int = 7
        private const val FORESTRY_GROUP_BOOST_CAP: Int = 10
        private val WOODCUTTING_GUILD_X_RANGE: IntRange = 1560..1664
        private val WOODCUTTING_GUILD_Z_RANGE: IntRange = 3470..3528
    }
}
