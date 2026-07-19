package org.rsmod.content.generic.locs.ladders

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ObjectServerType
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpContentLoc1
import org.rsmod.api.script.onOpContentLoc2
import org.rsmod.api.script.onOpContentLoc3
import org.rsmod.api.script.onProtectedEvent
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class LadderScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpContentLoc1("content.ladder_down") {
            arriveDelay()
            climbDown(it.type)
        }
        onOpContentLoc1("content.ladder_up") {
            arriveDelay()
            climbUp(it.type)
        }
        onOpContentLoc1("content.ladder_option") {
            arriveDelay()
            climbOption(it.type)
        }
        onOpContentLoc2("content.ladder_option") {
            arriveDelay()
            climbUp(it.type)
        }
        onOpContentLoc3("content.ladder_option") {
            arriveDelay()
            climbDown(it.type)
        }
        for (type in ROPE_LADDER_UP_TYPES) {
            onProtectedEvent<LocEvents.Op1>(type) {
                arriveDelay()
                climbRopeLadder(1)
            }
        }
        for (type in ROPE_LADDER_DOWN_TYPES) {
            onProtectedEvent<LocEvents.Op1>(type) {
                arriveDelay()
                climbRopeLadder(-1)
            }
        }
    }

    private suspend fun ProtectedAccess.climbUp(type: ObjectServerType): Unit = climb(type, 1)

    private suspend fun ProtectedAccess.climbDown(type: ObjectServerType): Unit = climb(type, -1)

    private suspend fun ProtectedAccess.climb(type: ObjectServerType, translateLevel: Int) {
        val dest = player.coords.translateLevel(translateLevel)
        anim(type.climbAnim())
        delay(1)
        telejump(dest)
    }

    private suspend fun ProtectedAccess.climbRopeLadder(translateLevel: Int) {
        val dest = player.coords.translateLevel(translateLevel)
        anim("seq.human_reachforladder")
        delay(1)
        telejump(dest)
    }

    private suspend fun ProtectedAccess.climbOption(type: ObjectServerType) = startDialogue {
        val translate =
            choice2("Climb-up", 1, "Climb-down", -1, title = "Climb up or down the ladder?")
        val dest = player.coords.translateLevel(translate)
        anim(type.climbAnim())
        delay(2)
        telejump(dest)
    }

    private fun ObjectServerType.climbAnim(): String =
        RSCM.getReverseMapping(RSCMType.SEQ, param(params.climb_anim).id)

    private companion object {
        private val ROPE_LADDER_UP_TYPES = intArrayOf(28857, 32321, 32322, 32323, 34477)
        private val ROPE_LADDER_DOWN_TYPES = intArrayOf(28858, 34478)
    }
}
