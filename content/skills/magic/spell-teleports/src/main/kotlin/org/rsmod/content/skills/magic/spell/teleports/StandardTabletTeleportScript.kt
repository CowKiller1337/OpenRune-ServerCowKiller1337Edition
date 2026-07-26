package org.rsmod.content.skills.magic.spell.teleports

import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.player.hook.PlayerTeleportValidator
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.clearMapFlag
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld1
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class StandardTabletTeleportScript
@Inject
constructor(
    private val teleportValidator: PlayerTeleportValidator,
    private val areaChecker: AreaChecker,
) : PluginScript() {
    override fun ScriptContext.startup() {
        StandardTabletTeleport.entries.forEach { tablet ->
            onOpHeld1(tablet.obj) { breakTablet(it.slot, tablet) }
        }
    }

    private suspend fun ProtectedAccess.breakTablet(slot: Int, tablet: StandardTabletTeleport) {
        if (actionDelay > mapClock) {
            return
        }
        if (!canTeleport()) {
            return
        }
        if (invDel(inv, tablet.obj, count = 1, slot = slot).failure) {
            return
        }

        actionDelay = mapClock + TeleportActionDelay
        anim(BreakAnim)
        spotanim(BreakSpotanim, height = 92)
        soundSynth(TeleportSound)
        delay(2)
        anim(AbsorbAnim)
        delay(1)
        telejump(tablet.destination)
    }

    private fun ProtectedAccess.canTeleport(): Boolean {
        val denial = teleportValidator.validate(player, TeleportType.Standard, areaChecker)
        if (denial == null) {
            return true
        }
        player.clearMapFlag()
        player.mes(denial, ChatType.Engine)
        return false
    }

    private enum class StandardTabletTeleport(
        val obj: String,
        val destination: CoordGrid,
    ) {
        Varrock("obj.poh_tablet_varrockteleport", CoordGrid(3213, 3424, 0)),
        Lumbridge("obj.poh_tablet_lumbridgeteleport", CoordGrid(3222, 3218, 0)),
        Falador("obj.poh_tablet_faladorteleport", CoordGrid(2965, 3380, 0)),
        House("obj.poh_tablet_teleporttohouse", CoordGrid(2954, 3224, 0)),
        Camelot("obj.poh_tablet_camelotteleport", CoordGrid(2757, 3477, 0)),
        Ardougne("obj.poh_tablet_ardougneteleport", CoordGrid(2662, 3305, 0)),
        Watchtower("obj.poh_tablet_watchtowerteleport", CoordGrid(2549, 3112, 0)),
        Kourend("obj.poh_tablet_kourendteleport", CoordGrid(1643, 3673, 0)),
        CivitasIllaFortis("obj.poh_tablet_fortisteleport", CoordGrid(1680, 3134, 0)),
        VolcanicMine("obj.fossil_tablet_volcanoteleport", CoordGrid(3815, 3810, 0)),
        Moonclan("obj.lunar_tablet_moonclan_teleport", CoordGrid(2113, 3915, 0)),
        Ourania("obj.lunar_tablet_ourania_teleport", CoordGrid(2467, 3245, 0)),
        Waterbirth("obj.lunar_tablet_waterbirth_teleport", CoordGrid(2546, 3758, 0)),
        Barbarian("obj.lunar_tablet_barbarian_teleport", CoordGrid(2544, 3570, 0)),
        Khazard("obj.lunar_tablet_khazard_teleport", CoordGrid(2664, 3161, 0)),
        FishingGuild("obj.lunar_tablet_fishing_guild_teleport", CoordGrid(2611, 3392, 0)),
        Catherby("obj.lunar_tablet_catherby_teleport", CoordGrid(2803, 3434, 0)),
        IcePlateau("obj.lunar_tablet_ice_plateau_teleport", CoordGrid(2975, 3943, 0)),
        Paddewwa("obj.tablet_paddewa", CoordGrid(3098, 9882, 0)),
        Senntisten("obj.tablet_senntisten", CoordGrid(3321, 3336, 0)),
        Kharyrll("obj.tablet_kharyll", CoordGrid(3494, 3473, 0)),
        Lassar("obj.tablet_lassar", CoordGrid(3004, 3470, 0)),
        Dareeyak("obj.tablet_dareeyak", CoordGrid(2968, 3696, 0)),
        Carrallangar("obj.tablet_carrallangar", CoordGrid(3158, 3666, 0)),
        Annakarl("obj.tablet_annakarl", CoordGrid(3288, 3886, 0)),
        Ghorrock("obj.tablet_ghorrock", CoordGrid(2976, 3872, 0)),
        SalveGraveyard("obj.teletab_salve", CoordGrid(3432, 3461, 0)),
        FenkenstrainsCastle("obj.teletab_fenk", CoordGrid(3548, 3529, 0)),
        WestArdougne("obj.teletab_westardy", CoordGrid(2500, 3291, 0)),
        HarmonyIsland("obj.teletab_harmony", CoordGrid(3797, 2867, 0)),
        Cemetery("obj.teletab_cemetery", CoordGrid(2980, 3763, 0)),
        Barrows("obj.teletab_barrows", CoordGrid(3565, 3314, 0)),
        ApeAtollDungeon("obj.teletab_ape", CoordGrid(2771, 9101, 0)),
        Battlefront("obj.teletab_battlefront", CoordGrid(1348, 3739, 0)),
        MindAltar("obj.teletab_mind_altar", CoordGrid(2980, 3509, 0)),
        DraynorManor("obj.teletab_draynor", CoordGrid(3108, 3351, 0)),
        LumbridgeGraveyard("obj.teletab_lumbridge", CoordGrid(3236, 3196, 0)),
    }

    private companion object {
        private const val BreakAnim = "seq.poh_smash_magic_tablet"
        private const val BreakSpotanim = "spotanim.poh_absorb_tablet_magic"
        private const val AbsorbAnim = "seq.poh_absorb_tablet_teleport"
        private const val TeleportSound = "synth.teleport_all"
        private const val TeleportActionDelay = 5
    }
}
