package org.rsmod.content.generic.locs.banks

import dev.openrune.ServerCacheManager
import dev.openrune.types.ObjectServerType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpContentLoc2
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLoc4
import org.rsmod.api.script.onOpLoc5
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BankBooth : PluginScript() {
    override fun ScriptContext.startup() {
        onOpContentLoc2("content.bank_booth") { openBank() }
        onOpContentLoc2("content.bank_chest") { openBank() }
        bindDiscoveredBankAccessLocs()
    }

    private fun ScriptContext.bindDiscoveredBankAccessLocs() {
        for (type in ServerCacheManager.getObjects().values) {
            if (!type.isBankAccessLoc()) {
                continue
            }
            bindBankAccessLoc(type)
        }
    }

    private fun ScriptContext.bindBankAccessLoc(type: ObjectServerType) {
        val loc = type.safeInternalName() ?: return
        for (slot in 1..5) {
            if (!type.isBankAccessSlot(slot)) {
                continue
            }
            when (slot) {
                1 -> onOpLoc1(loc) { openBank() }
                2 -> onOpLoc2(loc) { openBank() }
                3 -> onOpLoc3(loc) { openBank() }
                4 -> onOpLoc4(loc) { openBank() }
                5 -> onOpLoc5(loc) { openBank() }
            }
        }
    }

    private fun ProtectedAccess.openBank() {
        ifOpenMainSidePair(main = "interface.bankmain", side = "interface.bankside")
    }

    private companion object {
        private val accessDescSubstrings =
            listOf(
                "bank teller will serve you from here",
                "use for quick access to your bank",
                "allows you to access your bank account",
                "i can access my bank from here",
                "a handy bank chest",
                "a convenient bank chest",
                "an open bank chest",
                "for all your banking needs",
                "banking business can be conducted here",
                "banking transactions are processed here",
            )

        private fun ObjectServerType.isBankAccessLoc(): Boolean {
            if ((1..5).none { isBankAccessSlot(it) }) {
                return false
            }
            val text = "${name.lowercase()} ${desc.lowercase()} ${safeInternalName().orEmpty().lowercase()}"
            return accessDescSubstrings.any(text::contains) ||
                text.contains("bankbooth") ||
                text.contains("bank booth") ||
                text.contains("bankchest") ||
                text.contains("bank chest")
        }

        private fun ObjectServerType.isBankAccessSlot(slot: Int): Boolean {
            val op = actions.getOpOrNull(slot - 1)?.lowercase() ?: return false
            return op == "bank" || op == "use" || op == "open"
        }

        private fun ObjectServerType.safeInternalName(): String? =
            runCatching { internalName }.getOrNull()
    }
}
