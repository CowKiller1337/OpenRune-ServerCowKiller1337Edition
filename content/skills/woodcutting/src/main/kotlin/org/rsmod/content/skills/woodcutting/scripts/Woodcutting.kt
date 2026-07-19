package org.rsmod.content.skills.woodcutting.scripts

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.ObjectServerType
import dev.openrune.types.SequenceServerType
import jakarta.inject.Inject
import org.rsmod.api.config.Constants
import org.rsmod.api.config.locParam
import org.rsmod.api.config.locXpParam
import org.rsmod.api.config.objParam
import org.rsmod.api.config.refs.params
import org.rsmod.api.controller.vars.intVarCon
import org.rsmod.api.player.output.ClientScripts
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.firemakingLvl
import org.rsmod.api.player.stat.woodcuttingLvl
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.controller.ControllerRepository
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.repo.player.PlayerRepository
import org.rsmod.api.script.onAiConTimer
import org.rsmod.api.script.onOpContentLoc1
import org.rsmod.api.script.onOpContentLoc3
import org.rsmod.api.script.onOpContentU
import org.rsmod.api.stats.levelmod.InvisibleLevels
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.api.table.FiremakingLogsRow
import org.rsmod.content.skills.woodcutting.configs.WoodcuttingParams
import org.rsmod.events.UnboundEvent
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Controller
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.getInvObj
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO: Degrade axe charges once charged-tool obj vars are safely decoded.
class Woodcutting
@Inject
constructor(
    private val locRepo: LocRepository,
    private val conRepo: ControllerRepository,
    private val objRepo: ObjRepository,
    private val playerRepo: PlayerRepository,
    private val xpMods: XpModifiers,
    private val invisibleLvls: InvisibleLevels,
    private val mapClock: MapClock,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpContentLoc1("content.tree") { attempt(it.loc, it.type) }
        onOpContentLoc3("content.tree") { cut(it.loc, it.type) }
        onOpContentU("content.tree", "content.woodcutting_axe") { cut(it.loc, it.type) }
        onAiConTimer("controller.woodcutting_tree_duration") { controller.treeDespawnTick() }
    }

    private fun ProtectedAccess.attempt(tree: BoundLocInfo, type: ObjectServerType) {
        if (player.woodcuttingLvl < type.treeLevelReq) {
            mes("You need a Woodcutting level of ${type.treeLevelReq} to chop down this tree.")
            return
        }

        if (inv.isFull()) {
            val product = type.treeLogs
            mes("Your inventory is too full to hold any more ${product.name.lowercase()}.")
            soundSynth("synth.pillory_wrong")
            return
        }

        val axe = findAxe(player, type)
        if (axe == null) {
            mes("You need an axe to chop down this tree.")
            mes("You do not have an axe which you have the woodcutting level to use.")
            return
        }

        if (actionDelay < mapClock) {
            actionDelay = mapClock + 3
            skillAnimDelay = mapClock + 4
            swingAxe(axe, message = true)
            opLoc1(tree)
            return
        }

        cut(tree, type)
    }

    private fun ProtectedAccess.cut(tree: BoundLocInfo, type: ObjectServerType) {
        val axe = findAxe(player, type)
        if (axe == null) {
            mes("You need an axe to chop down this tree.")
            mes("You do not have an axe which you have the woodcutting level to use.")
            return
        }

        if (player.woodcuttingLvl < type.treeLevelReq) {
            mes("You need a Woodcutting level of ${type.treeLevelReq} to chop down this tree.")
            return
        }

        if (inv.isFull()) {
            val product = type.treeLogs
            mes("Your inventory is too full to hold any more ${product.name.lowercase()}.")
            soundSynth("synth.pillory_wrong")
            return
        }

        if (skillAnimDelay <= mapClock) {
            skillAnimDelay = mapClock + 4
            swingAxe(axe)
        }

        var cutLogs = false
        val despawn: Boolean

        if (actionDelay < mapClock) {
            actionDelay = mapClock + 3
        } else if (actionDelay == mapClock) {
            val (low, high) = cutSuccessRates(type, axe)
            cutLogs = statRandom("stat.woodcutting", low, high, invisibleLvls)
        }

        if (type.hasDespawnTimer) {
            treeSwingDespawnTick(tree, type)
            despawn = cutLogs && isTreeDespawnRequired(tree)
        } else {
            despawn = cutLogs && random.of(1, 255) > type.treeDepleteChance
        }

        if (cutLogs) {
            val product = type.treeLogs
            val xp = type.treeXp * xpMods.get(player, "stat.woodcutting")
            statAdvance("stat.woodcutting", xp)

            val infernalBurnXp = infernalBurnXp(axe, product)
            if (infernalBurnXp != null && random.randomBoolean(INFERNAL_AXE_BURN_CHANCE)) {
                spam("The infernal axe burns the logs to ashes.")
                statAdvance(
                    "stat.firemaking",
                    infernalBurnXp * xpMods.get(player, "stat.firemaking"),
                )
            } else {
                val productName = product.name.lowercase()
                val add = invAdd(inv, RSCM.getReverseMapping(RSCMType.OBJ, product.id))
                if (add.failure) {
                    mes("Your inventory is too full to hold any more $productName.")
                    soundSynth("synth.pillory_wrong")
                    resetAnim()
                    return
                }
                spam("You get some $productName.")
                soundSynth(LOG_OBTAINED_SOUND)
            }

            publish(CutLogs(player, tree, product))
            rollBirdNest(product)
        }

        if (despawn) {
            val respawnTime = type.resolveRespawnTime(random)
            locRepo.change(tree, type.treeStump, respawnTime)
            resetAnim()
            soundSynth("synth.tree_fall_sound")
            sendLocalOverlayLoc(tree, type, respawnTime)
        }

        opLoc3(tree)
    }

    private fun ProtectedAccess.swingAxe(axe: InvObj, message: Boolean = false) {
        anim(RSCM.getReverseMapping(RSCMType.SEQ, getInvObj(axe).axeWoodcuttingAnim.id))
        soundSynth(CHOP_SOUND)
        if (message) {
            spam("You swing your axe at the tree.")
        }
    }

    private fun ProtectedAccess.rollBirdNest(product: ItemServerType) {
        if (product.isType("obj.blisterwood_logs")) {
            return
        }

        val nest =
            when {
                product.isType("obj.redwood_logs") -> {
                    if (!random.randomBoolean(REDWOOD_CLUE_NEST_CHANCE)) {
                        return
                    }
                    NestDrop(random.rollClueNest(), "A clue nest falls out of the tree.")
                }
                random.randomBoolean(player.resolveClueNestChance()) ->
                    NestDrop(random.rollClueNest(), "A clue nest falls out of the tree.")
                random.randomBoolean(player.resolveBirdNestChance()) ->
                    NestDrop(
                        random.rollRegularNest(player.wearingStrungRabbitFoot()),
                        "A bird's nest falls out of the tree.",
                    )
                else -> return
            }

        mes("<col=B50A11>${nest.message}")
        objRepo.add(nest.obj, coords, BIRD_NEST_DURATION, player)
    }

    private fun ProtectedAccess.infernalBurnXp(axe: InvObj, product: ItemServerType): Double? {
        if (axe.id !in infernalAxeIds) {
            return null
        }
        val log = firemakingLogRows.firstOrNull { it.input.id == product.id } ?: return null
        val firemakingReq = log.statReq.firstOrNull()?.t1 ?: 1
        if (player.firemakingLvl < firemakingReq && !product.isType("obj.redwood_logs")) {
            return null
        }
        return log.xp * INFERNAL_AXE_FIREMAKING_XP_MULTIPLIER
    }

    private fun Controller.treeDespawnTick() {
        val type = ServerCacheManager.getObject(treeLocId)!!
        val tree = locRepo.findExact(coords, type)
        if (tree == null) {
            // Make sure the controller has lived beyond a single tick. Otherwise, we can make an
            // educated guess that there's an oversight allowing the tree to recreate controllers
            // faster than we'd expect. (1 tick intervals in this case)
            check(mapClock > creationCycle + 1) { "Tree loc deleted faster than expected." }
            conRepo.del(this)
            return
        }

        // If tree is actively being cut down by a player, increment the associated varcon.
        if (treeLastCut == mapClock.cycle - 1) {
            treeActivelyCutTicks++
        } else {
            treeActivelyCutTicks--
        }

        // If the tree has been idle (not cut) for a duration equal to or longer than the time it
        // was actively cut, the controller is no longer needed and can be safely deleted.
        if (treeActivelyCutTicks <= 0) {
            conRepo.del(this)
            return
        }

        // Reset the timer for next tick.
        aiTimer(1)

        // Keep the controller alive.
        resetDuration()
    }

    private fun treeSwingDespawnTick(tree: BoundLocInfo, type: ObjectServerType) {
        val controller = conRepo.findExact(tree.coords, "controller.woodcutting_tree_duration")
        if (controller != null) {
            check(controller.treeLocId == tree.id) {
                "Controller in coords is not associated with tree: " +
                    "controller=$controller, treeLoc=$tree, treeType=$type"
            }
            controller.treeLastCut = mapClock.cycle
            return
        }

        val spawn = Controller("controller.woodcutting_tree_duration", tree.coords)
        conRepo.add(spawn, type.treeDespawnTime)

        spawn.treeLocId = tree.id
        spawn.treeLastCut = mapClock.cycle
        spawn.treeActivelyCutTicks = 0
        spawn.aiTimer(1)
    }

    private fun isTreeDespawnRequired(tree: BoundLocInfo): Boolean {
        val controller = conRepo.findExact(tree.coords, "controller.woodcutting_tree_duration")
        return controller != null && controller.treeActivelyCutTicks >= controller.durationStart
    }

    private fun sendLocalOverlayLoc(tree: BoundLocInfo, type: ObjectServerType, respawnTime: Int) {
        val players = playerRepo.findAll(ZoneKey.from(tree.coords), zoneRadius = 3)
        for (player in players) {
            ClientScripts.addOverlayTimerLoc(
                player = player,
                coords = tree.coords,
                loc = type,
                shape = tree.shape,
                timer = Constants.overlay_timer_woodcutting,
                ticks = respawnTime,
                colour = 16765184,
            )
        }
    }

    data class CutLogs(val player: Player, val tree: BoundLocInfo, val product: ItemServerType) :
        UnboundEvent

    companion object {
        var Controller.treeActivelyCutTicks: Int by intVarCon("varcon.woodcutting_tree_cut_ticks")
        var Controller.treeLastCut: Int by intVarCon("varcon.woodcutting_tree_last_cut")
        var Controller.treeLocId: Int by intVarCon("varcon.woodcutting_tree_loc")

        val ItemServerType.axeWoodcuttingReq: Int by objParam(params.levelrequire)
        val ItemServerType.axeWoodcuttingAnim: SequenceServerType by objParam(params.skill_anim)

        val ObjectServerType.treeLevelReq: Int by locParam(params.levelrequire)
        val ObjectServerType.treeLogs: ItemServerType by locParam(params.skill_productitem)
        val ObjectServerType.treeXp: Double by locXpParam(params.skill_xp)
        val ObjectServerType.treeStump: ObjectServerType by locParam(params.next_loc_stage)
        val ObjectServerType.treeDespawnTime: Int by locParam(params.despawn_time)
        val ObjectServerType.treeDepleteChance: Int by locParam(params.deplete_chance)
        val ObjectServerType.treeRespawnTime: Int by locParam(params.respawn_time)
        val ObjectServerType.treeRespawnTimeLow: Int by locParam(params.respawn_time_low)
        val ObjectServerType.treeRespawnTimeHigh: Int by locParam(params.respawn_time_high)

        private val ObjectServerType.hasDespawnTimer: Boolean
            get() = hasParam(params.despawn_time)

        fun findAxe(player: Player, type: ObjectServerType): InvObj? {
            val worn = player.wornAxe()
            val carried = player.carriedAxe()
            if (worn != null && carried != null) {
                val wornSuccess = cutSuccessRates(type, worn)
                val carriedSuccess = cutSuccessRates(type, carried)
                if (
                    (wornSuccess.first + wornSuccess.second) / 2 >=
                        (carriedSuccess.first + carriedSuccess.second) / 2
                ) {
                    return worn
                }
                return carried
            }
            return worn ?: carried
        }

        private fun Player.wornAxe(): InvObj? {
            val righthand = righthand ?: return null
            return righthand.takeIf { getInvObj(it).isUsableAxe(woodcuttingLvl) }
        }

        private fun Player.carriedAxe(): InvObj? {
            return inv.filterNotNull { getInvObj(it).isUsableAxe(woodcuttingLvl) }
                .maxByOrNull { getInvObj(it).axeWoodcuttingReq }
        }

        private fun ItemServerType.isUsableAxe(woodcuttingLevel: Int): Boolean =
            isContentType("content.woodcutting_axe") && woodcuttingLevel >= axeWoodcuttingReq

        private fun ObjectServerType.resolveRespawnTime(random: GameRandom): Int {
            val fixed = treeRespawnTime
            if (fixed > 0) {
                return fixed
            }
            return random.of(treeRespawnTimeLow, treeRespawnTimeHigh)
        }

        fun cutSuccessRates(treeType: ObjectServerType, axe: InvObj): Pair<Int, Int> {
            val axes = treeType.param(WoodcuttingParams.success_rates)
            val rates = axes.find { it.key.id == axe.id }?.value ?: error("Unable to get axe rates")
            val low = rates shr 16
            val high = rates and 0xFFFF
            return low to high
        }

        private fun GameRandom.rollRegularNest(wearingRabbitFoot: Boolean): String {
            return if (wearingRabbitFoot) {
                when (of(BIRD_NEST_ROLL_SLOTS)) {
                    in 0 until 60 -> "obj.bird_nest_seeds_jan2019"
                    in 60 until 90 -> "obj.bird_nest_ring"
                    in 90 until 94 -> "obj.bird_nest_egg_blue"
                    in 94 until 97 -> "obj.bird_nest_egg_red"
                    else -> "obj.bird_nest_egg_green"
                }
            } else {
                when (of(BIRD_NEST_ROLL_SLOTS)) {
                    in 0 until 65 -> "obj.bird_nest_seeds_jan2019"
                    in 65 until 97 -> "obj.bird_nest_ring"
                    97 -> "obj.bird_nest_egg_blue"
                    98 -> "obj.bird_nest_egg_red"
                    else -> "obj.bird_nest_egg_green"
                }
            }
        }

        private fun GameRandom.rollClueNest(): String {
            return when (of(CLUE_NEST_ROLL_SLOTS)) {
                in 0 until 20 -> "obj.wc_clue_nest_beginner"
                in 20 until 50 -> "obj.wc_clue_nest_easy"
                in 50 until 75 -> "obj.wc_clue_nest_medium"
                in 75 until 93 -> "obj.wc_clue_nest_hard"
                else -> "obj.wc_clue_nest_elite"
            }
        }

        private fun Player.wearingStrungRabbitFoot(): Boolean {
            return worn.any { obj -> obj != null && obj.id in strungRabbitFootIds }
        }

        private fun Player.wearingWoodcuttingCape(): Boolean {
            return worn.any { obj -> obj != null && obj.id in woodcuttingCapeIds }
        }

        private fun Player.resolveBirdNestChance(): Int {
            return if (wearingWoodcuttingCape()) BIRD_NEST_CAPE_CHANCE else BIRD_NEST_CHANCE
        }

        private fun Player.resolveClueNestChance(): Int {
            return if (wearingWoodcuttingCape()) CLUE_NEST_CAPE_CHANCE else CLUE_NEST_CHANCE
        }

        private data class NestDrop(val obj: String, val message: String)

        private val firemakingLogRows: List<FiremakingLogsRow> by lazy { FiremakingLogsRow.all() }

        private val infernalAxeIds: Set<Int> by lazy {
            itemIds("obj.infernal_axe", "obj.trailblazer_axe")
        }

        private val strungRabbitFootIds: Set<Int> by lazy {
            itemIds("obj.hunting_strung_rabbit_foot")
        }

        private val woodcuttingCapeIds: Set<Int> by lazy {
            setOfNotNull(
                itemIdOrNull("obj.skillcape_woodcutting"),
                itemIdOrNull("obj.skillcape_woodcutting_trimmed"),
                itemIdOrNull("obj.skillcape_max"),
            )
        }

        private fun itemIdOrNull(internal: String): Int? {
            return runCatching { RSCM.getRSCM(internal) }.getOrNull()
        }

        private fun itemIds(vararg internals: String): Set<Int> {
            return internals.mapNotNull(::itemIdOrNull).toSet()
        }

        private const val CHOP_SOUND: Int = 2053
        private const val LOG_OBTAINED_SOUND: Int = 2734
        private const val INFERNAL_AXE_BURN_CHANCE: Int = 3
        private const val INFERNAL_AXE_FIREMAKING_XP_MULTIPLIER: Double = 0.5
        private const val BIRD_NEST_CHANCE: Int = 256
        private const val BIRD_NEST_CAPE_CHANCE: Int = 233
        private const val CLUE_NEST_CHANCE: Int = 1024
        private const val CLUE_NEST_CAPE_CHANCE: Int = 932
        private const val BIRD_NEST_ROLL_SLOTS: Int = 100
        private const val CLUE_NEST_ROLL_SLOTS: Int = 100
        private const val REDWOOD_CLUE_NEST_CHANCE: Int = 380
        private const val BIRD_NEST_DURATION: Int = 200
    }
}
