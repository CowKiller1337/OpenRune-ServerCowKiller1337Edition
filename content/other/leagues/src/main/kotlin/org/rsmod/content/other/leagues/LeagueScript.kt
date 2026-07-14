package org.rsmod.content.other.leagues

import dev.or2.central.account.Rights
import jakarta.inject.Inject
import org.rsmod.api.cheat.CheatHandlerBuilder
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.ui.ifOpenMainModal
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onIfOpen
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.Cheat
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class LeagueScript
@Inject
constructor(
    private val leagues: LeagueService,
    private val eventBus: EventBus,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerLogin { leagues.enable(player) }
        onIfOpen("interface.league_side_panel") { leagues.applySavedState(player) }

        onIfOverlayButton("component.league_side_panel:tasks_button") {
            player.ifOpenMainModal("interface.league_tasks", eventBus)
        }

        onIfOverlayButton("component.league_side_panel:mastery_button") {
            player.ifOpenMainModal("interface.league_combat_mastery", eventBus)
        }

        onIfOverlayButton("component.league_side_panel:areas_button") {
            player.ifOpenMainModal("interface.trailblazer_areas", eventBus)
        }

        onIfOverlayButton("component.league_side_panel:relics_button") {
            player.ifOpenMainModal("interface.league_relics", eventBus)
        }

        onAdminCommand("leagueon", "Enable Leagues VI for yourself.", ::leagueOn)
        onAdminCommand("leagueoff", "Disable Leagues VI for yourself.", ::leagueOff)
        onAdminCommand("leaguepoints", "Set or add Leagues VI points.", ::leaguePoints) {
            invalidArgs = "Use as ::leaguepoints set|add amount"
        }
        onAdminCommand("leagueprofile", "Show your saved Leagues VI profile.", ::leagueProfile)
    }

    private fun leagueOn(cheat: Cheat) =
        with(cheat) {
            val profile = leagues.enable(player)
            player.mes("${LeagueConstants.CURRENT_LEAGUE_NAME} enabled.")
            player.mes("League points: ${profile.leaguePoints}; pact points: ${profile.pactPointsEarned}.")
        }

    private fun leagueOff(cheat: Cheat) =
        with(cheat) {
            leagues.disable(player)
            player.mes("${LeagueConstants.CURRENT_LEAGUE_NAME} disabled.")
        }

    private fun leaguePoints(cheat: Cheat) =
        with(cheat) {
            val mode = args[0].lowercase()
            val amount = args[1].toInt()
            val profile =
                when (mode) {
                    "set" -> leagues.setLeaguePoints(player, amount)
                    "add" -> leagues.addLeaguePoints(player, amount)
                    else -> error("Invalid mode: $mode")
                }
            player.mes("League points set to ${profile.leaguePoints}.")
        }

    private fun leagueProfile(cheat: Cheat) =
        with(cheat) {
            val profile = player.leagueProfile
            player.mes(
                "League enabled=${profile.enabled}, points=${profile.leaguePoints}, " +
                    "tasks=${profile.completedTaskIds.size}, relics=${profile.selectedRelics.size}.",
            )
            player.mes(
                "Pacts earned=${profile.pactPointsEarned}, spent=${profile.pactPointsSpent}, " +
                    "unlocked=${profile.unlockedPactIds.size}, resets=${profile.pactResetsAvailable}.",
            )
        }

    private fun ScriptContext.onAdminCommand(
        command: String,
        desc: String,
        cheat: Cheat.() -> Unit,
        init: CheatHandlerBuilder.() -> Unit = {},
    ) {
        onCommand(command) {
            this.desc = desc
            this.requiredRights = Rights.ADMINISTRATOR
            this.invalidRights = "You need administrator rights to use that command."
            this.cheat(cheat)
            init()
        }
    }
}
