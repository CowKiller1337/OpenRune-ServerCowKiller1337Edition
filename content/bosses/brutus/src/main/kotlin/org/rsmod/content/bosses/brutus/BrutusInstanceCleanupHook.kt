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
        if (!context.npc.isType(BRUTUS_NPC) && !context.npc.isVisType(BRUTUS_NPC)) {
            return
        }

        val session = manager.sessionForPlayer(context.hero) ?: return
        if (session.key != INSTANCE_KEY) {
            return
        }

        manager.beginGrace(
            session = session,
            currentTick = worldClock.cycle,
            message = "Brutus has been defeated. You have about 20 seconds to collect your loot.",
        )
    }

    private companion object {
        private const val INSTANCE_KEY = "cowboss"
        private const val BRUTUS_NPC = "npc.cowboss"
    }
}
