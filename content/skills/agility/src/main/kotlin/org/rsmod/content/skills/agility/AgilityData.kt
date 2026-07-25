package org.rsmod.content.skills.agility

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.config.constants
import org.rsmod.map.CoordGrid

internal data class AgilityCourse(
    val key: String,
    val name: String,
    val obstacleCount: Int,
    val lapXp: Double,
    val stepAttr: AttributeKey<Int>,
    val lapsAttr: AttributeKey<Int>,
)

internal data class AgilityObstacle(
    val course: AgilityCourse,
    val step: Int,
    val locs: IntArray,
    val level: Int,
    val xp: Double,
    val start: CoordGrid,
    val end: CoordGrid,
    val anim: String?,
    val animReplayInterval: Int? = null,
    val delay: Int,
    val exactMoveDelay: Int,
    val faceDir: Int,
    val movement: AgilityMovement,
)

internal enum class AgilityMovement {
    ExactMove,
    StraightWalk,
    TelejumpAfterDelay,
}

internal object AgilityData {
    val gnomeStrongholdCourse =
        AgilityCourse(
            key = "gnome_stronghold",
            name = "Gnome Stronghold Agility Course",
            obstacleCount = 7,
            lapXp = 39.5,
            stepAttr = GNOME_STRONGHOLD_COURSE_STEP_ATTR,
            lapsAttr = GNOME_STRONGHOLD_LAPS_ATTR,
        )

    val obstacles =
        listOf(
            AgilityObstacle(
                course = gnomeStrongholdCourse,
                step = 1,
                locs = intArrayOf(23145),
                level = 1,
                xp = 7.5,
                start = CoordGrid(2474, 3436, 0),
                end = CoordGrid(2474, 3429, 0),
                anim = null,
                delay = 7,
                exactMoveDelay = 0,
                faceDir = constants.em_face_south,
                movement = AgilityMovement.StraightWalk,
            ),
            AgilityObstacle(
                course = gnomeStrongholdCourse,
                step = 2,
                locs = intArrayOf(23134),
                level = 1,
                xp = 7.5,
                start = CoordGrid(2473, 3425, 0),
                end = CoordGrid(2473, 3425, 1),
                anim = "seq.human_reachforladder",
                delay = 2,
                exactMoveDelay = 0,
                faceDir = constants.em_face_north,
                movement = AgilityMovement.TelejumpAfterDelay,
            ),
            AgilityObstacle(
                course = gnomeStrongholdCourse,
                step = 3,
                locs = intArrayOf(23559),
                level = 1,
                xp = 5.0,
                start = CoordGrid(2473, 3422, 1),
                end = CoordGrid(2473, 3420, 2),
                anim = "seq.human_reachforladder",
                delay = 2,
                exactMoveDelay = 0,
                faceDir = constants.em_face_north,
                movement = AgilityMovement.TelejumpAfterDelay,
            ),
            AgilityObstacle(
                course = gnomeStrongholdCourse,
                step = 4,
                locs = intArrayOf(23557),
                level = 1,
                xp = 7.5,
                start = CoordGrid(2477, 3420, 2),
                end = CoordGrid(2483, 3420, 2),
                anim = null,
                delay = 6,
                exactMoveDelay = 0,
                faceDir = constants.em_face_east,
                movement = AgilityMovement.StraightWalk,
            ),
            AgilityObstacle(
                course = gnomeStrongholdCourse,
                step = 5,
                locs = intArrayOf(23560),
                level = 1,
                xp = 5.0,
                start = CoordGrid(2485, 3420, 2),
                end = CoordGrid(2485, 3421, 0),
                anim = "seq.human_reachforladder",
                delay = 2,
                exactMoveDelay = 0,
                faceDir = constants.em_face_south,
                movement = AgilityMovement.TelejumpAfterDelay,
            ),
            AgilityObstacle(
                course = gnomeStrongholdCourse,
                step = 6,
                locs = intArrayOf(23135),
                level = 1,
                xp = 7.5,
                start = CoordGrid(2486, 3425, 0),
                end = CoordGrid(2486, 3427, 0),
                anim = "seq.human_reachforladder",
                delay = 2,
                exactMoveDelay = 60,
                faceDir = constants.em_face_north,
                movement = AgilityMovement.ExactMove,
            ),
            AgilityObstacle(
                course = gnomeStrongholdCourse,
                step = 7,
                locs = intArrayOf(23138),
                level = 1,
                xp = 7.5,
                start = CoordGrid(2484, 3430, 0),
                end = CoordGrid(2484, 3437, 0),
                anim = seq(PIPE_ANIM),
                animReplayInterval = 2,
                delay = 6,
                exactMoveDelay = 180,
                faceDir = constants.em_face_north,
                movement = AgilityMovement.ExactMove,
            ),
            AgilityObstacle(
                course = gnomeStrongholdCourse,
                step = 7,
                locs = intArrayOf(23139),
                level = 1,
                xp = 7.5,
                start = CoordGrid(2487, 3430, 0),
                end = CoordGrid(2487, 3437, 0),
                anim = seq(PIPE_ANIM),
                animReplayInterval = 2,
                delay = 6,
                exactMoveDelay = 180,
                faceDir = constants.em_face_north,
                movement = AgilityMovement.ExactMove,
            ),
        )

    private fun seq(id: Int): String =
        RSCM.getReverseMapping(RSCMType.SEQ, id) ?: error("Missing agility seq: $id")

    private const val BALANCE_ANIM: Int = 762
    private const val PIPE_ANIM: Int = 749
}
