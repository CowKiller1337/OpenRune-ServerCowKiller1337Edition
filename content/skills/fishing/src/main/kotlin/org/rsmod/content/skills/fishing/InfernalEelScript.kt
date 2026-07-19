package org.rsmod.content.skills.fishing

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onPlayerQueue
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class InfernalEelScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld1(INFERNAL_EEL) { mes("The eel is far too hot to eat.") }
        onOpHeldU(HAMMER, INFERNAL_EEL) { startCrackingEels() }
        onPlayerQueue(CRACK_QUEUE) { crackEel() }
    }

    private fun ProtectedAccess.startCrackingEels() {
        if (!inv.contains(HAMMER)) {
            mes("You need a hammer to crack open infernal eels.")
            return
        }
        if (!inv.contains(INFERNAL_EEL)) {
            return
        }
        weakQueue(CRACK_QUEUE, 1)
    }

    private fun ProtectedAccess.crackEel() {
        if (!inv.contains(HAMMER) || !inv.contains(INFERNAL_EEL)) {
            resetAnim()
            return
        }

        val reward = rollInfernalEelReward()
        if (!canFitRewardAfterEelIsRemoved(reward.obj)) {
            mes("You don't have enough inventory space.")
            resetAnim()
            return
        }

        anim("seq.human_hammering")
        if (invDel(inv, INFERNAL_EEL).failure) {
            resetAnim()
            return
        }

        invAdd(inv, reward.obj, reward.count)
        statAdvance("stat.cooking", COOKING_XP)
        mes("You crack open the infernal eel.")

        if (inv.contains(INFERNAL_EEL)) {
            weakQueue(CRACK_QUEUE, CRACK_DELAY)
        } else {
            resetAnim()
        }
    }

    private fun ProtectedAccess.canFitRewardAfterEelIsRemoved(obj: String): Boolean {
        return obj in inv || inv.freeSpace() + 1 >= 1
    }

    private fun ProtectedAccess.rollInfernalEelReward(): InfernalEelReward {
        return when {
            random.of(maxExclusive = 16) == 0 -> InfernalEelReward(ONYX_BOLT_TIPS, 1)
            random.of(maxExclusive = 12) == 0 -> InfernalEelReward(LAVA_SCALE, random.of(1..5))
            else -> InfernalEelReward(TOKKUL, random.of(10..20))
        }
    }

    private data class InfernalEelReward(val obj: String, val count: Int)

    private companion object {
        private const val CRACK_QUEUE = "queue.fishing_crack_infernal_eel"
        private const val CRACK_DELAY = 3
        private const val COOKING_XP = 95.0

        private const val HAMMER = "obj.hammer"
        private const val INFERNAL_EEL = "obj.infernal_eel"
        private const val TOKKUL = "obj.tzhaar_token"
        private const val LAVA_SCALE = "obj.lava_scale"
        private const val ONYX_BOLT_TIPS = "obj.xbows_bolt_tips_onyx"
    }
}
