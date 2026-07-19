package org.rsmod.content.skills.fishing

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcMode
import jakarta.inject.Inject
import org.rsmod.api.player.events.interact.NpcEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.fishingLvl
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onProtectedEvent
import org.rsmod.api.stats.levelmod.InvisibleLevels
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class Fishing
@Inject
constructor(
    private val xpMods: XpModifiers,
    private val invisibleLvls: InvisibleLevels,
    private val mapClock: MapClock,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (spot in FishingData.stationarySpotIds) {
            onEvent<NpcStateEvents.Spawn>(spot) { npc.pinFishingSpot() }
        }
        onEvent<NpcStateEvents.Respawn> {
            if (npc.id in FishingData.stationarySpotIds) {
                npc.pinFishingSpot()
            }
        }
        for (spot in FishingData.spotOptions) {
            val method = spot.method
            when (spot.op) {
                1 -> onNpcOp1(spot.npc) { fish(it.npc, spot.op, method) }
                2 -> onNpcOp2(spot.npc) { fish(it.npc, spot.op, method) }
                3 -> onNpcOp3(spot.npc) { fish(it.npc, spot.op, method) }
                4 -> onNpcOp4(spot.npc) { fish(it.npc, spot.op, method) }
                5 -> onNpcOp5(spot.npc) { fish(it.npc, spot.op, method) }
            }
        }
    }

    private fun Npc.pinFishingSpot() {
        mode = NpcMode.None
        movementLocked = true
        abortRoute()
    }

    private fun ProtectedAccess.fish(spot: Npc, op: Int, method: FishingMethod) {
        if (!canFish(method)) {
            resetAnim()
            return
        }

        faceEntitySquare(spot)
        if (actionDelay < mapClock) {
            startFishing(spot, op, method)
            return
        }

        if (skillAnimDelay <= mapClock) {
            playFishingAnim(method)
        }

        if (actionDelay == mapClock) {
            val catch = rollCatch(method)
            if (catch != null) {
                if (!removeBait(catch)) {
                    resetAnim()
                    return
                }
                val xp = catch.xp * xpMods.get(player, "stat.fishing")
                statAdvance("stat.fishing", xp)
                for (extra in catch.extraXp) {
                    statAdvance(extra.stat, extra.xp * xpMods.get(player, extra.stat))
                }
                val baseCount = random.of(catch.count)
                val extraCount = FishingRewards.run { extraCatchCount(method, catch, baseCount) }
                val finalObj = FishingRewards.finalCatchObj(player, catch)
                addCatch(finalObj, baseCount + extraCount)
                FishingRewards.run { rollTertiaryRewards(catch) }
                method.catchSound?.let { soundSynth(it) }
                spam(catch.message)
            }
            actionDelay = mapClock + method.attemptTicks
        }

        continueFishing(spot, op)
    }

    private fun ProtectedAccess.startFishing(spot: Npc, op: Int, method: FishingMethod) {
        actionDelay = mapClock + method.attemptTicks
        playFishingAnim(method)
        method.attemptSound?.let { soundSynth(it) }
        spam(method.startMessage)
        continueFishing(spot, op)
    }

    private fun ProtectedAccess.playFishingAnim(method: FishingMethod) {
        skillAnimDelay = mapClock + ANIM_INTERVAL
        anim(RSCM.getReverseMapping(RSCMType.SEQ, method.animation))
    }

    private fun ProtectedAccess.continueFishing(spot: Npc, op: Int) {
        when (op) {
            1 -> opNpc1(spot)
            2 -> opNpc2(spot)
            3 -> opNpc3(spot)
            4 -> opNpc4(spot)
            else -> resetAnim()
        }
    }

    private fun ProtectedAccess.canFish(method: FishingMethod): Boolean {
        if (player.fishingLvl < method.level) {
            mes("You need a Fishing level of ${method.level} to fish here.")
            return false
        }
        if (!canFitAnyCatch(method)) {
            mes("Your inventory is too full to hold any more fish.")
            soundSynth("synth.pillory_wrong")
            return false
        }
        if (method.tools.none { it in inv || it in player.worn }) {
            mes("You need a ${method.toolName} to catch these fish.")
            return false
        }
        for (requirement in method.wornRequirements) {
            if (requirement.items.none { it in player.worn }) {
                mes(requirement.message)
                return false
            }
        }
        val baitName = method.baitName ?: return true
        if (method.catches.none { catch -> catch.baits.isEmpty() || catch.baits.any { it in inv } }) {
            mes("You don't have any $baitName.")
            return false
        }
        return true
    }

    private fun ProtectedAccess.canFitAnyCatch(method: FishingMethod): Boolean {
        if (!inv.isFull()) {
            return true
        }
        return method.catches.any { catch ->
            player.fishingLvl >= catch.level && canFitCatch(catch)
        }
    }

    private fun ProtectedAccess.canFitCatch(catch: FishingCatch): Boolean {
        if (invAdd(inv, catch.obj, autoCommit = false).success) {
            return true
        }
        val finalObj = FishingRewards.finalCatchObj(player, catch)
        return FishBarrel.isOpen(player) && FishBarrel.canStore(finalObj) && !FishBarrel.inventory(player).isFull()
    }

    private fun ProtectedAccess.rollCatch(method: FishingMethod): FishingCatch? {
        for (catch in method.catches) {
            if (player.fishingLvl < catch.level) {
                continue
            }
            if (catch.baits.isNotEmpty() && catch.baits.none { it in inv }) {
                continue
            }
            if (statRandom("stat.fishing", catch.low, catch.high, invisibleLvls)) {
                return catch
            }
        }
        return null
    }

    private fun ProtectedAccess.removeBait(catch: FishingCatch): Boolean {
        val bait = catch.baits.firstOrNull { it in inv } ?: return catch.baits.isEmpty()
        if (bait !in inv) {
            return false
        }
        invDel(inv, bait)
        return true
    }

    private fun ProtectedAccess.addCatch(obj: String, count: Int) {
        val stored = FishBarrel.storeCatch(player, obj, count)
        val remaining = count - stored
        if (remaining > 0) {
            invAdd(inv, obj, remaining)
        }
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

    private companion object {
        private const val ANIM_INTERVAL: Int = 5
    }
}
