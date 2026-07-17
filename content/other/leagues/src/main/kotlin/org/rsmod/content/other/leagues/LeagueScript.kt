package org.rsmod.content.other.leagues

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.StructType
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.or2.central.account.Rights
import org.rsmod.api.attr.AttributeKey
import jakarta.inject.Inject
import org.rsmod.api.cheat.CheatHandlerBuilder
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.ui.IfScriptArgs
import org.rsmod.api.player.ui.ifCloseModals
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifOpenMainModal
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.ui.ifSetHide
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onIfOpen
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onIfScriptTrigger
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.Cheat
import org.rsmod.game.entity.Player
import org.rsmod.game.ui.Component
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

data class PactApplyArgs(val nodeIds: IntArray) : IfScriptArgs

class PactResetArgs : IfScriptArgs

data class RelicToggleArgs(val dbrow: Int) : IfScriptArgs

private val PENDING_LEAGUE_RELIC_ATTR = AttributeKey<LeagueRelicChoice>(temp = true)
private val PENDING_LEAGUE_AREA_ATTR = AttributeKey<LeagueAreaChoice>(temp = true)

private var Player.pendingLeagueRelic: LeagueRelicChoice?
    get() = attr[PENDING_LEAGUE_RELIC_ATTR]
    set(value) {
        if (value == null) {
            attr.remove(PENDING_LEAGUE_RELIC_ATTR)
        } else {
            attr[PENDING_LEAGUE_RELIC_ATTR] = value
        }
    }

private var Player.pendingLeagueArea: LeagueAreaChoice?
    get() = attr[PENDING_LEAGUE_AREA_ATTR]
    set(value) {
        if (value == null) {
            attr.remove(PENDING_LEAGUE_AREA_ATTR)
        } else {
            attr[PENDING_LEAGUE_AREA_ATTR] = value
        }
    }

private data class LeagueRelicTier(
    val tierIndex: Int,
    val tier: Int,
    val requiredPoints: Int,
    val choicesEnumId: Int,
)

private data class LeagueRelicChoice(
    val tierIndex: Int,
    val tier: Int,
    val choice: Int,
    val structId: Int,
    val name: String,
    val description: String,
    val requiredPoints: Int,
)

private data class LeagueAreaChoice(
    val areaId: Int,
    val regionId: String,
    val name: String,
    val requiredTasks: Int = 0,
)

