package org.rsmod.content.other.leagues

import org.rsmod.api.table.ActionRow

data class LeagueTask(
    val index: Int,
    val rowId: Int?,
    val name: String,
    val description: String?,
    val points: Int,
    val difficulty: Int?,
) {
    val profileId: String
        get() = "task_$index"
}

object LeagueTaskCatalog {
    val tasks: List<LeagueTask> by lazy { loadCurrentLeagueTasks() }

    val firstLevelTasks: List<LeagueTask> by lazy {
        tasks.mapNotNull { task ->
            val threshold = task.firstLevelThreshold() ?: return@mapNotNull null
            task to threshold
        }.sortedBy { it.second }.map { it.first }
    }

    fun byIndex(index: Int): LeagueTask? = tasks.getOrNull(index)

    fun byName(name: String): LeagueTask? =
        tasks.firstOrNull { task -> task.name.equals(name, ignoreCase = true) }

    fun find(query: String, limit: Int = 10): List<LeagueTask> {
        val terms = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (terms.isEmpty()) {
            return tasks.take(limit)
        }
        return tasks
            .asSequence()
            .filter { task ->
                val blob = "${task.index} ${task.rowId.orEmpty()} ${task.name} ${task.description.orEmpty()}".lowercase()
                terms.all(blob::contains)
            }
            .take(limit)
            .toList()
    }

    fun LeagueTask.firstLevelThreshold(): Int? {
        if (name.equals("Achieve Your First Level Up", ignoreCase = true)) {
            return 2
        }
        val match = firstLevelRegex.matchEntire(name) ?: return null
        return match.groupValues[1].toIntOrNull()
    }

    private fun loadCurrentLeagueTasks(): List<LeagueTask> {
        val cacheTasks =
            runCatching {
                ActionRow.all()
                    .asSequence()
                    .filter { it.inCurrentLeague == true }
                    .sortedBy { it.rowId }
                    .mapIndexed { index, row ->
                        LeagueTask(
                            index = index,
                            rowId = row.rowId,
                            name = row.actionName,
                            description = row.actionDesc,
                            points = row.actionDifficulty.leaguePoints(),
                            difficulty = row.actionDifficulty,
                        )
                    }
                    .toList()
            }.getOrDefault(emptyList())
        return cacheTasks.ifEmpty { fallbackStarterTasks }
    }

    private fun Int?.leaguePoints(): Int =
        when (this) {
            0, 1, null -> 10
            2 -> 40
            3 -> 80
            4 -> 200
            5 -> 400
            else -> 10
        }

    private fun Int?.orEmpty(): String = this?.toString().orEmpty()

    private val firstLevelRegex = Regex("""Achieve Your First Level (\d+)""", RegexOption.IGNORE_CASE)

    private val fallbackStarterTasks =
        listOf(
            LeagueTask(0, null, "Achieve Your First Level Up", null, 10, null),
            LeagueTask(1, null, "Achieve Your First Level 5", null, 10, null),
            LeagueTask(2, null, "Achieve Your First Level 10", null, 10, null),
        )
}
