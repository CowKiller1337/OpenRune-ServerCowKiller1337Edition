package org.rsmod.content.other.leagues

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcServerType
import jakarta.inject.Inject
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.game.entity.Npc
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class LeagueHomeNpcSpawns
@Inject
constructor(
    private val npcRepo: NpcRepository,
) : PluginScript() {

    override fun ScriptContext.startup() {
        val testChicken = resolveTestChickenType() ?: return
        spawnPermanentNpc(testChicken, YAMA_LAIR_TEST_CHICKEN_COORD)
    }

    private fun spawnPermanentNpc(type: NpcServerType, coords: CoordGrid) {
        val alreadySpawned = npcRepo.findAll(coords).any { npc -> npc.type.id == type.id }
        if (!alreadySpawned) {
            npcRepo.add(Npc(type, coords), duration = Int.MAX_VALUE)
        }
    }

    private fun resolveTestChickenType(): NpcServerType? {
        return ServerCacheManager.getNpc(YAMA_LAIR_TEST_CHICKEN_ID)
            ?: ServerCacheManager.getNpc(CHICKEN.asRSCM(RSCMType.NPC))
    }

    private companion object {
        private const val CHICKEN = "npc.chicken"
        private const val YAMA_LAIR_TEST_CHICKEN_ID = 16383
        private val YAMA_LAIR_TEST_CHICKEN_COORD = CoordGrid(1507, 5602, 0)
    }
}
