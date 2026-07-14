package org.rsmod.content.other.leagues

import org.rsmod.plugin.module.PluginModule

class LeagueModule : PluginModule() {
    override fun bind() {
        bindInstance<LeagueService>()
    }
}
