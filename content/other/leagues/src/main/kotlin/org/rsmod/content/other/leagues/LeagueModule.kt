package org.rsmod.content.other.leagues

import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.plugin.module.PluginModule

class LeagueModule : PluginModule() {
    override fun bind() {
        bindInstance<LeagueService>()
        bindInstance<LeagueTaskService>()
        addSetBinding<NpcDeathKillHook>(LeagueNpcKillHook::class.java)
    }
}
