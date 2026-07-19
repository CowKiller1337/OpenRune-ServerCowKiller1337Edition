package org.rsmod.content.bosses.brutus

import jakarta.inject.Inject
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.instances.InstanceManager
import org.rsmod.game.MapClock

class BrutusInstanceCleanupHook
@Inject
constructor(
    private val manager: InstanceManager,
    private val worldClock: MapClock,
) : NpcDeathKillHook {

    override fun onKill(context: NpcDeathKillContext) {
        if (BRUTUS_NPCS.none { context.npc.isType(it) || context.npc.isVisType(it) }) {
            return
        }

        val session = manager.sessionForPlayer(context.hero) ?: return
        if (session.key != INSTANCE_KEY) {
            return
        }

        manager.beginGrace(
            session = session,
            currentTick = worldClock.cycle,
            message = null,
        )
    }

    private companion object {
        private const val INSTANCE_KEY = "cowboss"
        private val BRUTUS_NPCS = setOf("npc.cowboss", "npc.cowboss_hardmode")
    }
}
