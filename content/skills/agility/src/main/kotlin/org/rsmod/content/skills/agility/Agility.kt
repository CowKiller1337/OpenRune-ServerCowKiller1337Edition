package org.rsmod.content.skills.agility

import dev.openrune.types.BasType
import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.agilityLvl
import org.rsmod.api.player.vars.setActiveMoveSpeed
import org.rsmod.api.player.vars.varMoveSpeed
import org.rsmod.api.script.onProtectedEvent
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.movement.MoveSpeed
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

class Agility
@Inject
constructor(private val xpMods: XpModifiers, private val collision: CollisionFlagMap) :
    PluginScript() {
    override fun ScriptContext.startup() {
        for (loc in AgilityData.locIds) {
            onProtectedEvent<LocEvents.Op1>(loc) {
                val obstacle = AgilityData.findObstacle(it.loc.id, it.loc.coords) ?: return@onProtectedEvent
                cross(obstacle)
            }
        }
        onProtectedEvent<LocEvents.Op1>(BARBARIAN_OUTPOST_PIPE) { squeezeBarbarianPipe() }
        onProtectedEvent<LocEvents.Op1>(BARBARIAN_OUTPOST_LADDER_TOP) {
            if (it.loc.coords != BARBARIAN_OUTPOST_LADDER_TOP_COORD) {
                return@onProtectedEvent
            }
            climbDownBarbarianOutpostLadder()
        }
        for (loc in BARBARIAN_OUTPOST_FRONT_GATE_LOCS) {
            onProtectedEvent<LocEvents.Op1>(loc) {
                if (!it.loc.coords.isBarbarianOutpostFrontGateArea()) {
                    return@onProtectedEvent
                }
                passBarbarianOutpostFrontGate(it.loc)
            }
        }
    }

    private suspend fun ProtectedAccess.cross(obstacle: AgilityObstacle) {
        val previousMoveSpeed = player.varMoveSpeed
        player.setActiveMoveSpeed(MoveSpeed.Walk)

        try {
            if (obstacle.waitForArrival) {
                arriveDelay()
            }

            if (player.agilityLvl < obstacle.level) {
                mes("You need an Agility level of ${obstacle.level} to use this obstacle.")
                return
            }

            if (obstacle.waitForArrival) {
                walkToObstacleStart(obstacle)
            }
            val moveStart = coords
            faceSquare(obstacle.end)
            val protectExactMoveAnim =
                obstacle.anim != null && obstacle.movement == AgilityMovement.ExactMove
            try {
                obstacle.anim?.let {
                    anim(it)
                    if (protectExactMoveAnim) {
                        animProtect(true)
                    }
                }

                when (obstacle.movement) {
                    AgilityMovement.ExactMove -> {
                        exactMove(
                            start = moveStart,
                            end = obstacle.end,
                            delay1 = 0,
                            delay2 = obstacle.exactMoveDelay,
                            dir = obstacle.faceDir,
                        )
                        delayObstacleMove(obstacle)
                    }
                    AgilityMovement.StraightWalk -> {
                        obstacleWalk(obstacle, BALANCE_BAS, moveStart)
                    }
                    AgilityMovement.BalanceWalk -> {
                        obstacleWalk(obstacle, WALL_BAS, moveStart)
                    }
                    AgilityMovement.TelejumpAfterDelay -> {
                        delay(obstacle.delay)
                        telejump(obstacle.end)
                    }
                }
            } finally {
                if (protectExactMoveAnim) {
                    animProtect(false)
                }
                resetAnim()
            }

            awardObstacleXp(obstacle)
            updateCourseProgress(obstacle)
        } finally {
            player.setActiveMoveSpeed(previousMoveSpeed)
        }
    }

    private suspend fun ProtectedAccess.walkToObstacleStart(obstacle: AgilityObstacle) {
        if (coords == obstacle.start) {
            return
        }
        playerWalkWithMinDelay(obstacle.start)
        if (coords != obstacle.start) {
            telejump(obstacle.start)
        }
    }

    private suspend fun ProtectedAccess.obstacleWalk(
        obstacle: AgilityObstacle,
        bas: BasType,
        start: CoordGrid,
    ) {
        val previousBas = player.bas
        var current = start
        player.abortRoute()
        player.clearInteraction()
        player.bas = bas
        player.cachedMoveSpeed = MoveSpeed.Walk
        player.moveSpeed = MoveSpeed.Walk

        try {
            val path = obstacle.path.ifEmpty { listOf(obstacle.end) }
            for (waypoint in path) {
                current = walkSegment(current, waypoint)
            }
        } finally {
            player.bas = previousBas
        }
    }

    private suspend fun ProtectedAccess.walkSegment(start: CoordGrid, end: CoordGrid): CoordGrid {
        if (start.level != end.level) {
            telejump(end)
            return end
        }

        val xStep = end.x.compareTo(start.x).coerceIn(-1, 1)
        val zStep = end.z.compareTo(start.z).coerceIn(-1, 1)
        if (xStep != 0 && zStep != 0) {
            val corner = CoordGrid(end.x, start.z, start.level)
            val aligned = walkSegment(start, corner)
            return walkSegment(aligned, end)
        }

        var current = start
        while (current != end) {
            val next = current.translate(xStep, zStep)
            player.removeBlockWalkCollision(collision, current)
            player.lastProcessedCoord = current
            player.coords = next
            player.addBlockWalkCollision(collision, next)
            player.lastMovement = player.currentMapClock
            player.pendingStepCount = 1
            current = next
            delay(1)
        }
        return current
    }

    private companion object {
        private const val AGILITY_STAT = "stat.agility"
        private const val BARBARIAN_OUTPOST_LADDER_TOP = 42487
        private const val BARBARIAN_OUTPOST_PIPE = 20210
        private const val BARBARIAN_OUTPOST_PIPE_LEVEL = 32
        private const val BARBARIAN_OUTPOST_PIPE_Z = 3559
        private val BARBARIAN_OUTPOST_LADDER_TOP_COORD = CoordGrid(2532, 3545, 1)
        private val BARBARIAN_OUTPOST_LADDER_START = CoordGrid(2532, 3546, 1)
        private val BARBARIAN_OUTPOST_LADDER_END = CoordGrid(2532, 3553, 0)
        private val BARBARIAN_OUTPOST_FRONT_GATE_LOCS =
            intArrayOf(
                2115,
                2116,
                11620,
                11621,
                11624,
                11625,
                11721,
                11722,
                11766,
                11767,
                11770,
                11771,
                20045,
                20046,
                20049,
                20050,
            )

        private val BALANCE_BAS =
            BasType(
                readyAnim = 763,
                turnOnSpot = 763,
                walkForward = 762,
                walkBack = 762,
                walkLeft = 762,
                walkRight = 762,
                running = 762,
            )

        private val WALL_BAS =
            BasType(
                readyAnim = 757,
                turnOnSpot = 757,
                walkForward = 756,
                walkBack = 756,
                walkLeft = 756,
                walkRight = 756,
                running = 756,
            )
    }

    private suspend fun ProtectedAccess.delayObstacleMove(obstacle: AgilityObstacle) {
        val anim = obstacle.anim
        val replayInterval = obstacle.animReplayInterval
        if (anim == null || replayInterval == null) {
            delay(obstacle.delay)
            return
        }

        var remaining = obstacle.delay
        while (remaining > 0) {
            val nextDelay = minOf(replayInterval, remaining)
            delay(nextDelay)
            remaining -= nextDelay
            if (remaining > 0) {
                animProtect(false)
                anim(anim)
                animProtect(true)
            }
        }
    }

    private fun ProtectedAccess.awardObstacleXp(obstacle: AgilityObstacle) {
        val xp = obstacle.xp * xpMods.get(player, AGILITY_STAT)
        statAdvance(AGILITY_STAT, xp)
    }

    private fun ProtectedAccess.updateCourseProgress(obstacle: AgilityObstacle) {
        val course = obstacle.course
        val previousStep = player.attr[course.stepAttr] ?: 0
        val expectedStep = previousStep + 1

        if (obstacle.step != expectedStep) {
            player.attr[course.stepAttr] = if (obstacle.step == 1) 1 else 0
            return
        }

        if (obstacle.step == course.obstacleCount) {
            val lapXp = course.lapXp * xpMods.get(player, AGILITY_STAT)
            statAdvance(AGILITY_STAT, lapXp)
            player.attr[course.stepAttr] = 0
            player.attr.increment(course.lapsAttr)
            mes("You completed a lap of the ${course.name}.")
            return
        }

        player.attr[course.stepAttr] = obstacle.step
    }

    private suspend fun ProtectedAccess.squeezeBarbarianPipe() {
        arriveDelay()

        if (player.agilityLvl < BARBARIAN_OUTPOST_PIPE_LEVEL) {
            mes("You need an Agility level of $BARBARIAN_OUTPOST_PIPE_LEVEL to use this shortcut.")
            return
        }

        val south = CoordGrid(2552, 3558, 0)
        val north = CoordGrid(2552, 3561, 0)
        val fromSouth = coords.z <= BARBARIAN_OUTPOST_PIPE_Z
        val start = if (fromSouth) south else north
        val end = if (fromSouth) north else south
        val dir = if (fromSouth) constants.em_face_north else constants.em_face_south

        if (coords != start) {
            playerWalkWithMinDelay(start)
        }

        faceSquare(end)
        try {
            anim(AgilityData.pipeAnim)
            animProtect(true)
            exactMove(start = start, end = end, delay1 = 0, delay2 = 150, dir = dir)
            delay(5)
        } finally {
            animProtect(false)
            resetAnim()
        }
    }

    private suspend fun ProtectedAccess.climbDownBarbarianOutpostLadder() {
        arriveDelay()

        if (coords != BARBARIAN_OUTPOST_LADDER_START) {
            playerWalkWithMinDelay(BARBARIAN_OUTPOST_LADDER_START)
        }

        faceSquare(BARBARIAN_OUTPOST_LADDER_TOP_COORD)
        anim("seq.human_reachforladder")
        delay(2)
        telejump(BARBARIAN_OUTPOST_LADDER_END)
    }

    private suspend fun ProtectedAccess.passBarbarianOutpostFrontGate(gate: BoundLocInfo) {
        arriveDelay()
        telejump(coords.passThroughGate(gate))
    }

    private fun CoordGrid.isBarbarianOutpostFrontGateArea(): Boolean =
        level == 0 && x in 2538..2558 && z in 3560..3582

    private fun CoordGrid.passThroughGate(gate: BoundLocInfo): CoordGrid {
        val xDistance = kotlin.math.abs(x - gate.x)
        val zDistance = kotlin.math.abs(z - gate.z)
        return if (xDistance >= zDistance) {
            if (x <= gate.x) translateX(2) else translateX(-2)
        } else {
            if (z <= gate.z) translateZ(2) else translateZ(-2)
        }
    }
}
