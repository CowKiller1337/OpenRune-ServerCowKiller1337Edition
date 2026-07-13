package org.rsmod.content.bosses.brutus

import jakarta.inject.Inject
import org.rsmod.api.instances.BossInstanceRegistry
import org.rsmod.api.instances.INSTANCE_TICKS_PER_MINUTE
import org.rsmod.api.instances.InstanceAccess
import org.rsmod.api.instances.InstanceArea
import org.rsmod.api.instances.InstanceEnterTransition
import org.rsmod.api.instances.InstanceNpc
import org.rsmod.api.instances.InstanceScript
import org.rsmod.api.instances.RegionLocal
import org.rsmod.api.instances.events.InstancePlayerJoinEvent
import org.rsmod.api.instances.withInstanceEnterTransition
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.inv.isType
import org.rsmod.plugin.scripts.ScriptContext

class BrutusInstance
@Inject
constructor(
    registry: BossInstanceRegistry,
    private val aiPlayerInteractions: AiPlayerInteractions,
) : InstanceScript(registry) {

    override fun settingsRow(): String = "dbrow.instance_cowboss"

    override fun area(): InstanceArea = PRIVATE_AREA

    override fun destroyWhenEmpty(): Boolean = true

    override fun ScriptContext.configure() {
        onEnterObject { enterBrutus() }
        onExitObject { leaveBrutus() }

        onEnterPrelude { _, enter ->
            withInstanceEnterTransition(InstanceEnterTransition(), enter)
        }

        onInstancePlayerJoin { engageBrutus() }
    }

    private suspend fun ProtectedAccess.enterBrutus() {
        if (!player.hasCowbellAmulet()) {
            mes("The gate is locked securely.")
            return
        }
        if (manager.sessionForPlayer(player) != null) {
            mes("You are already inside an instance.")
            return
        }

        val spec = buildBrutusSpec()
        val result = manager.create(
            owner = player,
            key = key,
            spec = spec,
            access = InstanceAccess.Private,
            currentTick = worldClock.cycle,
        )
        completeInstanceEntry(result)
    }

    private fun buildBrutusSpec() = buildSpec().copy(graceTicks = POST_KILL_GRACE_TICKS)

    private suspend fun ProtectedAccess.leaveBrutus() {
        defaultLeaveFlow()
    }

    private fun InstancePlayerJoinEvent.engageBrutus() {
        val brutus = manager.npcsForInstance(instanceId)
            .firstOrNull { npc -> npc.isType(BRUTUS_NPC) || npc.isVisType(BRUTUS_NPC) }
            ?: return
        brutus.opPlayer2(player, aiPlayerInteractions)
    }

    private fun org.rsmod.game.entity.Player.hasCowbellAmulet(): Boolean =
        COWBELL_AMULETS.any { amulet ->
            amulet in inv || worn.any { obj -> obj.isType(amulet) }
        }

    private companion object {
        /*
         * The supplied NPC ID 13214 is a banker in the rev 239 dump. The cache Brutus entry is
         * npc.cowboss, currently ID 15626.
         */
        private const val BRUTUS_NPC = "npc.cowboss"

        private const val POST_KILL_GRACE_TICKS = (INSTANCE_TICKS_PER_MINUTE * 20 + 59) / 60

        private val COWBELL_AMULETS = setOf("obj.cowbell_amulet", "obj.cowbell_amulet_empty")

        private val PRIVATE_AREA = InstanceArea.copyRegions(
            regionIds = listOf(12851),
            npcSpawns = listOf(
                InstanceNpc(
                    npcType = BRUTUS_NPC,
                    coord = RegionLocal(level = 0, regionZoneX = 50, regionZoneZ = 51, localX = 55, localZ = 31),
                )
            ),
        )
    }
}
