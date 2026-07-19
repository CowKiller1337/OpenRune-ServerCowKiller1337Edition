package org.rsmod.content.other.tob

import dev.openrune.ServerCacheManager
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.ui.ifCloseSub
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.ui.ifSetText
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player

private const val MAX_PARTY_CAPACITY = 5
private const val TOB_MEMBERS_INTERFACE = "interface.tob_hud"
private const val TOB_MEMBERS_TEXT = "component.tob_hud:names"
private const val QUEST_LIST_INTERFACE = "interface.questlist"
private const val JOURNAL_TAB_CONTAINER = "component.side_journal:tab_container"
private const val TOB_PARTY_STATE_VARBIT = 6440
private const val TOB_HUD_STATE_VARBIT = 6441
private const val TOB_PARTY_LEADER_VARP = 1740
private const val TOB_STATE_NO_PARTY = 0
private const val TOB_STATE_IN_PARTY = 1
private const val TOB_HUD_NAMES = 0
private const val TOB_NO_LEADER = -1

private val TOB_PARTY_ATTR = AttributeKey<ToBParty>(temp = true)

var Player.tobParty: ToBParty?
    get() = attr[TOB_PARTY_ATTR]
    set(value) {
        if (value == null) {
            attr.remove(TOB_PARTY_ATTR)
        } else {
            attr[TOB_PARTY_ATTR] = value
        }
    }

data class ToBParty(
    var leader: Player,
    val members: MutableList<Player> = mutableListOf(),
    val maxCapacity: Int = MAX_PARTY_CAPACITY,
) {
    init {
        require(maxCapacity in 1..MAX_PARTY_CAPACITY) {
            "ToB party capacity must be within 1..$MAX_PARTY_CAPACITY."
        }
        if (members.none { it.sameCharacter(leader) }) {
            members.add(0, leader)
        }
        check(members.size <= maxCapacity) {
            "ToB party cannot start with ${members.size} members."
        }
        members.forEach { it.tobParty = this }
    }

    fun addPlayer(player: Player, eventBus: EventBus): Boolean {
        if (contains(player)) {
            player.openToBPartyOverlay(eventBus)
            player.mes("You are already in this party.")
            return false
        }
        if (isFull()) {
            player.mes("That Theatre of Blood party is full.")
            return false
        }
        if (player.tobParty != null) {
            player.mes("You are already in a Theatre of Blood party.")
            return false
        }

        members.add(player)
        player.tobParty = this
        player.openToBPartyOverlay(eventBus)
        player.mes("You join ${leader.partyDisplayName()}'s Theatre of Blood party.")
        refreshAll(eventBus)
        return true
    }

    fun removePlayer(player: Player, eventBus: EventBus, restoreQuestTab: Boolean = true): Boolean {
        val removed = members.removeAll { it.sameCharacter(player) }
        if (!removed) {
            return false
        }

        player.tobParty = null
        player.applyToBPartyVars(null)
        if (restoreQuestTab) {
            player.clearToBPartyOverlay(eventBus, restoreQuestTab = true)
        }

        if (members.isEmpty()) {
            ToBPartyManager.unregister(this)
            return true
        }

        if (leader.sameCharacter(player)) {
            leader = members.first()
            leader.mes("You are now the Theatre of Blood party leader.")
        }

        refreshAll(eventBus)
        return true
    }

    fun destroy(eventBus: EventBus, restoreQuestTabs: Boolean = true) {
        ToBPartyManager.unregister(this)
        val previousMembers = members.toList()
        members.clear()
        previousMembers.forEach { member ->
            member.tobParty = null
            member.applyToBPartyVars(null)
            member.clearToBPartyOverlay(eventBus, restoreQuestTabs)
        }
    }

    fun refreshAll(eventBus: EventBus) {
        members.toList().forEach { member ->
            member.tobParty = this
            member.applyToBPartyVars(this)
            member.openToBPartyOverlay(eventBus)
            member.refreshToBPartyOverlay()
        }
    }

    fun contains(player: Player): Boolean = members.any { it.sameCharacter(player) }

    fun isLeader(player: Player): Boolean = leader.sameCharacter(player)

    fun isFull(): Boolean = members.size >= maxCapacity

    fun slotText(index: Int): String =
        members.getOrNull(index)?.let { member ->
            if (isLeader(member)) {
                "${member.partyDisplayName()} *"
            } else {
                member.partyDisplayName()
            }
        } ?: "-"
}

object ToBPartyManager {
    private val parties = linkedSetOf<ToBParty>()

    fun create(leader: Player, eventBus: EventBus): ToBParty? =
        synchronized(parties) {
            if (leader.tobParty != null) {
                leader.mes("You are already in a Theatre of Blood party.")
                leader.tobParty?.openFor(leader, eventBus)
                return@synchronized null
            }
            val party = ToBParty(leader = leader)
            parties.add(party)
            party.refreshAll(eventBus)
            leader.mes("You form a Theatre of Blood party.")
            party
        }

    fun unregister(party: ToBParty) {
        synchronized(parties) {
            parties.remove(party)
        }
    }

    fun activePartyCount(): Int = synchronized(parties) { parties.size }
}

fun ToBParty.openFor(player: Player, eventBus: EventBus) {
    player.tobParty = this
    player.applyToBPartyVars(this)
    player.openToBPartyOverlay(eventBus)
    player.refreshToBPartyOverlay()
}

fun Player.openToBPartyOverlay(eventBus: EventBus) {
    ifOpenOverlay(TOB_MEMBERS_INTERFACE, eventBus)
    refreshToBPartyOverlay()
}

fun Player.refreshToBPartyOverlay() {
    val party = tobParty
    val text = (0 until MAX_PARTY_CAPACITY).joinToString("<br>") { index ->
        party?.slotText(index) ?: "-"
    }
    ifSetText(TOB_MEMBERS_TEXT, text)
}

fun Player.clearToBPartyOverlay(eventBus: EventBus, restoreQuestTab: Boolean) {
    ifSetText(TOB_MEMBERS_TEXT, List(MAX_PARTY_CAPACITY) { "-" }.joinToString("<br>"))
    ifCloseSub(TOB_MEMBERS_INTERFACE, eventBus)
    if (restoreQuestTab) {
        ifOpenOverlay(QUEST_LIST_INTERFACE, JOURNAL_TAB_CONTAINER, eventBus)
    }
}

private fun Player.applyToBPartyVars(party: ToBParty?) {
    ServerCacheManager.getVarbit(TOB_PARTY_STATE_VARBIT)?.let {
        VarPlayerIntMapSetter.set(this, it, if (party == null) TOB_STATE_NO_PARTY else TOB_STATE_IN_PARTY)
    }
    ServerCacheManager.getVarbit(TOB_HUD_STATE_VARBIT)?.let {
        VarPlayerIntMapSetter.set(this, it, TOB_HUD_NAMES)
    }
    ServerCacheManager.getVarp(TOB_PARTY_LEADER_VARP)?.let {
        VarPlayerIntMapSetter.set(this, it, party?.leader?.slotId ?: TOB_NO_LEADER)
    }
}

private fun Player.partyDisplayName(): String =
    displayName.ifBlank { username }.replaceFirstChar { char ->
        if (char.isLowerCase()) char.titlecase() else char.toString()
    }

private fun Player.sameCharacter(other: Player): Boolean =
    (username.equals(other.username, ignoreCase = true) && username.isNotBlank()) || this === other
