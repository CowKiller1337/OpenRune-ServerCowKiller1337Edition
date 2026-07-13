package org.rsmod.content.bosses.brutus

import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.plugin.module.PluginModule

class BrutusModule : PluginModule() {
    override fun bind() {
        addSetBinding<NpcDeathKillHook>(BrutusInstanceCleanupHook::class.java)
    }
}