class LeagueScript
@Inject
constructor(
    private val leagues: LeagueService,
    private val leagueTasks: LeagueTaskService,
    private val eventBus: EventBus,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerLogin {
            leagues.enable(player)
            leagueTasks.startStarterTaskChecks(player)
        }
        onPlayerSoftTimer(LeagueTaskService.STARTER_TASK_TIMER) {
            leagueTasks.checkStarterTaskProgress(player)
            player.softTimer(
                LeagueTaskService.STARTER_TASK_TIMER,
                LeagueTaskService.STARTER_TASK_CHECK_CYCLES,
            )
        }
        onPlayerSoftTimer(LeagueTaskService.TASK_MESSAGE_TIMER) {
            leagueTasks.flushQueuedTaskMessage(player)
        }
        onIfOpen("interface.league_side_panel") {
            leagues.applySavedState(player)
            player.enableLeagueSidePanelEvents()
        }

        onIfOpen("interface.league_info") {
            player.enableLeagueModalEvents(
                menuFrame = "component.league_info:league_menu_frame",
                closeButton = "component.league_info:close_button",
            )
        }

        onIfOpen("interface.league_tasks") {
            LeagueClientState.sync(player)
            player.enableLeagueModalEvents(
                menuFrame = "component.league_tasks:league_menu_frame",
                closeButton = "component.league_tasks:close_button",
            )
            player.enableLeagueTaskEvents()
            player.redrawLeagueTasks()
        }

        onIfOpen("interface.trailblazer_areas") {
            LeagueClientState.sync(player)
            player.enableLeagueModalEvents(
                menuFrame = "component.trailblazer_areas:league_menu_frame",
                closeButton = "component.trailblazer_areas:close_button",
            )
            player.enableLeagueAreaEvents()
        }

        onIfOpen("interface.league_relics") {
            player.enableLeagueModalEvents(
                menuFrame = "component.league_relics:league_menu_frame",
                closeButton = "component.league_relics:close_button",
            )
            player.enableLeagueRelicEvents()
        }

        onIfOpen("interface.league_combat_mastery") {
            player.enableLeagueModalEvents(
                menuFrame = "component.league_combat_mastery:league_menu_frame",
                closeButton = "component.league_combat_mastery:close_button",
            )
        }

        onIfOpen("interface.talent_tree") {
            player.ifSetEvents(
                "component.talent_tree:frame",
                LEAGUE_STEELBORDER_CLOSE_SUB..LEAGUE_STEELBORDER_CLOSE_SUB,
                IfEvent.Op1,
            )
            player.enableTalentTreeEvents()
        }

        onIfOpen("interface.league_rankings") {
            player.ifSetEvents("component.league_rankings:close_button", 0..0, IfEvent.Op1)
        }

        onIfOpen("interface.league_summary") {
            player.ifSetEvents(
                "component.league_summary:frame",
                LEAGUE_STEELBORDER_CLOSE_SUB..LEAGUE_STEELBORDER_CLOSE_SUB,
                IfEvent.Op1,
            )
        }

        onIfOverlayButton("component.league_side_panel:tasks_button") {
            player.ifOpenMainModal("interface.league_tasks", eventBus)
        }

        LEAGUE_SIDE_PANEL_MASTERY_BUTTONS.forEach { component ->
            onIfOverlayButton(component) {
                player.openTalentTree()
            }
        }

        onIfOverlayButton("component.league_side_panel:areas_button") {
            player.ifOpenMainModal("interface.trailblazer_areas", eventBus)
        }

        onIfOverlayButton("component.league_side_panel:relics_button") {
            player.ifOpenMainModal("interface.league_relics", eventBus)
        }

        onIfOverlayButton("component.league_side_panel:info") {
            player.ifOpenMainModal("interface.league_info", eventBus)
        }

        LEAGUE_SIDE_PANEL_RANK_BUTTONS.forEach { component ->
            onIfOverlayButton(component) {
                player.showLocalLeagueRank()
            }
        }

        onIfOverlayButton("component.league_side_panel:summary") {
            player.ifOpenMainModal("interface.league_summary", eventBus)
        }

        LEAGUE_MODAL_CLOSE_BUTTONS.forEach { component ->
            onIfModalButton(component) { player.ifCloseModals(eventBus) }
        }

        onIfModalButton("component.league_summary:frame") {
            if (it.comsub == LEAGUE_STEELBORDER_CLOSE_SUB) {
                player.ifCloseModals(eventBus)
            }
        }

        onIfModalButton("component.talent_tree:frame") {
            if (it.comsub == LEAGUE_STEELBORDER_CLOSE_SUB) {
                player.ifCloseModals(eventBus)
            }
        }

        onIfOverlayButton("component.talent_tree:frame") {
            if (it.comsub == LEAGUE_STEELBORDER_CLOSE_SUB) {
                player.ifCloseOverlay("interface.talent_tree", eventBus)
            }
        }

        onIfScriptTrigger<PactApplyArgs>("component.talent_tree:data_layer") {
            player.applyPendingPacts(it.nodeIds)
        }

        onIfScriptTrigger<PactResetArgs>("component.talent_tree:perm_floating_panel_content") {
            player.resetPactsFromInterface()
        }

        onIfScriptTrigger<RelicToggleArgs>("component.league_relics:toggles") {
            player.redrawPendingLeagueRelicDetails()
        }

        LEAGUE_MODAL_MENU_FRAMES.forEach { component ->
            onIfModalButton(component) { player.openLeagueMenuEntry(it.comsub) }
        }

        LEAGUE_TASK_DROPDOWN_COMPONENTS.forEach { component ->
            onIfModalButton(component) { player.redrawLeagueTasks() }
        }

        onIfModalButton("component.league_relics:clickzones") {
            player.openLeagueRelicDetails(it.comsub)
        }

        onIfModalButton("component.league_relics:select_back") {
            player.closeLeagueRelicDetails()
        }

        onIfModalButton("component.league_relics:confirm_button") {
            player.confirmPendingLeagueRelic()
        }

        LEAGUE_AREA_COMPONENT_CHOICES.forEach { (component, choice) ->
            onIfModalButton(component) {
                player.openLeagueAreaDetails(choice)
            }
        }

        onIfModalButton(LEAGUE_AREA_SELECT_BUTTON_COMPONENT) {
            confirmPendingLeagueArea()
        }

        onIfModalButton(LEAGUE_AREA_SELECT_BACK_COMPONENT) {
            player.closeLeagueAreaDetails()
        }

        onAdminCommand("leagueon", "Enable Leagues VI for yourself.", ::leagueOn)
        onAdminCommand("leagueoff", "Disable Leagues VI for yourself.", ::leagueOff)
        onAdminCommand("leaguepoints", "Set or add Leagues VI points.", ::leaguePoints) {
            invalidArgs = "Use as ::leaguepoints set|add amount"
        }
        onAdminCommand("leagueprofile", "Show your saved Leagues VI profile.", ::leagueProfile)
        onAdminCommand("leaguefind", "Find Leagues VI tasks in the cache.", ::leagueFind)
        onAdminCommand("leaguetask", "Complete a Leagues VI task by index.", ::leagueTask) {
            invalidArgs = "Use as ::leaguetask index [points]"
        }
        onAdminCommand("leaguereset", "Reset Leagues VI testing progress.", ::leagueReset)
        onAdminCommand("leaguepacts", "Open the Demonic Pacts interface.", ::leaguePacts)
        onAdminCommand("leaguepactpoints", "Set or add Demonic Pact points.", ::leaguePactPoints) {
            invalidArgs = "Use as ::leaguepactpoints set|add amount"
        }
        onAdminCommand("leaguepactresets", "Set or add Demonic Pact resets.", ::leaguePactResets) {
            invalidArgs = "Use as ::leaguepactresets set|add amount"
        }
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

    private fun leagueFind(cheat: Cheat) =
        with(cheat) {
            val query = args.joinToString(" ")
            val matches = LeagueTaskCatalog.find(query, limit = 8)
            if (matches.isEmpty()) {
                player.mes("No League tasks found for '$query'.")
                return@with
            }
            matches.forEach { task ->
                player.mes(
                    "#${task.index}: ${task.name} " +
                        "(${task.points} pts, diff=${task.difficulty ?: "-"}, row=${task.rowId ?: "-"})",
                )
            }
        }

    private fun leagueTask(cheat: Cheat) =
        with(cheat) {
            val index = args[0].toInt()
            val points = args.getOrNull(1)?.toInt()
            if (leagueTasks.completeByIndex(player, index, points)) {
                player.mes("Marked League task $index complete.")
            } else {
                player.mes("League task $index was already complete.")
            }
        }

    private fun leagueReset(cheat: Cheat) =
        with(cheat) {
            val mode = args.getOrNull(0)?.lowercase() ?: "all"
            val profile =
                when (mode) {
                    "all" -> leagues.resetProgress(player)
                    "tasks" ->
                        leagues.resetProgress(
                            player,
                            resetTasks = true,
                            resetRelics = false,
                            resetPacts = false,
                            resetRegions = false,
                        )
                    "relics" ->
                        leagues.resetProgress(
                            player,
                            resetTasks = false,
                            resetRelics = true,
                            resetPacts = false,
                            resetRegions = false,
                        )
                    "pacts" ->
                        leagues.resetProgress(
                            player,
                            resetTasks = false,
                            resetRelics = false,
                            resetPacts = true,
                            resetRegions = false,
                        )
                    else -> error("Invalid reset mode: $mode")
                }
            player.ifCloseModals(eventBus)
            player.mes(
                "<col=ff66cc>League progress reset: $mode. " +
                    "Points=${profile.leaguePoints}, tasks=${profile.completedTaskIds.size}, " +
                    "relics=${profile.selectedRelics.size}, pacts=${profile.unlockedPactIds.size}.",
            )
        }

    private fun leaguePacts(cheat: Cheat) =
        with(cheat) {
            player.mes("Opening Demonic Pacts.")
            player.openTalentTree()
        }

    private fun leaguePactPoints(cheat: Cheat) =
        with(cheat) {
            val mode = args[0].lowercase()
            val amount = args[1].toInt()
            val profile =
                when (mode) {
                    "set" -> leagues.setPactPointsEarned(player, amount)
                    "add" -> leagues.addPactPoints(player, amount)
                    else -> error("Invalid mode: $mode")
                }
            player.mes("Pact points set to ${profile.pactPointsEarned}.")
        }

    private fun leaguePactResets(cheat: Cheat) =
        with(cheat) {
            val mode = args[0].lowercase()
            val amount = args[1].toInt()
            val profile =
                when (mode) {
                    "set" -> leagues.setPactResetsAvailable(player, amount)
                    "add" -> leagues.addPactResets(player, amount)
                    else -> error("Invalid mode: $mode")
                }
            player.mes("Pact resets available: ${profile.pactResetsAvailable}.")
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

    private fun Player.enableLeagueModalEvents(menuFrame: String, closeButton: String) {
        ifSetEvents(menuFrame, LEAGUE_MODAL_MENU_SUBS, IfEvent.Op1)
        ifSetEvents(closeButton, 0..0, IfEvent.Op1)
    }

    private fun Player.enableLeagueSidePanelEvents() {
        LEAGUE_SIDE_PANEL_CLICK_COMPONENTS.forEach { component ->
            ifSetEvents(component, -1..-1, IfEvent.Op1)
            ifSetEvents(component, 0..10, IfEvent.Op1)
        }
    }

    private fun Player.enableLeagueTaskEvents() {
        LEAGUE_TASK_FILTER_COMPONENTS.forEach { component ->
            ifSetEvents(component, -1..-1, IfEvent.Op1)
            ifSetEvents(component, 0..LEAGUE_TASK_FILTER_MAX_SUB, IfEvent.Op1)
        }
        LEAGUE_TASK_DROPDOWN_COMPONENTS.forEach { component ->
            ifSetEvents(component, 0..LEAGUE_TASK_DROPDOWN_MAX_SUB, IfEvent.Op1)
        }
        LEAGUE_TASK_ROW_COMPONENTS.forEach { component ->
            ifSetEvents(component, 0..LEAGUE_TASK_ROW_MAX_SUB, IfEvent.Op1)
        }
        ifSetEvents("component.league_tasks:searchbar_image", -1..-1, IfEvent.Op1)
        ifSetEvents("component.league_tasks:searchbar_image", 0..0, IfEvent.Op1)
        ifSetEvents(
            "component.league_tasks:infinity",
            0..LEAGUE_TASK_RESIZE_HANDLE_MAX_SUB,
            IfEvent.Depth1,
            IfEvent.DragTarget,
        )
    }

    private fun Player.enableLeagueRelicEvents() {
        ifSetEvents(
            "component.league_relics:clickzones",
            0..LEAGUE_RELIC_CLICKZONE_MAX_SUB,
            IfEvent.Op1,
        )
        ifSetEvents(
            "component.league_relics:confirm_button",
            -1..LEAGUE_RELIC_BUTTON_MAX_SUB,
            IfEvent.Op1,
        )
        ifSetEvents(
            "component.league_relics:select_back",
            -1..LEAGUE_RELIC_BUTTON_MAX_SUB,
            IfEvent.Op1,
        )
        ifSetEvents(
            "component.league_relics:toggles",
            0..LEAGUE_RELIC_TOGGLE_MAX_SUB,
            IfEvent.Op1,
            IfEvent.ScriptTrigger,
        )
        ifSetEvents(
            "component.league_relics:toggles",
            -1..-1,
            IfEvent.ScriptTrigger,
        )
    }

    private fun Player.enableLeagueAreaEvents() {
        LEAGUE_AREA_MAP_COMPONENTS.forEach { component ->
            ifSetEvents(component, -1..-1, IfEvent.Op1)
            ifSetEvents(component, 0..0, IfEvent.Op1)
        }
        LEAGUE_AREA_RAW_BUTTON_COMPONENTS.forEach { component ->
            ifSetEvents(component, -1..LEAGUE_AREA_BUTTON_MAX_SUB, IfEvent.Op1)
        }
    }

    private fun Player.enableTalentTreeEvents() {
        ifSetEvents(
            "component.talent_tree:tree_node_draw_layer",
            0..PACT_NODE_MAX_ID,
            IfEvent.Op1,
            IfEvent.Op2,
        )
        ifSetEvents("component.talent_tree:floating_panel_content", 0..20, IfEvent.Op1)
        ifSetEvents("component.talent_tree:perm_floating_panel_content", 0..20, IfEvent.Op1)
        ifSetEvents("component.talent_tree:data_layer", -1..-1, IfEvent.ScriptTrigger)
        ifSetEvents(
            "component.talent_tree:perm_floating_panel_content",
            -1..-1,
            IfEvent.ScriptTrigger,
        )
    }

    private fun Player.openLeagueMenuEntry(comsub: Int) {
        if (comsub == 17 || comsub == 18) {
            ifCloseModals(eventBus)
            openTalentTree()
            return
        }
        val modal =
            when (comsub) {
                9, 10 -> "interface.league_info"
                11, 12 -> "interface.league_tasks"
                13, 14 -> "interface.trailblazer_areas"
                15, 16 -> "interface.league_relics"
                else -> return
            }
        ifOpenMainModal(modal, eventBus)
    }

    private fun Player.openTalentTree() {
        LeagueClientState.sync(this)
        ifCloseOverlay("interface.talent_tree", eventBus)
        ifOpenMainModal("interface.talent_tree", eventBus)
        runClientScript(TALENT_TREE_INIT_SCRIPT)
    }

    private fun Player.applyPendingPacts(nodeIds: IntArray) {
        val pendingNodes =
            nodeIds
                .asSequence()
                .filter { it in 0..PACT_NODE_MAX_ID }
                .distinct()
                .filterNot { "node_$it" in leagueProfile.unlockedPactIds }
                .toList()
        if (pendingNodes.isEmpty()) {
            LeagueClientState.sync(this)
            runClientScript(TALENT_TREE_INIT_SCRIPT)
            return
        }
        val available = leagueProfile.pactPointsAvailable
        if (pendingNodes.size > available) {
            mes("<col=ff0000>You do not have enough pact points for those changes.")
            LeagueClientState.sync(this)
            runClientScript(TALENT_TREE_INIT_SCRIPT)
            return
        }
        var applied = 0
        pendingNodes.forEach { nodeId ->
            if (leagues.unlockPact(this, "node_$nodeId")) {
                applied++
            }
        }
        if (applied > 0) {
            mes("Applied $applied pact change${if (applied == 1) "" else "s"}.")
        }
        LeagueClientState.sync(this)
        runClientScript(TALENT_TREE_INIT_SCRIPT)
    }

    private fun Player.resetPactsFromInterface() {
        if (leagues.resetPacts(this)) {
            mes("Your pacts have been reset.")
        } else {
            mes("<col=ff0000>You do not have a pact reset available.")
        }
        LeagueClientState.sync(this)
        runClientScript(TALENT_TREE_INIT_SCRIPT)
    }

    private fun Player.redrawLeagueTasks() {
        runClientScript(LEAGUE_TASKS_DRAW_LIST_SCRIPT, LEAGUE_TASKS_DRAW_LIST_ARGS)
    }

    private fun Player.openLeagueRelicDetails(comsub: Int) {
        val choice = leagueRelicChoices().getOrNull(comsub)
        if (choice == null) {
            runClientScript(LEAGUE_INTERFACE_STOP_LOADING_SCRIPT, LEAGUE_RELIC_LOADING_COMPONENT)
            return
        }

        pendingLeagueRelic = choice
        runClientScript(
            LEAGUE_RELIC_EXPANDED_VIEW_SCRIPT,
            leagueRelicExpandedViewArgs(choice, choice.statusFor(leagueProfile)),
        )
        ifSetHide("component.league_relics:view_all_scrollbar", true)
    }

    private fun Player.closeLeagueRelicDetails() {
        pendingLeagueRelic = null
        runClientScript(LEAGUE_RELICS_INIT_SCRIPT, LEAGUE_RELICS_INIT_ARGS)
    }

    private fun Player.redrawPendingLeagueRelicDetails() {
        val choice = pendingLeagueRelic ?: return
        runClientScript(
            LEAGUE_RELIC_EXPANDED_VIEW_SCRIPT,
            leagueRelicExpandedViewArgs(choice, choice.statusFor(leagueProfile)),
        )
        ifSetHide("component.league_relics:view_all_scrollbar", true)
    }

    private fun Player.confirmPendingLeagueRelic() {
        val choice = pendingLeagueRelic ?: return
        val status = choice.statusFor(leagueProfile)
        if (status != RELIC_STATUS_SELECTABLE && status != RELIC_STATUS_REPICK_SELECTABLE) {
            runClientScript(LEAGUE_RELICS_INIT_SCRIPT, LEAGUE_RELICS_INIT_ARGS)
            return
        }

        val selected = leagues.selectRelic(this, choice.tier, "choice_${choice.choice}")
        if (selected) {
            pendingLeagueRelic = null
            mes("You have unlocked the ${choice.name} relic.")
        }
        runClientScript(LEAGUE_RELICS_INIT_SCRIPT, LEAGUE_RELICS_INIT_ARGS)
    }

    private fun Player.openLeagueAreaDetails(choice: LeagueAreaChoice) {
        pendingLeagueArea = choice
        LeagueClientState.setViewedArea(this, choice.areaId)
        runClientScript(
            LEAGUE_AREAS_SHOW_DETAILED_SCRIPT,
            leagueAreaDetailedViewArgs(choice, choice.statusFor(leagueProfile)),
        )
    }

    private fun Player.closeLeagueAreaDetails() {
        pendingLeagueArea = null
        reopenLeagueAreaMap()
    }

    private suspend fun ProtectedAccess.confirmPendingLeagueArea() {
        val choice = player.pendingLeagueArea ?: return
        when (choice.statusFor(player.leagueProfile)) {
            LEAGUE_AREA_STATUS_UNLOCKABLE -> {
                val confirmed =
                    choice2(
                        "Yes, unlock ${choice.name}.",
                        true,
                        "No, keep choosing.",
                        false,
                        title = "Unlock ${choice.name}?",
                    )
                if (!confirmed || player.pendingLeagueArea != choice) {
                    return
                }
                player.unlockPendingLeagueArea(choice)
            }
            LEAGUE_AREA_STATUS_UNLOCKED -> player.mes("Area teleporting is not available yet.")
            LEAGUE_AREA_STATUS_MAXED -> player.mes("<col=ff0000>You cannot unlock any more areas.")
            else -> player.mes(choice.lockedMessage(player.leagueProfile))
        }
    }

    private fun Player.unlockPendingLeagueArea(choice: LeagueAreaChoice) {
        if (leagues.unlockRegion(this, choice.regionId)) {
            mes("You have unlocked the ${choice.name} area.")
        }
        pendingLeagueArea = null
        LeagueClientState.sync(this)
        reopenLeagueAreaMap()
    }

    private fun Player.reopenLeagueAreaMap() {
        ifCloseModals(eventBus)
        ifOpenMainModal("interface.trailblazer_areas", eventBus)
    }

    private fun LeagueAreaChoice.statusFor(profile: LeagueProfile): Int {
        if (!profile.enabled) {
            return LEAGUE_AREA_STATUS_LOCKED
        }
        if (regionId in profile.unlockedRegionIds) {
            return LEAGUE_AREA_STATUS_UNLOCKED
        }
        if (profile.unlockedRegionIds.size >= LEAGUE_AREA_MAX_UNLOCKS) {
            return LEAGUE_AREA_STATUS_MAXED
        }
        return if (areaId == nextForcedLeagueArea(profile)?.areaId &&
            profile.completedTaskIds.size >= requiredTasks
        ) {
            LEAGUE_AREA_STATUS_UNLOCKABLE
        } else {
            LEAGUE_AREA_STATUS_LOCKED
        }
    }

    private fun LeagueAreaChoice.lockedMessage(profile: LeagueProfile): String {
        val nextArea = nextForcedLeagueArea(profile)
        if (this == nextArea && profile.completedTaskIds.size < requiredTasks) {
            return "You need to complete $requiredTasks League tasks to unlock $name."
        }
        return "<col=ff0000>You cannot unlock this area yet."
    }

    private fun nextForcedLeagueArea(profile: LeagueProfile): LeagueAreaChoice? =
        LEAGUE_6_FORCED_AREA_ORDER.firstOrNull { it.regionId !in profile.unlockedRegionIds }

    private fun leagueAreaDetailedViewArgs(choice: LeagueAreaChoice, status: Int): List<Any> =
        listOf(choice.areaId) +
            LEAGUE_AREAS_SHOW_DETAILED_PRIMARY_COMPONENTS +
            listOf(status) +
            LEAGUE_AREAS_SHOW_DETAILED_CONFIRM_COMPONENTS +
            listOf("")

    private fun LeagueRelicChoice.statusFor(profile: LeagueProfile): Int {
        if (!profile.enabled) {
            return RELIC_STATUS_LOCKED
        }

        val selectedChoice = profile.selectedRelics[tier]?.toRelicChoice()
        if (selectedChoice == choice) {
            return RELIC_STATUS_UNLOCKED
        }
        if (selectedChoice != null) {
            return RELIC_STATUS_TIER_SELECTED
        }

        val nextTier = nextRelicTier(profile) ?: return RELIC_STATUS_LOCKED
        if (tier > nextTier) {
            return RELIC_STATUS_PREVIOUS_REQUIRED
        }
        if (tier < nextTier) {
            return RELIC_STATUS_TIER_SELECTED
        }
        return if (profile.leaguePoints >= requiredPoints) {
            RELIC_STATUS_SELECTABLE
        } else {
            RELIC_STATUS_LOCKED
        }
    }

    private fun nextRelicTier(profile: LeagueProfile): Int? =
        leagueRelicTiers().firstOrNull { it.tier !in profile.selectedRelics }?.tier

    private fun leagueRelicExpandedViewArgs(choice: LeagueRelicChoice, status: Int): List<Any> =
        LEAGUE_RELIC_EXPANDED_VIEW_COMPONENTS +
            listOf(
                status,
                choice.structId,
                -1,
                choice.description,
                "",
            )

    private fun leagueRelicChoices(): List<LeagueRelicChoice> =
        leagueRelicTiers().flatMap { tier ->
            val choicesEnum = ServerCacheManager.getEnum(tier.choicesEnumId) ?: return@flatMap emptyList()
            choicesEnum.values.keys
                .filterIsInstance<Int>()
                .sorted()
                .mapNotNull { choice ->
                    val structId = choicesEnum.values[choice] as? Int ?: return@mapNotNull null
                    val struct = ServerCacheManager.getStruct(structId) ?: return@mapNotNull null
                    LeagueRelicChoice(
                        tierIndex = tier.tierIndex,
                        tier = tier.tier,
                        choice = choice,
                        structId = structId,
                        name = struct.stringParam(PARAM_RELIC_NAME),
                        description = struct.stringParam(PARAM_RELIC_DESCRIPTION),
                        requiredPoints = tier.requiredPoints,
                    )
                }
        }

    private fun leagueRelicTiers(): List<LeagueRelicTier> {
        val leagueStruct = currentLeagueStruct() ?: return emptyList()
        val tiersEnumId = leagueStruct.intParam(PARAM_LEAGUE_RELIC_TIERS_ENUM) ?: return emptyList()
        val tiersEnum = ServerCacheManager.getEnum(tiersEnumId) ?: return emptyList()
        return tiersEnum.values.keys
            .filterIsInstance<Int>()
            .sorted()
            .mapNotNull { tierIndex ->
                val tierStructId = tiersEnum.values[tierIndex] as? Int ?: return@mapNotNull null
                val tierStruct = ServerCacheManager.getStruct(tierStructId) ?: return@mapNotNull null
                val choicesEnumId = tierStruct.intParam(PARAM_RELIC_TIER_CHOICES_ENUM)
                    ?: return@mapNotNull null
                LeagueRelicTier(
                    tierIndex = tierIndex,
                    tier = tierIndex + 1,
                    requiredPoints = tierStruct.intParam(PARAM_RELIC_TIER_POINTS) ?: 0,
                    choicesEnumId = choicesEnumId,
                )
            }
    }

    private fun currentLeagueStruct(): StructType? {
        val leagueStructId =
            ServerCacheManager.getEnum(LEAGUE_TYPE_ENUM_ID)
                ?.values
                ?.get(LeagueConstants.CURRENT_LEAGUE_TYPE) as? Int
                ?: return null
        return ServerCacheManager.getStruct(leagueStructId)
    }

    private fun StructType.intParam(id: Int): Int? = params?.get(id) as? Int

    private fun StructType.stringParam(id: Int): String = params?.get(id) as? String ?: ""

    private fun String.toRelicChoice(): Int? =
        removePrefix("relic_")
            .removePrefix("choice_")
            .toIntOrNull()
            ?.takeIf { it > 0 }

    private fun Player.showLocalLeagueRank() {
        val profile = leagueProfile
        mes("Local Leagues ranking is not live yet.")
        mes(
            "Server-only progress: ${profile.leaguePoints} points, " +
                "${profile.completedTaskIds.size} tasks, ${profile.unlockedRegionIds.size} areas.",
        )
    }

    private companion object {
        val LEAGUE_MODAL_MENU_SUBS = 9..18
        const val LEAGUE_STEELBORDER_CLOSE_SUB = 11
        const val TALENT_TREE_INIT_SCRIPT = 7892
        const val PACT_NODE_MAX_ID = 191
        const val LEAGUE_TASK_FILTER_MAX_SUB = 8
        const val LEAGUE_TASK_DROPDOWN_MAX_SUB = 256
        const val LEAGUE_TASK_ROW_MAX_SUB = 2048
        const val LEAGUE_TASK_RESIZE_HANDLE_MAX_SUB = 8
        const val LEAGUE_RELIC_CLICKZONE_MAX_SUB = 64
        const val LEAGUE_RELIC_BUTTON_MAX_SUB = 8
        const val LEAGUE_RELIC_TOGGLE_MAX_SUB = 128
        const val LEAGUE_AREA_BUTTON_MAX_SUB = 128
        const val LEAGUE_TYPE_ENUM_ID = 2670
        const val PARAM_LEAGUE_RELIC_TIERS_ENUM = 870
        const val PARAM_RELIC_TIER_POINTS = 877
        const val PARAM_RELIC_TIER_CHOICES_ENUM = 878
        const val PARAM_RELIC_NAME = 879
        const val PARAM_RELIC_DESCRIPTION = 880
        const val LEAGUE_RELICS_INIT_SCRIPT = 3188
        const val LEAGUE_RELIC_EXPANDED_VIEW_SCRIPT = 3193
        const val LEAGUE_INTERFACE_STOP_LOADING_SCRIPT = 7745
        const val RELIC_STATUS_LOCKED = 0
        const val RELIC_STATUS_UNLOCKED = 1
        const val RELIC_STATUS_SELECTABLE = 2
        const val RELIC_STATUS_PREVIOUS_REQUIRED = 3
        const val RELIC_STATUS_TIER_SELECTED = 4
        const val RELIC_STATUS_REPICK_SELECTABLE = 5
        const val LEAGUE_AREAS_SHOW_DETAILED_SCRIPT = 3668
        const val LEAGUE_AREA_STATUS_LOCKED = 0
        const val LEAGUE_AREA_STATUS_UNLOCKED = 1
        const val LEAGUE_AREA_STATUS_UNLOCKABLE = 2
        const val LEAGUE_AREA_STATUS_MAXED = 4
        const val LEAGUE_AREA_MAX_UNLOCKS = 5
        const val LEAGUE_AREA_SELECT_BUTTON_ID = 84
        const val LEAGUE_AREA_SELECT_BACK_ID = 85

        val MISHTHALIN_AREA = LeagueAreaChoice(1, "misthalin", "Misthalin")
        val KARAMJA_AREA = LeagueAreaChoice(2, "karamja", "Karamja", requiredTasks = 80)
        val ASGARNIA_AREA = LeagueAreaChoice(3, "asgarnia", "Asgarnia")
        val KANDARIN_AREA = LeagueAreaChoice(4, "kandarin", "Kandarin")
        val MORYTANIA_AREA = LeagueAreaChoice(5, "morytania", "Morytania")
        val DESERT_AREA = LeagueAreaChoice(6, "desert", "Desert")
        val TIRANNWN_AREA = LeagueAreaChoice(7, "tirannwn", "Tirannwn")
        val FREMENNIK_AREA = LeagueAreaChoice(8, "fremennik", "Fremennik")
        val WILDERNESS_AREA = LeagueAreaChoice(11, "wilderness", "Wilderness")
        val KOUREND_AREA = LeagueAreaChoice(20, "kebos_kourend", "Kebos & Kourend")
        val VARLAMORE_AREA = LeagueAreaChoice(21, "varlamore", "Varlamore")

        val LEAGUE_6_FORCED_AREA_ORDER =
            listOf(
                VARLAMORE_AREA,
                KARAMJA_AREA,
            )

        val LEAGUE_AREA_COMPONENT_CHOICES =
            linkedMapOf(
                "component.trailblazer_areas:league_shield_misthalin" to MISHTHALIN_AREA,
                "component.trailblazer_areas:league_name_misthalin" to MISHTHALIN_AREA,
                "component.trailblazer_areas:league_shield_karamja" to KARAMJA_AREA,
                "component.trailblazer_areas:league_name_karamja" to KARAMJA_AREA,
                "component.trailblazer_areas:league_shield_desert" to DESERT_AREA,
                "component.trailblazer_areas:league_name_desert" to DESERT_AREA,
                "component.trailblazer_areas:league_shield_morytania" to MORYTANIA_AREA,
                "component.trailblazer_areas:league_name_morytania" to MORYTANIA_AREA,
                "component.trailblazer_areas:league_shield_asgarnia" to ASGARNIA_AREA,
                "component.trailblazer_areas:league_name_asgarnia" to ASGARNIA_AREA,
                "component.trailblazer_areas:league_shield_kandarin" to KANDARIN_AREA,
                "component.trailblazer_areas:league_name_kandarin" to KANDARIN_AREA,
                "component.trailblazer_areas:league_shield_fremennik" to FREMENNIK_AREA,
                "component.trailblazer_areas:league_name_fremennik" to FREMENNIK_AREA,
                "component.trailblazer_areas:league_shield_tirannwn" to TIRANNWN_AREA,
                "component.trailblazer_areas:league_name_tirannwn" to TIRANNWN_AREA,
                "component.trailblazer_areas:league_shield_wilderness" to WILDERNESS_AREA,
                "component.trailblazer_areas:league_name_wilderness" to WILDERNESS_AREA,
                "component.trailblazer_areas:league_shield_kebos_kourend" to KOUREND_AREA,
                "component.trailblazer_areas:league_name_kebos_kourend" to KOUREND_AREA,
                "component.trailblazer_areas:league_shield_varlamore" to VARLAMORE_AREA,
                "component.trailblazer_areas:league_name_varlamore" to VARLAMORE_AREA,
            )

        val LEAGUE_AREA_MAP_COMPONENTS = LEAGUE_AREA_COMPONENT_CHOICES.keys.toList()

        val LEAGUE_AREA_SELECT_BUTTON_COMPONENT =
            trailblazerAreaComponent(LEAGUE_AREA_SELECT_BUTTON_ID)

        val LEAGUE_AREA_SELECT_BACK_COMPONENT =
            trailblazerAreaComponent(LEAGUE_AREA_SELECT_BACK_ID)

        val LEAGUE_AREA_RAW_BUTTON_COMPONENTS =
            listOf(
                LEAGUE_AREA_SELECT_BUTTON_COMPONENT,
                LEAGUE_AREA_SELECT_BACK_COMPONENT,
            )

        val LEAGUE_AREAS_SHOW_DETAILED_PRIMARY_COMPONENTS =
            listOf(
                "component.trailblazer_areas:league_maps".asRSCM(RSCMType.COMPONENT),
                "component.trailblazer_areas:league_shields".asRSCM(RSCMType.COMPONENT),
                "component.trailblazer_areas:league_names".asRSCM(RSCMType.COMPONENT),
                "component.trailblazer_areas:league_area_details".asRSCM(RSCMType.COMPONENT),
                "component.trailblazer_areas:name_shield".asRSCM(RSCMType.COMPONENT),
                "component.trailblazer_areas:name_header".asRSCM(RSCMType.COMPONENT),
                "component.trailblazer_areas:description".asRSCM(RSCMType.COMPONENT),
                LEAGUE_AREA_SELECT_BUTTON_COMPONENT.packed,
                LEAGUE_AREA_SELECT_BACK_COMPONENT.packed,
                "component.trailblazer_areas:league_area_icon_content".asRSCM(RSCMType.COMPONENT),
            )

        val LEAGUE_AREAS_SHOW_DETAILED_CONFIRM_COMPONENTS =
            listOf(
                "component.trailblazer_areas:league_tab_layer".asRSCM(RSCMType.COMPONENT),
                "component.trailblazer_areas:league_tab_infolayer".asRSCM(RSCMType.COMPONENT),
                "component.trailblazer_areas:confirm_overlay".asRSCM(RSCMType.COMPONENT),
                "component.trailblazer_areas:confirm_universe".asRSCM(RSCMType.COMPONENT),
                "component.trailblazer_areas:confirm".asRSCM(RSCMType.COMPONENT),
                "component.trailblazer_areas:confirm_frame".asRSCM(RSCMType.COMPONENT),
                "component.trailblazer_areas:confirm_text".asRSCM(RSCMType.COMPONENT),
                "component.trailblazer_areas:confirm_button".asRSCM(RSCMType.COMPONENT),
                "component.trailblazer_areas:confirm_cancel".asRSCM(RSCMType.COMPONENT),
                "component.trailblazer_areas:loading_overlay".asRSCM(RSCMType.COMPONENT),
                "component.trailblazer_areas:league_area_icon".asRSCM(RSCMType.COMPONENT),
            )

        val LEAGUE_RELIC_LOADING_COMPONENT =
            "component.league_relics:loading".asRSCM(RSCMType.COMPONENT)

        private fun trailblazerAreaComponent(child: Int) =
            ServerCacheManager.fromComponent(Component(LeagueInterfaces.TRAILBLAZER_AREAS, child).packed)

        val LEAGUE_RELICS_INIT_ARGS =
            listOf(
                "component.league_relics:header".asRSCM(RSCMType.COMPONENT),
                "component.league_relics:relic_available_header".asRSCM(RSCMType.COMPONENT),
                "component.league_relics:backgrounds".asRSCM(RSCMType.COMPONENT),
                "component.league_relics:icons".asRSCM(RSCMType.COMPONENT),
                "component.league_relics:outlines".asRSCM(RSCMType.COMPONENT),
                "component.league_relics:names".asRSCM(RSCMType.COMPONENT),
                "component.league_relics:clickzones".asRSCM(RSCMType.COMPONENT),
                "component.league_relics:view_all".asRSCM(RSCMType.COMPONENT),
                "component.league_relics:view_all_scrollbar".asRSCM(RSCMType.COMPONENT),
                "component.league_relics:view_one".asRSCM(RSCMType.COMPONENT),
                "component.league_relics:confirm".asRSCM(RSCMType.COMPONENT),
                "component.league_relics:loading".asRSCM(RSCMType.COMPONENT),
                "component.league_relics:league_btn_menu".asRSCM(RSCMType.COMPONENT),
                "component.league_relics:league_menu_frame".asRSCM(RSCMType.COMPONENT),
                "component.league_relics:league_menu_overlay".asRSCM(RSCMType.COMPONENT),
                "component.league_relics:progress_bar".asRSCM(RSCMType.COMPONENT),
                "component.league_relics:tooltip".asRSCM(RSCMType.COMPONENT),
                "component.league_relics:close_button".asRSCM(RSCMType.COMPONENT),
            )

        val LEAGUE_RELIC_EXPANDED_VIEW_COMPONENTS =
            listOf(
                "component.league_relics:header".asRSCM(RSCMType.COMPONENT),
                "component.league_relics:relic_available_header".asRSCM(RSCMType.COMPONENT),
                "component.league_relics:view_one".asRSCM(RSCMType.COMPONENT),
                "component.league_relics:view_all".asRSCM(RSCMType.COMPONENT),
                "component.league_relics:icon".asRSCM(RSCMType.COMPONENT),
                "component.league_relics:name".asRSCM(RSCMType.COMPONENT),
                "component.league_relics:description_header".asRSCM(RSCMType.COMPONENT),
                "component.league_relics:description".asRSCM(RSCMType.COMPONENT),
                "component.league_relics:select_button".asRSCM(RSCMType.COMPONENT),
                "component.league_relics:select_back".asRSCM(RSCMType.COMPONENT),
                "component.league_relics:confirm".asRSCM(RSCMType.COMPONENT),
                "component.league_relics:confirm_frame".asRSCM(RSCMType.COMPONENT),
                "component.league_relics:confirm_text".asRSCM(RSCMType.COMPONENT),
                "component.league_relics:confirm_button".asRSCM(RSCMType.COMPONENT),
                "component.league_relics:confirm_cancel".asRSCM(RSCMType.COMPONENT),
                "component.league_relics:passive_header".asRSCM(RSCMType.COMPONENT),
                "component.league_relics:passive_description".asRSCM(RSCMType.COMPONENT),
                "component.league_relics:loading".asRSCM(RSCMType.COMPONENT),
            )

        val LEAGUE_MODAL_CLOSE_BUTTONS =
            listOf(
                "component.league_info:close_button",
                "component.league_tasks:close_button",
                "component.trailblazer_areas:close_button",
                "component.league_relics:close_button",
                "component.league_combat_mastery:close_button",
                "component.league_rankings:close_button",
            )

        val LEAGUE_MODAL_MENU_FRAMES =
            listOf(
                "component.league_info:league_menu_frame",
                "component.league_tasks:league_menu_frame",
                "component.trailblazer_areas:league_menu_frame",
                "component.league_relics:league_menu_frame",
                "component.league_combat_mastery:league_menu_frame",
            )

        val LEAGUE_TASK_FILTER_COMPONENTS =
            listOf(
                "component.league_tasks:tier_filter",
                "component.league_tasks:type_filter",
                "component.league_tasks:area_filter",
                "component.league_tasks:skill_filter",
                "component.league_tasks:mastery_filter",
                "component.league_tasks:completed_filter",
            )

        val LEAGUE_TASK_DROPDOWN_COMPONENTS =
            listOf(
                "component.league_tasks:dropdown_tier",
                "component.league_tasks:dropdown_type",
                "component.league_tasks:dropdown_area",
                "component.league_tasks:dropdown_skill",
                "component.league_tasks:dropdown_mastery",
                "component.league_tasks:dropdown_completed",
            )

        const val LEAGUE_TASKS_DRAW_LIST_SCRIPT = 3203

        val LEAGUE_TASKS_DRAW_LIST_ARGS =
            listOf(
                -1,
                43057168,
                "component.league_tasks:tasks_background".asRSCM(RSCMType.COMPONENT),
                "component.league_tasks:tasks_name".asRSCM(RSCMType.COMPONENT),
                "component.league_tasks:tasks_points".asRSCM(RSCMType.COMPONENT),
                "component.league_tasks:tasks_type".asRSCM(RSCMType.COMPONENT),
                "component.league_tasks:tasks_area".asRSCM(RSCMType.COMPONENT),
                "component.league_tasks:tasks_description".asRSCM(RSCMType.COMPONENT),
                "component.league_tasks:tasks_expand".asRSCM(RSCMType.COMPONENT),
                "component.league_tasks:tasks_tier".asRSCM(RSCMType.COMPONENT),
                43057178,
                43057167,
                "component.league_tasks:league_btn_menu".asRSCM(RSCMType.COMPONENT),
                "component.league_tasks:league_menu_frame".asRSCM(RSCMType.COMPONENT),
                "component.league_tasks:league_menu_overlay".asRSCM(RSCMType.COMPONENT),
                "component.league_tasks:progress_bar".asRSCM(RSCMType.COMPONENT),
                "component.league_tasks:tooltip".asRSCM(RSCMType.COMPONENT),
                "component.league_tasks:searchbar_image".asRSCM(RSCMType.COMPONENT),
                "component.league_tasks:search_text".asRSCM(RSCMType.COMPONENT),
                1,
            )

        val LEAGUE_TASK_ROW_COMPONENTS =
            listOf(
                "component.league_tasks:tasks_background",
                "component.league_tasks:tasks_expand",
                "component.league_tasks:tasks_name",
                "component.league_tasks:tasks_points",
                "component.league_tasks:tasks_type",
                "component.league_tasks:tasks_area",
                "component.league_tasks:tasks_description",
                "component.league_tasks:tasks_tier",
            )

        val LEAGUE_SIDE_PANEL_MASTERY_BUTTONS =
            listOf(
                "component.league_side_panel:mastery_button",
                "component.league_side_panel:mastery_button_outer",
                "component.league_side_panel:mastery_button_text",
                "component.league_side_panel:mastery_highlight",
            )

        val LEAGUE_SIDE_PANEL_RANK_BUTTONS =
            listOf(
                "component.league_side_panel:rank",
                "component.league_side_panel:rank_backing",
            )

        val LEAGUE_SIDE_PANEL_CLICK_COMPONENTS =
            listOf(
                "component.league_side_panel:info",
                "component.league_side_panel:rank",
                "component.league_side_panel:rank_backing",
                "component.league_side_panel:summary",
                "component.league_side_panel:summary_backing",
                "component.league_side_panel:tasks_button",
                "component.league_side_panel:tasks_button_outer",
                "component.league_side_panel:tasks_button_text",
                "component.league_side_panel:areas_button",
                "component.league_side_panel:areas_button_outer",
                "component.league_side_panel:areas_button_text",
                "component.league_side_panel:relics_button",
                "component.league_side_panel:relics_button_outer",
                "component.league_side_panel:relics_button_text",
                "component.league_side_panel:mastery_button",
                "component.league_side_panel:mastery_button_outer",
                "component.league_side_panel:mastery_button_text",
                "component.league_side_panel:mastery_highlight",
            )
    }
}
