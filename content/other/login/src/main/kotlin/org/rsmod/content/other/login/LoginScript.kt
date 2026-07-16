package org.rsmod.content.other.login

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.StatType
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.misc.client.HideLocOps
import net.rsprot.protocol.game.outgoing.misc.client.HideNpcOps
import net.rsprot.protocol.game.outgoing.misc.client.HideObjOps
import net.rsprot.protocol.game.outgoing.misc.client.MinimapToggle
import net.rsprot.protocol.game.outgoing.misc.client.ResetAnims
import net.rsprot.protocol.game.outgoing.misc.player.ChatFilterSettings
import net.rsprot.protocol.game.outgoing.varp.VarpReset
import org.rsmod.api.inv.weight.InvWeight
import org.rsmod.api.net.central.OpenRuneCentralWorldLink
import org.rsmod.api.net.central.writeCentralSocialSnapshot
import org.rsmod.api.net.central.writeCentralSocialSnapshotEmpty
import org.rsmod.api.player.cinematic.CameraMode
import org.rsmod.api.player.cinematic.Cinematic
import org.rsmod.api.player.output.Camera
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.MiscOutput
import org.rsmod.api.player.output.UpdateRun
import org.rsmod.api.player.output.UpdateStat
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.startInvTransmit
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.resyncVar
import org.rsmod.api.realm.Realm
import org.rsmod.api.script.onEvent
import org.rsmod.api.server.config.ServerConfig
import org.rsmod.api.stats.levelmod.InvisibleLevels
import org.rsmod.api.table.DidyouknowRow
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class LoginScript
@Inject
constructor(
    private val realm: Realm,
    private val mapClock: MapClock,
    private val invisibleLevels: InvisibleLevels,
    private val config: ServerConfig,
    private val openRuneCentral: OpenRuneCentralWorldLink,
) : PluginScript() {
    private val statSyncEntries by lazy { statSyncEntries() }

    private var Player.chatboxUnlocked: Boolean by boolVarBit("varbit.has_displayname_transmitter")

    override fun ScriptContext.startup() {
        onEvent<SessionStateEvent.EngineLogin>(0L) { player.engineLogin() }
    }

    private fun Player.engineLogin() {
        sendHighPriority()
        sendLowPriority()
        VarPlayerIntMapSetter.set(this, "varbit.player_in_instance", 0)
    }

    private fun Player.sendHighPriority() {
        sendChatFilters()
        sendSocial()
        sendOpVisibility()
        sendWelcomeMessage()
        val validDidYouKnow = DidyouknowRow.all().filter { it.mobileonly != true }.random()
        mes("Did you know? ${validDidYouKnow.tip}", ChatType.DidYouKnow)
        sendVars()
    }

    private fun Player.sendChatFilters() {
        client.write(ChatFilterSettings(0, 0))
    }

    private fun Player.sendSocial() {
        if (!openRuneCentral.isEnabled) {
            return
        }

        if (characterId <= 0) {
            mes("Social list did not load: missing character.")
            writeCentralSocialSnapshotEmpty()
            return
        }

        when (val result = openRuneCentral.socialSnapshot(characterId)) {
            is OpenRuneCentralWorldLink.CentralSocialSnapshotResult.Ok -> {
                writeCentralSocialSnapshot(result.snapshot)
            }

            is OpenRuneCentralWorldLink.CentralSocialSnapshotResult.Failed -> {
                mes("Social list did not load: ${result.message}")
                writeCentralSocialSnapshotEmpty()
            }
        }
    }

    private fun Player.sendOpVisibility() {
        client.write(HideNpcOps(false))
        client.write(HideLocOps(false))
        client.write(HideObjOps(false))
    }

    private fun Player.sendWelcomeMessage() {
        val message = realm.config.loginMessage
        message?.let {
            mes(it.replace("RS Mod", config.name), ChatType.Welcome)
        }

        val broadcast = realm.config.loginBroadcast
        broadcast?.let { mes(it, ChatType.Broadcast) }
    }

    private fun Player.sendVars() {
        client.write(VarpReset)
        chatboxUnlocked = displayName.isNotBlank()
        applyLeagueWorldVars()
        for ((id, _) in vars) {
            val varp = ServerCacheManager.getVarp(id) ?: continue
            if (varp.transmit.never) {
                continue
            }
            resyncVar(varp)
        }
        resyncLeagueGeneralVar()
    }

    private fun Player.applyLeagueWorldVars() {
        ServerCacheManager.getVarbit(LEAGUE_ACCOUNT_VARBIT)?.let {
            VarPlayerIntMapSetter.set(this, it, 1)
        }
        ServerCacheManager.getVarbit(LEAGUE_TYPE_VARBIT)?.let {
            VarPlayerIntMapSetter.set(this, it, LEAGUE_6_TYPE)
        }
        ServerCacheManager.getVarbit(LEAGUE_TUTORIAL_COMPLETED_VARBIT)?.let {
            VarPlayerIntMapSetter.set(this, it, LEAGUE_TUTORIAL_COMPLETE_STAGE)
        }
        ServerCacheManager.getVarp(MAP_FLAGS_CACHED_VARP)?.let { varp ->
            val leagueWorldFlags =
                (vars[varp] or LEAGUE_WORLD_MAP_FLAG) and DEADMAN_WORLD_MAP_FLAG.inv()
            VarPlayerIntMapSetter.set(this, varp, leagueWorldFlags)
        }
    }

    private fun Player.resyncLeagueGeneralVar() {
        ServerCacheManager.getVarp(LEAGUE_GENERAL_VARP)?.let {
            resyncVar(it)
        }
    }

    private fun Player.sendLowPriority() {
        sendInvs()
        runClientScript(2498, 1, 0, 0)
        resetCam()
        runClientScript(828, 1)
        runClientScript(5141)
        runClientScript(626)
        sendPlayerOps()
        runClientScript(876, mapClock.cycle, 0, displayName, "REGULAR")
        sendStats()
        sendRun()
        client.write(ResetAnims)
        client.write(MinimapToggle(0))
    }

    private fun Player.sendInvs() {
        startInvTransmit(inv)
        startInvTransmit(worn)
    }

    private fun Player.resetCam() {
        Cinematic.setCameraMode(this, CameraMode.Normal)
        resyncVar("varbit.fov_clamp")
        Camera.camReset(this)
        runClientScript(CAMERA_DO_ZOOM_SCRIPT, DEFAULT_CAMERA_FOV, DEFAULT_CAMERA_FOV)
    }

    private fun Player.sendStats() {
        for ((statInternal, stat) in statSyncEntries) {
            val currXp = statMap.getXP(statInternal)
            val currLvl = stat(statInternal)
            val hiddenLvl = currLvl + invisibleLevels.get(this, statInternal)
            UpdateStat.update(this, stat, currXp, currLvl, hiddenLvl)
        }
    }

    private fun Player.sendRun() {
        val weightInGrams = InvWeight.calculateWeightInGrams(this)
        runWeight = weightInGrams
        UpdateRun.weight(this, kg = weightInGrams / 1000)
        UpdateRun.energy(this, runEnergy)
    }

    private fun Player.sendPlayerOps() {
        MiscOutput.setPlayerOp(this, slot = 2, op = null)
        MiscOutput.setPlayerOp(this, slot = 3, op = "Follow")
        MiscOutput.setPlayerOp(this, slot = 4, op = "Trade with")
        MiscOutput.setPlayerOp(this, slot = 5, op = null)
        MiscOutput.setPlayerOp(this, slot = 8, op = "Report")
    }

    private fun statSyncEntries(): List<Pair<String, StatType>> =
        ServerCacheManager.getStats().values.map { stat ->
            RSCM.getReverseMapping(RSCMType.STAT, stat.id) to stat
        }

    private companion object {
        private const val LEAGUE_ACCOUNT_VARBIT = 10031
        private const val LEAGUE_TYPE_VARBIT = 10032
        private const val LEAGUE_TUTORIAL_COMPLETED_VARBIT = 10037
        private const val LEAGUE_GENERAL_VARP = 2606
        private const val MAP_FLAGS_CACHED_VARP = 3717
        private const val LEAGUE_6_TYPE = 6
        private const val LEAGUE_TUTORIAL_COMPLETE_STAGE = 3
        private const val LEAGUE_WORLD_MAP_FLAG = 1 shl 30
        private const val DEADMAN_WORLD_MAP_FLAG = 1 shl 29
        private const val CAMERA_DO_ZOOM_SCRIPT = 42
        private const val DEFAULT_CAMERA_FOV = 314
    }
}
