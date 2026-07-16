package org.rsmod.content.other.leagues

import jakarta.inject.Inject
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.statBase
import org.rsmod.game.entity.Player

private val LEAGUE_TASK_MESSAGE_QUEUE_ATTR = AttributeKey<ArrayDeque<String>>(temp = true)
private val LEAGUE_TASK_MESSAGE_ACTIVE_ATTR = AttributeKey<Boolean>(temp = true)

private var Player.leagueTaskMessageQueue: ArrayDeque<String>?
    get() = attr[LEAGUE_TASK_MESSAGE_QUEUE_ATTR]
    set(value) {
        if (value == null || value.isEmpty()) {
            attr.remove(LEAGUE_TASK_MESSAGE_QUEUE_ATTR)
        } else {
            attr[LEAGUE_TASK_MESSAGE_QUEUE_ATTR] = value
        }
    }

private var Player.leagueTaskMessageActive: Boolean
    get() = attr[LEAGUE_TASK_MESSAGE_ACTIVE_ATTR] == true
    set(value) {
        if (value) {
            attr[LEAGUE_TASK_MESSAGE_ACTIVE_ATTR] = true
        } else {
            attr.remove(LEAGUE_TASK_MESSAGE_ACTIVE_ATTR)
        }
    }

class LeagueTaskService
@Inject
constructor(
    private val leagues: LeagueService,
) {
    fun startStarterTaskChecks(player: Player) {
        checkStarterTaskProgress(player)
        player.softTimer(STARTER_TASK_TIMER, STARTER_TASK_CHECK_CYCLES)
    }

    fun checkStarterTaskProgress(player: Player) {
        if (!player.leagueProfile.enabled) {
            return
        }
        val highestSkill = player.highestStarterSkillLevel()
        val readyTasks =
            LeagueTaskCatalog.firstLevelTasks.filter { task ->
                val threshold = LeagueTaskCatalog.run { task.firstLevelThreshold() } ?: return@filter false
                highestSkill >= threshold
            }
        completeAll(player, readyTasks)
    }

    fun completeByIndex(player: Player, index: Int, pointsOverride: Int? = null): Boolean {
        val task = LeagueTaskCatalog.byIndex(index)
            ?: LeagueTask(index, null, "League task $index", null, pointsOverride ?: DEFAULT_TASK_POINTS, null)
        return complete(player, task, pointsOverride)
    }

    fun complete(player: Player, task: LeagueTask, pointsOverride: Int? = null): Boolean {
        if (!player.leagueProfile.enabled) {
            return false
        }
        val points = pointsOverride ?: task.points
        val completed = leagues.completeTask(player, task.profileId, points)
        if (completed) {
            player.enqueueTaskCompletedMessage(task.name, points)
        }
        return completed
    }

    fun flushQueuedTaskMessage(player: Player) {
        val queue = player.leagueTaskMessageQueue
        val message = queue?.removeFirstOrNull()
        if (message == null) {
            player.leagueTaskMessageQueue = queue
            player.leagueTaskMessageActive = false
            return
        }
        player.mes(message)
        player.leagueTaskMessageQueue = queue
        player.softTimer(TASK_MESSAGE_TIMER, TASK_MESSAGE_DELAY_CYCLES)
    }

    private fun Player.highestStarterSkillLevel(): Int =
        STARTER_LEVEL_STATS.maxOf { statBase(it) }

    private fun completeAll(player: Player, tasks: List<LeagueTask>) {
        if (tasks.isEmpty() || !player.leagueProfile.enabled) {
            return
        }
        val completed =
            leagues.completeTasks(
                player,
                tasks.map { task -> task.profileId to task.points },
            )
        tasks.forEach { task ->
            if (task.profileId in completed) {
                player.enqueueTaskCompletedMessage(task.name, task.points)
            }
        }
    }

    private fun Player.enqueueTaskCompletedMessage(taskName: String, points: Int) {
        val message = rainbowMessage("League task completed: $taskName (+$points points).")
        if (!leagueTaskMessageActive) {
            leagueTaskMessageActive = true
            mes(message)
            softTimer(TASK_MESSAGE_TIMER, TASK_MESSAGE_DELAY_CYCLES)
            return
        }

        val queue = leagueTaskMessageQueue ?: ArrayDeque()
        queue.addLast(message)
        leagueTaskMessageQueue = queue
    }

    private fun rainbowMessage(text: String): String = buildString {
        var colorIndex = 0
        for (char in text) {
            if (char.isWhitespace()) {
                append(char)
                continue
            }
            append("<col=")
            append(RAINBOW_MESSAGE_COLORS[colorIndex % RAINBOW_MESSAGE_COLORS.size])
            append(">")
            append(char)
            colorIndex++
        }
    }

    companion object {
        const val STARTER_TASK_TIMER = "timer.league_task_check"
        const val STARTER_TASK_CHECK_CYCLES = 1
        const val TASK_MESSAGE_TIMER = "timer.league_task_message"
        const val TASK_MESSAGE_DELAY_CYCLES = 1

        private const val DEFAULT_TASK_POINTS = 10
        private val RAINBOW_MESSAGE_COLORS =
            arrayOf("ff3333", "ff8c00", "ffff33", "33cc33", "33ccff", "6666ff", "ff66cc")

        private val STARTER_LEVEL_STATS =
            listOf(
                "stat.attack",
                "stat.defence",
                "stat.strength",
                "stat.ranged",
                "stat.prayer",
                "stat.magic",
                "stat.cooking",
                "stat.woodcutting",
                "stat.fletching",
                "stat.fishing",
                "stat.firemaking",
                "stat.crafting",
                "stat.smithing",
                "stat.mining",
                "stat.herblore",
                "stat.agility",
                "stat.thieving",
                "stat.slayer",
                "stat.farming",
                "stat.runecrafting",
                "stat.hunter",
                "stat.construction",
            )
    }
}
