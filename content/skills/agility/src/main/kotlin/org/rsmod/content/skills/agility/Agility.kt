package org.rsmod.content.skills.agility

import dev.openrune.types.BasType
import jakarta.inject.Inject
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.agilityLvl
import org.rsmod.api.script.onProtectedEvent
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.game.movement.MoveSpeed
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

class Agility
@Inject
constructor(private val xpMods: XpModifiers, private val collision: CollisionFlagMap) :
    PluginScript() {
    override fun ScriptContext.startup() {
        for (obstacle in AgilityData.obstacles) {
            for (loc in obstacle.locs) {
                onProtectedEvent<LocEvents.Op1>(loc) { cross(obstacle) }
            }
        }
    }

    private suspend fun ProtectedAccess.cross(obstacle: AgilityObstacle) {
        arriveDelay()

        if (player.agilityLvl < obstacle.level) {
            mes("You need an Agility level of ${obstacle.level} to use this obstacle.")
            return
        }

        walkToObstacleStart(obstacle)
        faceSquare(obstacle.end)
        val protectExactMoveAnim = obstacle.anim != null && obstacle.movement == AgilityMovement.ExactMove
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
                        start = obstacle.start,
                        end = obstacle.end,
                        delay1 = 0,
                        delay2 = obstacle.exactMoveDelay,
                        dir = obstacle.faceDir,
                    )
                    delayObstacleMove(obstacle)
                }
                AgilityMovement.StraightWalk -> {
                    straightObstacleWalk(obstacle)
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
    }

    private suspend fun ProtectedAccess.walkToObstacleStart(obstacle: AgilityObstacle) {
        if (coords == obstacle.start) {
            return
        }
        playerWalkWithMinDelay(obstacle.start)
    }

    private suspend fun ProtectedAccess.straightObstacleWalk(obstacle: AgilityObstacle) {
        val xStep = obstacle.end.x.compareTo(obstacle.start.x).coerceIn(-1, 1)
        val zStep = obstacle.end.z.compareTo(obstacle.start.z).coerceIn(-1, 1)
        require(xStep == 0 || zStep == 0) {
            "StraightWalk only supports straight-line obstacles: $obstacle"
        }

        val previousBas = player.bas
        var current = obstacle.start
        player.abortRoute()
        player.clearInteraction()
        player.bas = BALANCE_BAS
        player.cachedMoveSpeed = MoveSpeed.Walk
        player.moveSpeed = MoveSpeed.Walk

        try {
            while (current != obstacle.end) {
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
        } finally {
            player.bas = previousBas
        }
    }

    private companion object {
        private const val AGILITY_STAT = "stat.agility"

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
}
