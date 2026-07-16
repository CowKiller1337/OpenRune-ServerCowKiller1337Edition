package org.rsmod.content.other.leagues

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
        spawnPermanentNpc(YAMA_LAIR_TEST_CHICKEN, YAMA_LAIR_TEST_CHICKEN_COORD)
    }

    private fun spawnPermanentNpc(type: String, coords: CoordGrid) {
        val alreadySpawned = npcRepo.findAll(coords).any { npc -> npc.isType(type) }
        if (!alreadySpawned) {
            npcRepo.add(Npc(type, coords), duration = Int.MAX_VALUE)
        }
    }

    private companion object {
        private const val YAMA_LAIR_TEST_CHICKEN = "npc.chicken"
        private val YAMA_LAIR_TEST_CHICKEN_COORD = CoordGrid(1507, 5602, 0)
    }
}
