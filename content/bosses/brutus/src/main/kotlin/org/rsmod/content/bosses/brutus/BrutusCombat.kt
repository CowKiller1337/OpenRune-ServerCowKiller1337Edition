package org.rsmod.content.bosses.brutus

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcServerType
import dev.openrune.types.aconverted.SpotanimType
import jakarta.inject.Inject
import kotlin.math.abs
import org.rsmod.api.bosses.dsl.boss
import org.rsmod.api.bosses.dsl.external
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.player.hit.modifier.NoopPlayerHitModifier
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.route.RayCastValidator
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onNpcQueue
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.entity.npc.NpcUid
import org.rsmod.game.hit.HitType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

class BrutusCombat
@Inject
constructor(
    deps: BossDeps,
    private val rayCast: RayCastValidator,
    private val death: NpcDeath,
    private val aiPlayerInteractions: AiPlayerInteractions,
) : BossPluginScript(deps) {

    private val states = HashMap<NpcUid, BrutusState>()

    override fun ScriptContext.startup() {
        val brutusTypes = BRUTUS_NPCS.map(::npcType).onEach(::configureAggroRange)

        deps.extensionRegistry.register(COMBAT_CYCLE_HANDLER) { access, npc, target, _ ->
            access.runCombatCycle(npc, target)
        }

        BossCombat.register(
            ctx = this,
            spec = spec,
            deps = deps,
        )

        for (type in brutusTypes) {
            onNpcQueue(type, "queue.death") { death.deathWithDrops(this, deathAnimOverride = DEATH_ANIM) }
        }

        onEvent<NpcStateEvents.Respawn> {
            if (npc.isBrutus()) {
                states.remove(npc.uid)
            }
        }
        onEvent<NpcStateEvents.Delete> {
            if (npc.isBrutus()) {
                states.remove(npc.uid)
            }
        }
    }

    override val spec =
        boss("npc.cowboss", "npc.cowboss_routefind", "npc.cowboss_hardmode") {
            stats(attackRate = ATTACK_SPEED, aggressionRadius = INSTANCE_AGGRO_RANGE)

            val combatCycle = ability("brutus_combat_cycle") { include(external(COMBAT_CYCLE_HANDLER)) }

            phase("combat") {
                weightedSelectorRandom(noRepeatBias = 0.0) {
                    +random(combatCycle, weight = 1)
                }
            }
        }

    private suspend fun StandardNpcAccess.runCombatCycle(npc: Npc, target: Player) {
        if (npc.hitpoints <= 0 || !target.isValidTarget()) {
            return
        }

        npc.lockFacing(target.coords)

        if (!npc.isWithinDistance(target, 1)) {
            if (shouldForceCharge(npc, target)) {
                states.reset(npc)
                growl(npc, target)
            } else {
                engageMelee(target)
            }
            return
        }

        if (shouldForceCharge(npc, target)) {
            states.reset(npc)
            growl(npc, target)
            return
        }

        val state = states.getOrPut(npc.uid) { BrutusState(nextSpecialAt = nextSpecialAt()) }
        if (state.standardAttacks >= state.nextSpecialAt) {
            states.reset(npc)
            if (deps.random.randomBoolean()) {
                snort(npc, target)
            } else {
                growl(npc, target)
            }
            return
        }

        state.standardAttacks++
        melee(npc, target)
    }

    private fun StandardNpcAccess.melee(npc: Npc, target: Player) {
        npc.anim(MELEE_ANIM)
        target.queueHit(
            source = npc,
            delay = 1,
            type = HitType.Melee,
            damage = deps.random.of(MAX_MELEE_HIT + 1),
        )
    }

    private suspend fun StandardNpcAccess.snort(npc: Npc, target: Player) {
        say("*Snort*")
        npc.lockFacing(target.coords)
        npc.anim(SLAM_ANIM)

        val markedTile = target.coords
        val direction = directionFrom(npc.coords, markedTile)
        delay(TELEGRAPH_TICKS)

        if (!target.isValidTarget()) {
            return
        }

        val unsafeTiles = slamDangerTiles(markedTile, direction)
        playSlamImpact(unsafeTiles)
        val damage =
            if (target.coords in unsafeTiles) {
                deps.random.of(1, MAX_SPECIAL_HIT)
            } else {
                0
            }
        target.queueHit(
            source = npc,
            delay = 1,
            type = HitType.Typeless,
            damage = damage,
            modifier = NoopPlayerHitModifier,
        )
    }

    private fun playSlamImpact(tiles: List<CoordGrid>) {
        for ((index, tile) in tiles.withIndex()) {
            val spot = SLAM_IMPACT_SPOTANIMS[index.coerceAtMost(SLAM_IMPACT_SPOTANIMS.lastIndex)]
            deps.worldRepo.spotanimMap(SpotanimType(spot.asRSCM(RSCMType.SPOTANIM)), tile)
        }
    }

    private suspend fun StandardNpcAccess.growl(npc: Npc, target: Player) {
        say("*Growl*")

        val origin = npc.coords
        val direction = directionFrom(origin, target.coords)
        val distance = deps.random.of(CHARGE_MIN_DISTANCE, CHARGE_MAX_DISTANCE)
        val path = forwardLine(origin, direction, distance)

        npc.lockFacing(target.coords)
        npc.anim(CHARGE_ANIM)
        spotanim(CHARGE_LAUNCH_SPOTANIM)
        delay(TELEGRAPH_TICKS)

        val victims = playersOnPath(path)
        playChargePath(path)
        telejump(path.last(), deps.collision)
        playMapSpotanim(CHARGE_IMPACT_SPOTANIM, path.last())
        engageMelee(target)

        for (victim in victims) {
            victim.spotanim(CHARGE_IMPACT_SPOTANIM)
            val damage = deps.random.of(1, MAX_SPECIAL_HIT)
            victim.queueHit(
                source = npc,
                delay = 1,
                type = HitType.Typeless,
                damage = damage,
                modifier = NoopPlayerHitModifier,
            )
            victim.actionDelay = maxOf(victim.actionDelay, victim.currentMapClock + STUN_TICKS)
        }
    }

    private fun playChargePath(path: List<CoordGrid>) {
        for ((index, tile) in path.withIndex()) {
            playMapSpotanim(CHARGE_PATH_SPOTANIM, tile, delay = index)
        }
    }

    private fun playMapSpotanim(spotanim: String, tile: CoordGrid, delay: Int = 0) {
        deps.worldRepo.spotanimMap(
            SpotanimType(spotanim.asRSCM(RSCMType.SPOTANIM)),
            tile,
            delay = delay,
        )
    }

    private fun StandardNpcAccess.engageMelee(target: Player) {
        npc.apRangeOverride = null
        npc.apRequiresLineOfSight = true
        npc.movementLocked = false
        npc.ignoreCombatInteractions = false
        opPlayer2(target, aiPlayerInteractions)
    }

    private fun playersOnPath(path: List<CoordGrid>): List<Player> {
        val dangerousTiles = path.toSet()
        return deps.playerList.filter { player ->
            player.isValidTarget() && player.coords in dangerousTiles
        }
    }

    private fun shouldForceCharge(npc: Npc, target: Player): Boolean {
        if (npc.coords.level != target.coords.level) {
            return false
        }

        val size = npc.type.size.coerceAtLeast(1)
        return !rayCast.hasLineOfWalk(
            source = npc.coords,
            destination = target.coords,
            srcWidth = size,
            srcLength = size,
        )
    }

    private fun MutableMap<NpcUid, BrutusState>.reset(npc: Npc) {
        this[npc.uid] = BrutusState(nextSpecialAt = nextSpecialAt())
    }

    private fun nextSpecialAt(): Int = deps.random.of(SPECIAL_ATTACK_MIN, SPECIAL_ATTACK_MAX)

    private fun Npc.isBrutus(): Boolean = BRUTUS_NPCS.any { isType(it) || isVisType(it) }

    private fun npcType(type: String): NpcServerType =
        ServerCacheManager.getNpc(type.asRSCM(RSCMType.NPC)) ?: error("Missing npc type: $type")

    private fun configureAggroRange(type: NpcServerType) {
        type.maxRange = maxOf(type.maxRange, INSTANCE_AGGRO_RANGE)
        type.huntRange = maxOf(type.huntRange, INSTANCE_AGGRO_RANGE)
        type.wanderRange = maxOf(type.wanderRange, INSTANCE_AGGRO_RANGE)
        type.giveChase = true
    }

    private companion object {
        private const val ATTACK_SPEED = 5
        private const val MAX_MELEE_HIT = 3
        private const val MAX_SPECIAL_HIT = 19
        private const val INSTANCE_AGGRO_RANGE = 64
        private const val TELEGRAPH_TICKS = 3
        private const val STUN_TICKS = 2
        private const val SPECIAL_ATTACK_MIN = 4
        private const val SPECIAL_ATTACK_MAX = 5
        private const val CHARGE_MIN_DISTANCE = 3
        private const val CHARGE_MAX_DISTANCE = 4

        private const val COMBAT_CYCLE_HANDLER = "brutus.combat_cycle"

        private const val MELEE_ANIM = "seq.cow_boss_attack"
        private const val SLAM_ANIM = "seq.cow_boss_stomp"
        private const val CHARGE_ANIM = "seq.cow_boss_charge"
        private const val DEATH_ANIM = "seq.cow_boss_death"
        private const val CHARGE_LAUNCH_SPOTANIM = "spotanim.vfx_cowboss_hm_launch_melee_01"
        private const val CHARGE_PATH_SPOTANIM = "spotanim.vfx_cowboss_hm_melee_01"
        private const val CHARGE_IMPACT_SPOTANIM = "spotanim.vfx_cowboss_hm_impact_melee_01"

        private val SLAM_IMPACT_SPOTANIMS =
            listOf(
                "spotanim.vfx_cowboss_stomp_impact01",
                "spotanim.vfx_cowboss_stomp_impact02",
                "spotanim.vfx_cowboss_stomp_impact03",
            )

        private val BRUTUS_NPCS =
            setOf("npc.cowboss", "npc.cowboss_routefind", "npc.cowboss_hardmode")
    }

    private data class BrutusState(
        var standardAttacks: Int = 0,
        val nextSpecialAt: Int,
    )

}

private data class Step(val dx: Int, val dz: Int)

private fun directionFrom(source: CoordGrid, target: CoordGrid): Step {
    val dx = target.x - source.x
    val dz = target.z - source.z
    return when {
        dx == 0 && dz == 0 -> Step(0, 1)
        abs(dx) >= abs(dz) -> Step(dx.signStep(), 0)
        else -> Step(0, dz.signStep())
    }
}

private fun slamDangerTiles(markedTile: CoordGrid, direction: Step): List<CoordGrid> =
    listOf(
        markedTile,
        markedTile.translate(direction.dx, direction.dz),
        markedTile.translate(-direction.dx, -direction.dz),
    )

private fun forwardLine(origin: CoordGrid, direction: Step, distance: Int): List<CoordGrid> =
    (1..distance).map { step ->
        origin.translate(direction.dx * step, direction.dz * step)
    }

private fun Int.signStep(): Int =
    when {
        this > 0 -> 1
        this < 0 -> -1
        else -> 0
    }
