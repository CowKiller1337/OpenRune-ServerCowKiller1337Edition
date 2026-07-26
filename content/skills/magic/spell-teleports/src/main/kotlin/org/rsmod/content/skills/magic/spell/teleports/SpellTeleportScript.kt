package org.rsmod.content.skills.magic.spell.teleports

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.aconverted.interf.IfButtonOp
import jakarta.inject.Inject
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.config.refs.params
import org.rsmod.api.combat.commons.magic.MagicSpell
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.hook.PlayerTeleportValidator
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.clearMapFlag
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.api.utils.time.epochMinute
import org.rsmod.content.quest.manager.Quest
import org.rsmod.game.inv.isType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SpellTeleportScript
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val teleportValidator: PlayerTeleportValidator,
    private val areaChecker: AreaChecker,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (teleport in StandardSpellTeleport.entries) {
            val spell = teleport.resolveSpell() ?: continue
            onIfOverlayButton(spell.component) { castSpellTeleport(spell, teleport, it.op) }
            if (teleport == StandardSpellTeleport.Home) {
                onIfOverlayButton(LeagueHomeTeleportComponent) {
                    castSpellTeleport(spell, teleport, it.op)
                }
            }
        }
        onPlayerQueueWithArgs<PendingSpellTeleport>(TeleportQueue) {
            processQueuedTeleport(it.args)
        }
    }

    private fun StandardSpellTeleport.resolveSpell(): MagicSpell? {
        val spellObj = ServerCacheManager.getItem(spellObj.asRSCM(RSCMType.OBJ)) ?: return null
        return spells.getObjSpell(spellObj)
    }

    private suspend fun ProtectedAccess.castSpellTeleport(
        spell: MagicSpell,
        teleport: StandardSpellTeleport,
        op: IfButtonOp,
    ) {
        if (actionDelay > mapClock) {
            return
        }

        if (!teleport.canCast(this)) {
            return
        }

        val option = teleport.option(op)
        val destination = teleport.destination(spell, option)
        if (destination == null) {
            mes(option.missingDestinationMessage)
            return
        }

        if (!canTeleport()) {
            return
        }

        if (teleport.isHome && !canCastHomeTeleport()) {
            return
        }

        if (!consumeRequirements(spell, teleport)) {
            return
        }

        actionDelay = mapClock + TeleportActionDelay
        anim(TeleportStartAnim)
        spotanim(TeleportSpotanim, height = TeleportSpotanimHeight)
        soundSynth(TeleportSound)
        clearQueue(TeleportQueue)
        queue(TeleportQueue, TeleportDelay, PendingSpellTeleport(teleport, destination.packed))
    }

    private fun ProtectedAccess.processQueuedTeleport(task: PendingSpellTeleport) {
        val spell = task.teleport.resolveSpell() ?: return
        if (!canTeleport()) {
            return
        }
        telejump(CoordGrid(task.destination))
        anim(TeleportEndAnim)
        if (task.teleport.isHome) {
            player.attr[HomeTeleportCooldownAttr] = epochMinute() + HomeTeleportCooldownMinutes
        }
        statAdvance("stat.magic", spell.castXp)
    }

    private fun ProtectedAccess.canCastHomeTeleport(): Boolean {
        val nextAllowed = player.attr[HomeTeleportCooldownAttr] ?: return true
        val remaining = nextAllowed - epochMinute()
        if (remaining <= 0) {
            return true
        }
        val minuteText = if (remaining == 1) "minute" else "minutes"
        mes("You can cast Home Teleport again in $remaining $minuteText.")
        return false
    }

    private fun ProtectedAccess.canTeleport(): Boolean {
        val denial =
            teleportValidator.validate(player, TeleportType.Standard, areaChecker)
        if (denial == null) {
            return true
        }
        player.clearMapFlag()
        player.mes(denial, ChatType.Engine)
        return false
    }

    private fun ProtectedAccess.consumeRequirements(
        spell: MagicSpell,
        teleport: StandardSpellTeleport,
    ): Boolean {
        val castSpell =
            if (teleport.requiresBanana) {
                val banana = ServerCacheManager.getItem(Banana.asRSCM(RSCMType.OBJ)) ?: return false
                val spellWithoutBanana = spell.copy(objReqs = spell.objReqs.withoutBananaReq())
                if (!runes.canCastSpell(player, spellWithoutBanana)) {
                    return false
                }
                if (!deleteBanana(banana)) {
                    mes("You need a banana to cast this spell.")
                    return false
                }
                spellWithoutBanana
            } else {
                spell
            }

        if (castSpell.objReqs.isEmpty()) {
            return runes.canCastSpell(player, castSpell)
        }
        return !runes.attemptCast(player, castSpell).isFailure()
    }

    private fun List<MagicSpell.ObjRequirement>.withoutBananaReq(): List<MagicSpell.ObjRequirement> {
        return filterNot { RSCM.getReverseMapping(RSCMType.OBJ, it.obj.id).contains("banana") }
    }

    private fun ProtectedAccess.deleteBanana(banana: ItemServerType): Boolean {
        val bananaSlot = inv.indexOfFirst { it.isType(banana) }
        if (bananaSlot == -1) {
            return false
        }
        val transaction =
            player.invTransaction(inv, autoCommit = true) {
                val targetInv = select(inv)
                delete {
                    from = targetInv
                    obj = banana.id
                    strictCount = 1
                    strictSlot = bananaSlot
                }
            }
        return transaction.success
    }

    private data class PendingSpellTeleport(
        val teleport: StandardSpellTeleport,
        val destination: Int,
    )

    private enum class StandardSpellTeleport(
        val spellObj: String,
        val destination: CoordGrid? = null,
        val destinationLevel: Int? = null,
        val alternate: TeleportOption? = null,
        val requiredQuest: String? = null,
        val isHome: Boolean = false,
        val requiresBanana: Boolean = false,
        val lockedMessage: String = "You need to complete the required quest to cast this spell.",
        val missingDestinationMessage: String = "That teleport is not implemented yet.",
    ) {
        Home(
            "obj.48_home_teleport",
            CoordGrid(1503, 5602, 0),
            isHome = true,
        ),
        AncientHome(
            "obj.01_zaros_home_tele",
            CoordGrid(1503, 5602, 0),
            isHome = true,
        ),
        LunarHome(
            "obj.01_lunar_home_tele",
            CoordGrid(1503, 5602, 0),
            isHome = true,
        ),
        Varrock(
            "obj.25_varrock_teleport",
            alternate = TeleportOption(destination = CoordGrid(3164, 3487, 0)),
        ),
        Lumbridge(
            "obj.31_lumbridge_teleport",
        ),
        Falador(
            "obj.37_falador_teleport",
        ),
        TeleportToHouse(
            "obj.67_house_teleport",
            alternate =
                TeleportOption(
                    missingDestinationMessage =
                        "You need to purchase a house before you can teleport outside it."
                ),
            missingDestinationMessage = "You need to purchase a house before you can use this spell.",
        ),
        Camelot(
            "obj.45_camelot_teleport",
            alternate = TeleportOption(destination = CoordGrid(2725, 3485, 0)),
        ),
        KourendCastle(
            // Cache name is wrong, but its spell params match Kourend Castle Teleport.
            "obj.cert_deadman_level99_lamp",
            requiredQuest = "quest_clientofkourend",
            lockedMessage = "You need to complete Client of Kourend to cast this spell.",
        ),
        Ardougne(
            "obj.51_ardougne_teleport",
            requiredQuest = "quest_plaguecity",
            lockedMessage = "You need to complete Plague City to cast this spell.",
        ),
        CivitasIllaFortis(
            // Cache name is wrong, but its spell params match Civitas illa Fortis Teleport.
            "obj.placeholder_blighted_sack_snare",
            requiredQuest = "quest_twilightspromise",
            lockedMessage = "You need to complete Twilight's Promise to cast this spell.",
        ),
        Watchtower(
            "obj.58_watchtower_teleport",
            alternate = TeleportOption(destination = CoordGrid(2544, 3095, 0)),
            requiredQuest = "quest_watchtowerquest",
            lockedMessage = "You need to complete Watchtower to cast this spell.",
        ),
        Trollheim(
            "obj.61_trollheim_teleport",
            requiredQuest = "quest_eadgarsruse",
            lockedMessage = "You need to complete Eadgar's Ruse to cast this spell.",
        ),
        ApeAtoll(
            "obj.64_ape_atoll_teleport",
            destinationLevel = 1,
            requiresBanana = true,
        ),
        TeleportBoatToMe(
            "obj.56_teleport_boat_to_me",
            requiredQuest = "quest_pandemonium",
            lockedMessage = "You need to complete Pandemonium to cast this spell.",
            missingDestinationMessage = "Boat teleports need boat-location support before they can be cast.",
        ),
        TeleportMeToBoat(
            "obj.67_teleport_me_to_boat",
            alternate =
                TeleportOption(
                    missingDestinationMessage =
                        "Last boat teleports need boat-location support before they can be cast."
                ),
            requiredQuest = "quest_pandemonium",
            lockedMessage = "You need to complete Pandemonium to cast this spell.",
            missingDestinationMessage = "Boat teleports need boat-location support before they can be cast.",
        ),
        Paddewwa("obj.54_paddewwa_teleport"),
        Senntisten("obj.60_senntisten_teleport"),
        Kharyrll("obj.66_kharyllyl_teleport"),
        Lassar("obj.72_lassar_teleport"),
        Dareeyak("obj.78_dareeyak_teleport"),
        Carrallangar("obj.84_carrallagar_teleport"),
        Annakarl("obj.90_annakarl_teleport"),
        Ghorrock("obj.96_ghorrock_teleport"),
        Moonclan("obj.69_tele_moonclan"),
        MoonclanGroup("obj.70_tele_moonclan_group"),
        Ourania("obj.71_tele_zmialtar"),
        Waterbirth("obj.72_tele_waterbirth"),
        WaterbirthGroup("obj.73_tele_waterbirth_group"),
        BarbarianOutpost("obj.75_tele_barb_outpost"),
        BarbarianOutpostGroup("obj.76_tele_barb_outpost_group"),
        Khazard("obj.78_tele_port_khazard"),
        KhazardGroup("obj.79_tele_port_khazard_group"),
        FishingGuild("obj.85_tele_fish_guild"),
        FishingGuildGroup("obj.86_tele_fish_guild_group"),
        Catherby("obj.87_tele_catherby"),
        CatherbyGroup("obj.88_tele_catherby_group"),
        LunarGhorrock("obj.89_tele_ghorrock"),
        LunarGhorrockGroup("obj.90_tele_ghorrock_group"),
        Battlefront("obj.23_teleport_battlefront"),
        // These Arceuus spells inherit mismatched cache names, but their spell params are valid.
        ArceuusLibrary("obj.br_mithril_platebody"),
        Respawn("obj.br_mithril_platelegs"),
        MindAltar("obj.br_greendhide_body"),
        SalveGraveyard("obj.br_greendhide_chaps"),
        FenkenstrainsCastle("obj.br_moonclan_body"),
        WestArdougne("obj.br_moonclan_legs"),
        HarmonyIsland("obj.br_xeric_body"),
        Cemetery("obj.br_xeric_legs"),
        Barrows("obj.br_air_staff"),
        ApeAtollArceuus("obj.br_dragon_helm");

        fun option(op: IfButtonOp): TeleportOption {
            return if (op == IfButtonOp.Op2 && alternate != null) {
                alternate
            } else {
                TeleportOption(destination, destinationLevel, missingDestinationMessage)
            }
        }

        fun destination(spell: MagicSpell, option: TeleportOption): CoordGrid? {
            val coord = option.destination ?: destination ?: spell.obj.paramOrNull(params.spell_telecoord)
            val level = option.destinationLevel ?: destinationLevel
            return if (coord != null && level != null) {
                coord.copy(level = level)
            } else {
                coord
            }
        }

        fun canCast(access: ProtectedAccess): Boolean {
            val questKey = requiredQuest ?: return true
            val quest = Quest.get(questKey)
            if (quest?.isQuestCompleted(access.player) == true) {
                return true
            }
            access.mes(lockedMessage)
            return false
        }
    }

    private data class TeleportOption(
        val destination: CoordGrid? = null,
        val destinationLevel: Int? = null,
        val missingDestinationMessage: String = "That teleport is not implemented yet.",
    )

    private companion object {
        private val TeleportStartAnim = RSCM.getReverseMapping(RSCMType.SEQ, 714)
        private val TeleportEndAnim = RSCM.getReverseMapping(RSCMType.SEQ, 715)
        private val TeleportSpotanim = RSCM.getReverseMapping(RSCMType.SPOTANIM, 111)
        private const val Banana = "obj.banana"
        private const val LeagueHomeTeleportComponent = "component.magic_spellbook:league_home_teleport"
        private const val TeleportQueue = "queue.spell_teleport"
        private const val TeleportSound = "synth.teleport_all"
        private const val TeleportSpotanimHeight = 92
        private const val TeleportDelay = 4
        private const val TeleportActionDelay = 5
        private const val HomeTeleportCooldownMinutes = 30
        private val HomeTeleportCooldownAttr =
            AttributeKey<Int>(persistenceKey = "magic.home_teleport_cooldown")
    }
}
