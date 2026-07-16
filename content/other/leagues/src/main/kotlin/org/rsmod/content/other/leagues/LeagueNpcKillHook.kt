package org.rsmod.content.other.leagues

import jakarta.inject.Inject
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.game.entity.Npc

class LeagueNpcKillHook
@Inject
constructor(
    private val tasks: LeagueTaskService,
) : NpcDeathKillHook {

    override fun onKill(context: NpcDeathKillContext) {
        if (!context.npc.isChicken()) {
            return
        }
        tasks.completeByName(context.hero, DEFEAT_CHICKEN_TASK)
    }

    private fun Npc.isChicken(): Boolean =
        CHICKEN_NPCS.any { type -> isType(type) || isVisType(type) }

    private companion object {
        private const val DEFEAT_CHICKEN_TASK = "Defeat a Chicken"

        private val CHICKEN_NPCS =
            setOf(
                "npc.chicken",
                "npc.chicken_brown",
                "npc.farm_chicken_brown",
                "npc.farm_chicken_brown_indoors",
                "npc.farm_chicken_brown_outdoors",
                "npc.farm_chicken_dark_brown",
                "npc.farm_chicken_dark_brown_outdoors",
                "npc.farm_chicken_tan",
                "npc.farm_chicken_tan_outdoors",
                "npc.misc_chicken",
                "npc.misc_chicken_brown",
                "npc.tut2_chicken",
            )
    }
}
