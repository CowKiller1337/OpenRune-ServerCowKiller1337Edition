package org.rsmod.content.generic.locs.gate

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ObjectServerType
import jakarta.inject.Inject
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpContentLoc1
import org.rsmod.content.generic.locs.gate.GateTranslations.leftGateClose
import org.rsmod.content.generic.locs.gate.GateTranslations.leftGateOpen
import org.rsmod.content.generic.locs.gate.GateTranslations.leftGateRightPair
import org.rsmod.content.generic.locs.gate.GateTranslations.rightGateClose
import org.rsmod.content.generic.locs.gate.GateTranslations.rightGateOpen
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PicketGate @Inject constructor(private val locRepo: LocRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpContentLoc1("content.closed_left_picketgate") { openLeftGate(it.loc, it.type) }
        onOpContentLoc1("content.closed_right_picketgate") { openRightGate(it.loc, it.type) }
        onOpContentLoc1("content.opened_left_picketgate") { closeLeftGate(it.loc, it.type) }
        onOpContentLoc1("content.opened_right_picketgate") { closeRightGate(it.loc, it.type) }
    }

    private suspend fun ProtectedAccess.openLeftGate(left: BoundLocInfo, type: ObjectServerType) {
        val passThrough = left.isBarbarianOutpostAgilityGate()
        if (passThrough) {
            arriveDelay()
        }

        val sound = type.param(params.opensound)
        soundSynth(sound)

        val right =
            locRepo.findExact(
                coords = left.coords + leftGateRightPair(left.shape, left.angle),
                content = "content.closed_right_picketgate",
                shape = left.shape,
            )

        left.let { locRepo.del(it, GateConstants.DURATION) }
        right?.let { locRepo.del(it, GateConstants.DURATION) }

        left.let {
            val openedLoc = type.param(params.next_loc_stage)
            val openedTranslation = leftGateOpen(it.shape, it.angle)
            val openedCoords = it.coords + openedTranslation
            val openedAngle = it.turnAngle(rotations = 3)
            locRepo.add(openedCoords, openedLoc, GateConstants.DURATION, openedAngle, it.shape)
        }
        right?.let {
            val openedLoc = locParamOrNull(it, params.next_loc_stage) ?: return
            val openedTranslation = rightGateOpen(it.shape, it.angle)
            val openedCoords = it.coords + openedTranslation
            val openedAngle = it.turnAngle(rotations = 3)
            locRepo.add(openedCoords, openedLoc, GateConstants.DURATION, openedAngle, it.shape)
        }

        if (passThrough) {
            stepThroughBarbarianOutpostGate(left)
        }
    }

    private suspend fun ProtectedAccess.openRightGate(right: BoundLocInfo, type: ObjectServerType) {
        val passThrough = right.isBarbarianOutpostAgilityGate()
        if (passThrough) {
            arriveDelay()
        }

        val sound = type.param(params.opensound)
        soundSynth(sound)

        val left =
            locRepo.findExact(
                coords = right.coords - leftGateRightPair(right.shape, right.angle),
                content = "content.closed_left_picketgate",
                shape = right.shape,
            )

        left?.let { locRepo.del(it, GateConstants.DURATION) }
        right.let { locRepo.del(it, GateConstants.DURATION) }

        left?.let {
            val openedLoc = locParamOrNull(it, params.next_loc_stage) ?: return@let
            val openedTranslation = leftGateOpen(it.shape, it.angle)
            val openedCoords = it.coords + openedTranslation
            val openedAngle = it.turnAngle(rotations = 3)
            locRepo.add(openedCoords, openedLoc, GateConstants.DURATION, openedAngle, it.shape)
        }
        right.let {
            val openedLoc = type.param(params.next_loc_stage)
            val openedTranslation = rightGateOpen(it.shape, it.angle)
            val openedCoords = it.coords + openedTranslation
            val openedAngle = it.turnAngle(rotations = 3)
            locRepo.add(openedCoords, openedLoc, GateConstants.DURATION, openedAngle, it.shape)
        }

        if (passThrough) {
            stepThroughBarbarianOutpostGate(right)
        }
    }

    private fun ProtectedAccess.closeLeftGate(left: BoundLocInfo, type: ObjectServerType) {
        val sound = type.param(params.closesound)
        soundSynth(sound)

        val right =
            locRepo.findExact(
                coords = left.coords + leftGateRightPair(left.shape, left.angle),
                content = "content.opened_right_picketgate",
                shape = left.shape,
            )

        left.let { locRepo.del(it, GateConstants.DURATION) }
        right?.let { locRepo.del(it, GateConstants.DURATION) }

        left.let {
            val closedLoc = type.param(params.next_loc_stage)
            val closedTranslation = leftGateClose(it.shape, it.angle)
            val closedCoords = it.coords + closedTranslation
            val closedAngle = it.turnAngle(rotations = -3)
            locRepo.add(closedCoords, closedLoc, GateConstants.DURATION, closedAngle, it.shape)
        }
        right?.let {
            val closedLoc = locParamOrNull(it, params.next_loc_stage) ?: return
            val closedTranslation = rightGateClose(it.shape, it.angle)
            val closedCoords = it.coords + closedTranslation
            val closedAngle = it.turnAngle(rotations = -3)
            locRepo.add(closedCoords, closedLoc, GateConstants.DURATION, closedAngle, it.shape)
        }
    }

    private fun ProtectedAccess.closeRightGate(right: BoundLocInfo, type: ObjectServerType) {
        val sound = type.param(params.closesound)
        soundSynth(sound)

        val left =
            locRepo.findExact(
                coords = right.coords - leftGateRightPair(right.shape, right.angle),
                content = "content.opened_left_picketgate",
                shape = right.shape,
            )

        left?.let { locRepo.del(it, GateConstants.DURATION) }
        right.let { locRepo.del(it, GateConstants.DURATION) }

        left?.let {
            val openedLoc = locParamOrNull(it, params.next_loc_stage) ?: return@let
            val openedTranslation = leftGateClose(it.shape, it.angle)
            val openedCoords = it.coords + openedTranslation
            val openedAngle = it.turnAngle(rotations = -3)
            locRepo.add(openedCoords, openedLoc, GateConstants.DURATION, openedAngle, it.shape)
        }
        right.let {
            val openedLoc = type.param(params.next_loc_stage)
            val openedTranslation = rightGateClose(it.shape, it.angle)
            val openedCoords = it.coords + openedTranslation
            val openedAngle = it.turnAngle(rotations = -3)
            locRepo.add(openedCoords, openedLoc, GateConstants.DURATION, openedAngle, it.shape)
        }
    }

    private suspend fun ProtectedAccess.stepThroughBarbarianOutpostGate(gate: BoundLocInfo) {
        delay(1)
        playerWalkWithMinDelay(coords.stepThroughGate(gate))
    }

    private fun BoundLocInfo.isBarbarianOutpostAgilityGate(): Boolean =
        level == 0 &&
            x in BARBARIAN_OUTPOST_GATE_MIN_X..BARBARIAN_OUTPOST_GATE_MAX_X &&
            z in BARBARIAN_OUTPOST_GATE_MIN_Z..BARBARIAN_OUTPOST_GATE_MAX_Z

    private fun CoordGrid.stepThroughGate(gate: BoundLocInfo): CoordGrid {
        val xDistance = kotlin.math.abs(x - gate.x)
        val zDistance = kotlin.math.abs(z - gate.z)
        return if (xDistance >= zDistance) {
            if (x <= gate.x) translateX(1) else translateX(-1)
        } else {
            if (z <= gate.z) translateZ(1) else translateZ(-1)
        }
    }

    private companion object {
        private const val BARBARIAN_OUTPOST_GATE_MIN_X = 2538
        private const val BARBARIAN_OUTPOST_GATE_MAX_X = 2558
        private const val BARBARIAN_OUTPOST_GATE_MIN_Z = 3556
        private const val BARBARIAN_OUTPOST_GATE_MAX_Z = 3575
    }
}
