package org.rsmod.content.skills.fishing

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.events.interact.NpcEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.fishingLvl
import org.rsmod.api.script.onProtectedEvent
import org.rsmod.api.stats.levelmod.InvisibleLevels
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class Fishing
@Inject
constructor(
    private val xpMods: XpModifiers,
    private val invisibleLvls: InvisibleLevels,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (spot in FishingData.spotOptions) {
            val method = spot.method
            when (spot.op) {
                1 -> onNpcOp1(spot.npc) { fish(it.npc, method) }
                2 -> onNpcOp2(spot.npc) { fish(it.npc, method) }
                3 -> onNpcOp3(spot.npc) { fish(it.npc, method) }
                4 -> onNpcOp4(spot.npc) { fish(it.npc, method) }
                5 -> onNpcOp5(spot.npc) { fish(it.npc, method) }
            }
        }
    }

    private suspend fun ProtectedAccess.fish(spot: Npc, method: FishingMethod) {
        arriveDelay()
        val originalCoords = spot.coords
        if (!canFish(method)) {
            return
        }

        spam(method.startMessage)
        try {
            while (true) {
                if (spot.coords != originalCoords || !canFish(method)) {
                    return
                }
                faceEntitySquare(spot)
                anim(RSCM.getReverseMapping(RSCMType.SEQ, method.animation))
                delay(5)

                if (spot.coords != originalCoords || !canFish(method)) {
                    return
                }
                val catch = rollCatch(method) ?: continue
                if (!removeBait(catch)) {
                    return
                }

                val xp = catch.xp * xpMods.get(player, "stat.fishing")
                statAdvance("stat.fishing", xp)
                invAdd(inv, catch.obj)
                spam(catch.message)
            }
        } finally {
            resetAnim()
        }
    }

    private fun ProtectedAccess.canFish(method: FishingMethod): Boolean {
        if (player.fishingLvl < method.level) {
            mes("You need a Fishing level of ${method.level} to fish here.")
            return false
        }
        if (inv.isFull()) {
            mes("Your inventory is too full to hold any more fish.")
            soundSynth("synth.pillory_wrong")
            return false
        }
        if (method.tools.none { it in inv || it in player.worn }) {
            mes("You need a ${method.toolName} to catch these fish.")
            return false
        }
        val baitName = method.baitName ?: return true
        if (method.catches.none { catch -> catch.bait == null || catch.bait in inv }) {
            mes("You don't have any $baitName.")
            return false
        }
        return true
    }

    private fun ProtectedAccess.rollCatch(method: FishingMethod): FishingCatch? {
        for (catch in method.catches) {
            if (player.fishingLvl < catch.level) {
                continue
            }
            val bait = catch.bait
            if (bait != null && bait !in inv) {
                continue
            }
            if (statRandom("stat.fishing", catch.low, catch.high, invisibleLvls)) {
                return catch
            }
        }
        return null
    }

    private fun ProtectedAccess.removeBait(catch: FishingCatch): Boolean {
        val bait = catch.bait ?: return true
        if (bait !in inv) {
            return false
        }
        invDel(inv, bait)
        return true
    }

    private fun ScriptContext.onNpcOp1(
        npc: Int,
        action: suspend ProtectedAccess.(NpcEvents.Op1) -> Unit,
    ): Unit = onProtectedEvent<NpcEvents.Op1>(npc, action)

    private fun ScriptContext.onNpcOp2(
        npc: Int,
        action: suspend ProtectedAccess.(NpcEvents.Op2) -> Unit,
    ): Unit = onProtectedEvent<NpcEvents.Op2>(npc, action)

    private fun ScriptContext.onNpcOp3(
        npc: Int,
        action: suspend ProtectedAccess.(NpcEvents.Op3) -> Unit,
    ): Unit = onProtectedEvent<NpcEvents.Op3>(npc, action)

    private fun ScriptContext.onNpcOp4(
        npc: Int,
        action: suspend ProtectedAccess.(NpcEvents.Op4) -> Unit,
    ): Unit = onProtectedEvent<NpcEvents.Op4>(npc, action)

    private fun ScriptContext.onNpcOp5(
        npc: Int,
        action: suspend ProtectedAccess.(NpcEvents.Op5) -> Unit,
    ): Unit = onProtectedEvent<NpcEvents.Op5>(npc, action)
}
