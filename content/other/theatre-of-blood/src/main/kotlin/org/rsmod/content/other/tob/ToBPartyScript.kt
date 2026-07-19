package org.rsmod.content.other.tob

import jakarta.inject.Inject
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.player.output.MiscOutput
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.advanced.onOpPlayer5
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onPlayerCoordsChanged
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private const val TOB_INVITE_PLAYER_OP_SLOT = 5
private const val TOB_INVITE_PLAYER_OP = "Invite"
private const val VER_SINHAZA_MAP_SQUARE_Z = 50
private const val VER_SINHAZA_WEST_MAP_SQUARE_X = 56
private const val VER_SINHAZA_EAST_MAP_SQUARE_X = 57
private val TOB_INVITE_OPTION_VISIBLE_ATTR = AttributeKey<Boolean>(temp = true)

class ToBPartyScript
@Inject
constructor(
    private val eventBus: EventBus,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerLogin { player.resetToBInviteOptionState() }
        onPlayerCoordsChanged { player.refreshToBInviteOption() }
        onOpLoc1("loc.tob_surface_notice_board") { openNoticeBoard() }
        onOpPlayer5 { inviteToParty(it.target) }

        onIfOverlayButton("component.tob_partydetails:com_15") { player.formPartyFromButton() }
        onIfModalButton("component.tob_partydetails:com_15") { player.formPartyFromButton() }

        onIfOverlayButton("component.tob_partydetails:com_22") { player.leaveToBParty() }
        onIfModalButton("component.tob_partydetails:com_22") { player.leaveToBParty() }

        onPlayerLogout {
            player.tobParty?.removePlayer(player, eventBus, restoreQuestTab = false)
        }
    }

    private suspend fun ProtectedAccess.openNoticeBoard() {
        val party = player.tobParty
        if (party == null) {
            when (choice2("Form a new party.", NoticeBoardChoice.Form, "Cancel.", NoticeBoardChoice.Cancel)) {
                NoticeBoardChoice.Form -> ToBPartyManager.create(player, eventBus)
                NoticeBoardChoice.Cancel -> Unit
                else -> Unit
            }
            return
        }

        val action =
            if (party.isLeader(player)) {
                choice3(
                    "Show my party.",
                    NoticeBoardChoice.Show,
                    "Disband my party.",
                    NoticeBoardChoice.Disband,
                    "Cancel.",
                    NoticeBoardChoice.Cancel,
                )
            } else {
                choice3(
                    "Show my party.",
                    NoticeBoardChoice.Show,
                    "Leave my party.",
                    NoticeBoardChoice.Leave,
                    "Cancel.",
                    NoticeBoardChoice.Cancel,
                )
            }

        when (action) {
            NoticeBoardChoice.Show -> {
                party.openFor(player, eventBus)
                mes("Your Theatre of Blood party is shown on the overlay.")
            }
            NoticeBoardChoice.Disband -> player.disbandToBParty()
            NoticeBoardChoice.Leave -> player.leaveToBParty()
            else -> Unit
        }
    }

    private suspend fun ProtectedAccess.inviteToParty(target: Player) {
        if (target === player) {
            mes("You cannot invite yourself.")
            return
        }

        val targetName = target.tobDisplayName()
        val party = player.tobParty
        if (party == null) {
            mes("Form a Theatre of Blood party at the notice board first.")
            return
        }
        if (!party.isLeader(player)) {
            mes("Only the Theatre of Blood party leader can invite players.")
            return
        }
        if (party.contains(target)) {
            mes("${target.tobDisplayName()} is already in your Theatre of Blood party.")
            return
        }
        if (target.tobParty != null) {
            mes("${target.tobDisplayName()} is already in a Theatre of Blood party.")
            return
        }
        if (party.isFull()) {
            mes("Your Theatre of Blood party is already full.")
            return
        }
        if (!player.isInToBInviteArea()) {
            mes("You need to be at Ver Sinhaza to invite players.")
            return
        }
        if (!target.isInToBInviteArea()) {
            mes("$targetName needs to be at Ver Sinhaza to join your Theatre of Blood party.")
            return
        }

        val inviter = player
        val inviterName = inviter.tobDisplayName()
        val launched =
            protectedAccess.launch(target, busyText = "$targetName is busy right now.") {
                val accepted =
                    choice2(
                        "Yes.",
                        true,
                        "No.",
                        false,
                        title = "$inviterName invites you to the ToB party, do you wish to join?",
                    )
                if (!accepted) {
                    inviter.mes("$targetName declined your Theatre of Blood party invitation.")
                    return@launch
                }

                val currentParty = inviter.tobParty
                if (currentParty !== party || !party.isLeader(inviter)) {
                    mes("That Theatre of Blood party invitation has expired.")
                    return@launch
                }
                if (player.tobParty != null) {
                    mes("You are already in a Theatre of Blood party.")
                    return@launch
                }
                if (party.isFull()) {
                    mes("That Theatre of Blood party is now full.")
                    return@launch
                }
                if (!inviter.isInToBInviteArea()) {
                    mes("That Theatre of Blood party invitation has expired.")
                    return@launch
                }
                if (!player.isInToBInviteArea()) {
                    mes("You need to be at Ver Sinhaza to accept that Theatre of Blood party invitation.")
                    return@launch
                }

                if (party.addPlayer(player, eventBus)) {
                    inviter.mes("$targetName joined your Theatre of Blood party.")
                }
            }

        if (launched) {
            mes("You invite $targetName to your Theatre of Blood party.")
        } else {
            mes("$targetName cannot respond to an invitation right now.")
        }
    }

    private fun Player.disbandToBParty() {
        val party = tobParty ?: return
        if (!party.isLeader(this)) {
            party.removePlayer(this, eventBus)
            return
        }

        val members = party.members.toList()
        members.forEach { member ->
            if (member !== this) {
                member.mes("Your Theatre of Blood party has been disbanded.")
            }
        }
        party.destroy(eventBus)
        mes("You disband your Theatre of Blood party.")
    }

    private fun Player.resetToBInviteOptionState() {
        attr[TOB_INVITE_OPTION_VISIBLE_ATTR] = false
    }

    private fun Player.refreshToBInviteOption() {
        val shouldShow = isInToBInviteArea()
        val visible = attr.getOrDefault(TOB_INVITE_OPTION_VISIBLE_ATTR, false)
        if (shouldShow == visible) {
            return
        }

        attr[TOB_INVITE_OPTION_VISIBLE_ATTR] = shouldShow
        val option = if (shouldShow) TOB_INVITE_PLAYER_OP else null
        MiscOutput.setPlayerOp(this, slot = TOB_INVITE_PLAYER_OP_SLOT, op = option)
    }

    private fun Player.formPartyFromButton() {
        ToBPartyManager.create(this, eventBus)
    }

    private fun Player.leaveToBParty() {
        val party = tobParty
        if (party == null) {
            clearToBPartyOverlay(eventBus, restoreQuestTab = true)
            return
        }
        party.removePlayer(this, eventBus)
    }

    private fun Player.tobDisplayName(): String = displayName.ifBlank { username }

    private fun Player.isInToBInviteArea(): Boolean {
        val coords = coords
        return coords.level == 0 &&
            coords.mz == VER_SINHAZA_MAP_SQUARE_Z &&
            coords.mx in VER_SINHAZA_WEST_MAP_SQUARE_X..VER_SINHAZA_EAST_MAP_SQUARE_X
    }

    private enum class NoticeBoardChoice {
        Form,
        Show,
        Leave,
        Disband,
        Cancel,
    }
}
