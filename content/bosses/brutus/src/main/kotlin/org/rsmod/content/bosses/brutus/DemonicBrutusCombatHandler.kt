package org.rsmod.content.bosses.brutus

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcServerType
import dev.openrune.types.aconverted.SpotanimType
import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.boss
import org.rsmod.api.bosses.dsl.external
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.combat.commons.player.queueCombatRetaliate
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.player.cheat.adminGodMode
import org.rsmod.api.player.hit.modifier.NoopPlayerHitModifier
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.interact.NpcInteractions
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onNpcHit
import org.rsmod.api.script.onNpcQueue
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.entity.npc.NpcUid
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.isType
import org.rsmod.game.interact.InteractionNpc
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.movement.MoveSpeed
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

class DemonicBrutusCombatHandler
@Inject
constructor(
    deps: BossDeps,
    private val death: NpcDeath,
    private val npcRepo: NpcRepository,
    private val aiPlayerInteractions: AiPlayerInteractions,
    private val playerNpcInteractions: NpcInteractions,
) : BossPluginScript(deps) {

    private val states = HashMap<NpcUid, DemonicBrutusState>()

    override fun ScriptContext.startup() {
        val type = npcType(DEMONIC_BRUTUS_NPC).also(::configureAggroRange)
        npcType(GHOST_NPC)

        deps.extensionRegistry.register(COMBAT_CYCLE_HANDLER) { access, npc, target, _ ->
            access.runCombatCycle(npc, target)
        }

        BossCombat.register(ctx = this, spec = spec, deps = deps)

        onNpcQueue(type, "queue.death") { handleDeathQueue() }

        onNpcHit(type) {
            if (!hit.isFromPlayer) {
                return@onNpcHit
            }
            val state = states[npc.uid] ?: return@onNpcHit
            if (npc.currentMapClock - state.fightStartTick <= OPENING_SHORT_CIRCUIT_TICKS) {
                state.openingDamage += hit.damage
            }
        }

        onEvent<NpcStateEvents.Respawn> {
            if (npc.isDemonicBrutus()) {
                cleanupState(npc)
            }
        }
        onEvent<NpcStateEvents.Delete> {
            if (npc.isDemonicBrutus()) {
                cleanupState(npc)
            }
        }
    }

    override val spec =
        boss(DEMONIC_BRUTUS_NPC) {
            stats(attackRate = ATTACK_SPEED, aggressionRadius = INSTANCE_AGGRO_RANGE)

            val combatCycle = ability("demonic_brutus_combat_cycle") {
                include(external(COMBAT_CYCLE_HANDLER))
            }

            phase("combat") {
                weightedSelectorRandom(noRepeatBias = 0.0) {
                    +random(combatCycle, weight = 1)
                }
            }
        }

    private suspend fun StandardNpcAccess.runCombatCycle(npc: Npc, target: Player) {
        val state = states.getOrPut(npc.uid) { newState(npc) }
        if (state.specialActive || state.phaseTransitionActive || npc.hitpoints <= 0) {
            return
        }
        if (!target.isValidTarget()) {
            return
        }

        npc.setIdleAnim(IDLE_ANIM)
        npc.lockFacing(target.coords)

        if (!npc.isWithinDistance(target, 1)) {
            engageMelee(target)
            return
        }

        val openingShortCircuit =
            !state.openingShortCircuitUsed &&
                state.openingDamage > OPENING_SHORT_CIRCUIT_DAMAGE &&
                mapClock - state.fightStartTick <= OPENING_SHORT_CIRCUIT_TICKS
        val attacksBeforeSpecial =
            if (openingShortCircuit) OPENING_SHORT_CIRCUIT_ATTACKS else BASIC_ATTACKS_BEFORE_SPECIAL

        if (state.basicAttackCount >= attacksBeforeSpecial) {
            state.openingShortCircuitUsed = state.openingShortCircuitUsed || openingShortCircuit
            state.basicAttackCount = 0
            doSpecial(npc, target, state)
            return
        }

        state.basicAttackCount++
        melee(npc, target)
    }

    private fun StandardNpcAccess.melee(npc: Npc, target: Player) {
        npc.anim(MELEE_ANIM)
        val rolled = deps.random.of(MAX_BASIC_HIT + 1)
        val damage =
            when {
                target.adminGodMode -> 0
                rolled > 0 && target.protecting(DemonicElement.Melee) ->
                    maxOf(1, (rolled * PROTECT_MELEE_CHIP_PERCENT) / 100)
                else -> rolled
            }
        target.queueHit(
            source = npc,
            delay = 1,
            type = HitType.Melee,
            damage = damage,
            modifier = NoopPlayerHitModifier,
        )
    }

    private suspend fun StandardNpcAccess.doSpecial(
        npc: Npc,
        target: Player,
        state: DemonicBrutusState,
    ) {
        state.specialActive = true
        val restorePlayerCombat = target.isAttacking(npc)
        try {
            state.ensureLayout()
            if (state.nextSpecIsCharge) {
                state.nextSpecIsCharge = false
                demonicCharge(npc, target, state)
                state.toggleChargeSide()
            } else {
                state.nextSpecIsCharge = true
                demonicStomp(npc, target, state)
            }
        } finally {
            clearClones(state)
            state.specialActive = false
            if (target.isValidTarget() && npc.hitpoints > 0) {
                engageMelee(target)
                if (restorePlayerCombat) {
                    playerNpcInteractions.interact(target, npc, InteractionOp.Op2)
                }
            }
        }
    }

    private suspend fun StandardNpcAccess.demonicCharge(
        npc: Npc,
        target: Player,
        state: DemonicBrutusState,
    ) {
        say("*Growl*")
        val markedTile = target.coords
        val layoutTile = markedTile.coerceToDemonicArena()
        val profile = state.currentProfile(npc.hitpoints)
        val actors = prepareChargeActors(npc, markedTile, layoutTile, state, profile)

        for (actor in actors) {
            actor.npc.lockFacing(markedTile)
            actor.npc.spotanim(actor.element.launchSpotanim)
        }
        delay(profile.telegraphTicks)

        for (actor in actors) {
            if (npc.hitpoints <= 0 || !target.isValidTarget()) {
                return
            }

            if (!actor.real) {
                actor.npc.anim(actor.chargeAnim)
            }
            val path =
                if (actor.real) {
                    chargeAtTargetPath(actor.origin, NPC_SIZE, actor.direction, markedTile)
                } else {
                    chargePath(actor.origin, NPC_SIZE, actor.direction, CHARGE_DISTANCE, markedTile)
                }
            playChargePath(path, actor.element)
            damagePlayersIn(actor, state, npc, path.dangerTiles, stun = true)
            if (actor.real) {
                rushAtTarget(actor.npc, path.destination)
                npc.lockFacing(markedTile)
            }
            playMapSpotanim(actor.element.impactSpotanim, path.destination)
            delay(1)
        }
    }

    private suspend fun StandardNpcAccess.demonicStomp(
        npc: Npc,
        target: Player,
        state: DemonicBrutusState,
    ) {
        say("*Snort*")
        val markedTile = target.coords
        val layoutTile = markedTile.coerceToDemonicArena()
        val profile = state.currentProfile(npc.hitpoints)
        val actors = prepareStompActors(npc, markedTile, layoutTile, state, profile)

        for (actor in actors) {
            actor.npc.lockFacing(markedTile)
            actor.npc.spotanim(actor.element.launchSpotanim)
        }
        delay(profile.telegraphTicks)

        for (actor in actors) {
            if (npc.hitpoints <= 0 || !target.isValidTarget()) {
                return
            }

            actor.npc.anim(actor.stompAnim)
            val dangerTiles = playerCenteredStompTiles(markedTile, actor.direction, profile.clockwise)
            playStompImpacts(dangerTiles)
            damagePlayersIn(actor, state, npc, dangerTiles, stun = false)
            delay(STOMP_ACTOR_DELAY_TICKS)
        }
    }

    private fun StandardNpcAccess.prepareChargeActors(
        npc: Npc,
        markedTile: CoordGrid,
        layoutTile: CoordGrid,
        state: DemonicBrutusState,
        profile: SpecialProfile,
    ): List<SpecialActor> {
        clearClones(state)
        val elements = elements(profile.clockwise)
        val origins = chargeOrigins(layoutTile, state.axis, state.side, profile.clockwise, NPC_SIZE)

        npc.lockFacing(markedTile)
        val realDirection = directionFrom(npc.coords.translate(1, 1), markedTile)
        val real =
            SpecialActor(
                npc = npc,
                real = true,
                origin = npc.coords,
                direction = realDirection,
                element = elements[0],
                activeColor = 0,
            )

        val clones =
            origins.drop(1).mapIndexed { index, origin ->
                val clone = spawnClone(state, origin, profile.revealSequentially, index + 1)
                SpecialActor(
                    npc = clone,
                    real = false,
                    origin = origin,
                    direction = directionFromSide(state.side),
                    element = elements[(index + 1) % elements.size],
                    activeColor = (index + 1) and 1,
                )
            }

        return listOf(real) + clones
    }

    private fun StandardNpcAccess.prepareStompActors(
        npc: Npc,
        markedTile: CoordGrid,
        layoutTile: CoordGrid,
        state: DemonicBrutusState,
        profile: SpecialProfile,
    ): List<SpecialActor> {
        clearClones(state)
        npc.lockFacing(markedTile)
        val elements = elements(profile.clockwise)
        val real =
            SpecialActor(
                npc = npc,
                real = true,
                origin = npc.coords,
                direction = directionFrom(npc.coords.translate(1, 1), markedTile),
                element = elements[0],
                activeColor = 0,
            )
        val visualOrigins =
            stompOrigins(markedTile, profile.clockwise, NPC_SIZE)
                .sortedBy { origin -> origin.distanceSqTo(layoutTile) }
        val clones =
            visualOrigins.mapIndexed { index, origin ->
                val clone = spawnClone(state, origin, profile.revealSequentially, index + 1)
                SpecialActor(
                    npc = clone,
                    real = false,
                    origin = origin,
                    direction = directionFrom(origin.translate(1, 1), markedTile),
                    element = elements[(index + 1) % elements.size],
                    activeColor = (index + 1) and 1,
                )
            }
        return listOf(real) + clones
    }

    private fun StandardNpcAccess.prepareActors(
        npc: Npc,
        markedTile: CoordGrid,
        state: DemonicBrutusState,
        profile: SpecialProfile,
        origins: List<CoordGrid>,
        direction: (CoordGrid) -> GridStep,
    ): List<SpecialActor> {
        clearClones(state)
        val elements = elements(profile.clockwise)
        telejump(origins.first(), deps.collision)
        npc.lockFacing(markedTile)

        val real =
            SpecialActor(
                npc = npc,
                real = true,
                origin = npc.coords,
                direction = direction(npc.coords),
                element = elements[0],
                activeColor = 0,
            )

        val clones =
            origins.drop(1).mapIndexed { index, origin ->
                val clone = spawnClone(state, origin, profile.revealSequentially, index + 1)
                SpecialActor(
                    npc = clone,
                    real = false,
                    origin = origin,
                    direction = direction(origin),
                    element = elements[(index + 1) % elements.size],
                    activeColor = (index + 1) and 1,
                )
            }

        return listOf(real) + clones
    }

    private fun spawnClone(
        state: DemonicBrutusState,
        origin: CoordGrid,
        revealSequentially: Boolean,
        revealDelay: Int,
    ): Npc {
        val clone = Npc(GHOST_NPC, origin)
        clone.hideAllOps()
        clone.ignoreCombatInteractions = true
        clone.movementLocked = true
        clone.apRangeOverride = 0
        clone.apRequiresLineOfSight = false
        clone.setIdleAnim(GHOST_IDLE_ANIM)
        npcRepo.add(clone, CLONE_LIFETIME_TICKS)
        if (revealSequentially) {
            npcRepo.hide(clone, revealDelay)
        }
        state.clones += clone
        return clone
    }

    private fun damagePlayersIn(
        actor: SpecialActor,
        state: DemonicBrutusState,
        retaliationSource: Npc,
        dangerTiles: Set<CoordGrid>,
        stun: Boolean,
    ) {
        for (player in deps.playerList) {
            if (!player.isValidTarget() || player.coords !in dangerTiles) {
                continue
            }
            val failedTile = !player.safeFor(actor.activeColor, state)
            val failedPrayer = state.isPhaseTwo && !player.protecting(actor.element)
            if (!failedTile && !failedPrayer) {
                continue
            }

            player.spotanim(actor.element.impactSpotanim)
            player.queueHit(
                source = actor.npc,
                delay = 1,
                type = HitType.Typeless,
                damage = deps.random.of(MIN_SPECIAL_HIT, MAX_SPECIAL_HIT),
            )
            player.queueCombatRetaliate(retaliationSource, delay = 1)
            if (stun) {
                player.actionDelay = maxOf(player.actionDelay, player.currentMapClock + CHARGE_STUN_TICKS)
            }
        }
    }

    private suspend fun StandardNpcAccess.handleDeathQueue() {
        val state = states.getOrPut(npc.uid) { newState(npc) }
        if (!state.isPhaseTwo) {
            state.phaseTransitionActive = true
            state.isPhaseTwo = true
            state.basicAttackCount = 0
            state.openingShortCircuitUsed = true
            clearClones(state)
            clearQueue("queue.death")
            npc.baseHitpointsLvl = PHASE_TWO_HITPOINTS
            npc.hitpoints = PHASE_TWO_HITPOINTS
            npc.ignoreCombatInteractions = false
            npc.movementLocked = false
            npc.showAllOps()
            say("Moo moo!")
            npc.anim(SPAWN_ANIM)
            delay(PHASE_TRANSITION_TICKS)
            state.phaseTransitionActive = false
            return
        }

        cleanupState(npc)
        death.deathWithDrops(this, deathAnimOverride = DEATH_ANIM)
    }

    private fun playChargePath(path: DemonicChargePath, element: DemonicElement) {
        for ((tile, delay) in path.effects) {
            playMapSpotanim(element.pathSpotanim, tile, delay = delay)
        }
    }

    private fun playStompImpacts(tiles: Set<CoordGrid>) {
        for ((index, tile) in tiles.withIndex()) {
            val spot = STOMP_IMPACT_SPOTANIMS[index % STOMP_IMPACT_SPOTANIMS.size]
            playMapSpotanim(spot, tile)
        }
    }

    private suspend fun StandardNpcAccess.rushAtTarget(charger: Npc, destination: CoordGrid) {
        val distance = charger.coords.chebyshevDistance(destination)
        if (distance <= 0) {
            return
        }
        charger.tempMoveSpeed = MoveSpeed.Run
        charger.walk(destination)
        delay(((distance + 1) / 2).coerceAtLeast(1))
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

    private fun newState(npc: Npc) =
        DemonicBrutusState(
            fightStartTick = npc.currentMapClock,
            nextSpecIsCharge = deps.random.randomBoolean(),
        )

    private fun DemonicBrutusState.ensureLayout() {
        if (axis != Axis.Unset) {
            return
        }
        axis = if (deps.random.randomBoolean()) Axis.Horizontal else Axis.Vertical
        side =
            when (axis) {
                Axis.Horizontal -> if (deps.random.randomBoolean()) Side.West else Side.East
                Axis.Vertical -> if (deps.random.randomBoolean()) Side.South else Side.North
                Axis.Unset -> error("Layout axis was not set.")
            }
    }

    private fun DemonicBrutusState.toggleChargeSide() {
        side =
            when (side) {
                Side.West -> Side.East
                Side.East -> Side.West
                Side.South -> Side.North
                Side.North -> Side.South
                Side.Unset -> Side.Unset
            }
    }

    private fun DemonicBrutusState.currentProfile(hitpoints: Int): SpecialProfile =
        when {
            hitpoints > SLOW_PROFILE_HITPOINTS ->
                SpecialProfile(telegraphTicks = 4, clockwise = true, revealSequentially = false)
            hitpoints > ENRAGED_PROFILE_HITPOINTS ->
                SpecialProfile(telegraphTicks = 2, clockwise = true, revealSequentially = true)
            else ->
                SpecialProfile(telegraphTicks = 2, clockwise = false, revealSequentially = true)
        }

    private fun Player.safeFor(activeColor: Int, state: DemonicBrutusState): Boolean {
        val tileColor = (coords.x + coords.z) and 1
        return if (state.isPhaseTwo) tileColor != activeColor else tileColor == activeColor
    }

    private fun Player.protecting(element: DemonicElement): Boolean =
        vars[element.prayerVarbit] == 1

    private fun cleanupState(npc: Npc) {
        states.remove(npc.uid)?.let(::clearClones)
    }

    private fun clearClones(state: DemonicBrutusState) {
        for (clone in state.clones) {
            if (clone.isSlotAssigned) {
                npcRepo.del(clone, Int.MAX_VALUE)
            }
        }
        state.clones.clear()
    }

    private fun Npc.isDemonicBrutus(): Boolean =
        isType(DEMONIC_BRUTUS_NPC) || isVisType(DEMONIC_BRUTUS_NPC)

    private fun Player.isAttacking(npc: Npc): Boolean =
        (interaction as? InteractionNpc)?.uid == npc.uid

    private fun npcType(type: String): NpcServerType =
        ServerCacheManager.getNpc(type.asRSCM(RSCMType.NPC)) ?: error("Missing npc type: $type")

    private fun configureAggroRange(type: NpcServerType) {
        type.maxRange = maxOf(type.maxRange, INSTANCE_AGGRO_RANGE)
        type.huntRange = maxOf(type.huntRange, INSTANCE_AGGRO_RANGE)
        type.wanderRange = maxOf(type.wanderRange, INSTANCE_AGGRO_RANGE)
        type.giveChase = true
    }

    private companion object {
        private const val DEMONIC_BRUTUS_NPC = "npc.cowboss_hardmode"
        private const val GHOST_NPC = "npc.cowboss_hardmode_ghost"
        private const val COMBAT_CYCLE_HANDLER = "demonic_brutus.combat_cycle"

        private const val ATTACK_SPEED = 4
        private const val INSTANCE_AGGRO_RANGE = 64
        private const val NPC_SIZE = 3
        private const val PHASE_TWO_HITPOINTS = 487
        private const val SLOW_PROFILE_HITPOINTS = 500
        private const val ENRAGED_PROFILE_HITPOINTS = 250
        private const val PHASE_TRANSITION_TICKS = 2
        private const val BASIC_ATTACKS_BEFORE_SPECIAL = 3
        private const val OPENING_SHORT_CIRCUIT_ATTACKS = 2
        private const val OPENING_SHORT_CIRCUIT_TICKS = 10
        private const val OPENING_SHORT_CIRCUIT_DAMAGE = 100
        private const val MAX_BASIC_HIT = 4
        private const val PROTECT_MELEE_CHIP_PERCENT = 20
        private const val MIN_SPECIAL_HIT = 45
        private const val MAX_SPECIAL_HIT = 56
        private const val CHARGE_DISTANCE = 6
        private const val CHARGE_STUN_TICKS = 2
        private const val STOMP_ACTOR_DELAY_TICKS = 1
        private const val CLONE_LIFETIME_TICKS = 20

        private const val IDLE_ANIM = "seq.cow_boss_heavy_breath"
        private const val GHOST_IDLE_ANIM = "seq.cow_boss_heavy_breath"
        private const val MELEE_ANIM = "seq.cow_boss_attack"
        private const val CHARGE_ANIM = "seq.cow_boss_charge"
        private const val GHOST_CHARGE_ANIM = "seq.cow_boss_ghost_charge"
        private const val STOMP_ANIM = "seq.cow_boss_stomp"
        private const val GHOST_STOMP_ANIM = "seq.cow_boss_ghost_stomp"
        private const val SPAWN_ANIM = "seq.cow_boss_spawn"
        private const val DEATH_ANIM = "seq.cow_boss_death"

        private val STOMP_IMPACT_SPOTANIMS =
            listOf(
                "spotanim.vfx_cowboss_stomp_impact01",
                "spotanim.vfx_cowboss_stomp_impact02",
                "spotanim.vfx_cowboss_stomp_impact03",
            )
    }
}

private data class DemonicBrutusState(
    val fightStartTick: Int,
    var basicAttackCount: Int = 0,
    var openingDamage: Int = 0,
    var openingShortCircuitUsed: Boolean = false,
    var nextSpecIsCharge: Boolean,
    var isPhaseTwo: Boolean = false,
    var specialActive: Boolean = false,
    var phaseTransitionActive: Boolean = false,
    var axis: Axis = Axis.Unset,
    var side: Side = Side.Unset,
    val clones: MutableList<Npc> = mutableListOf(),
)

private const val DEMONIC_CHARGE_START_DISTANCE = 3
private const val DEMONIC_STOMP_GAP = 1
private const val DEMONIC_STOMP_REACH = 3
private const val DEMONIC_ARENA_MIN_LOCAL_X = 52
private const val DEMONIC_ARENA_MAX_LOCAL_X = 63
private const val DEMONIC_ARENA_MIN_LOCAL_Z = 18
private const val DEMONIC_ARENA_MAX_LOCAL_Z = 28

private data class SpecialProfile(
    val telegraphTicks: Int,
    val clockwise: Boolean,
    val revealSequentially: Boolean,
)

private data class SpecialActor(
    val npc: Npc,
    val real: Boolean,
    val origin: CoordGrid,
    val direction: GridStep,
    val element: DemonicElement,
    val activeColor: Int,
) {
    val chargeAnim: String
        get() = if (real) "seq.cow_boss_charge" else "seq.cow_boss_ghost_charge"

    val stompAnim: String
        get() = if (real) "seq.cow_boss_stomp" else "seq.cow_boss_ghost_stomp"
}

private enum class Axis {
    Unset,
    Horizontal,
    Vertical,
}

private enum class Side {
    Unset,
    West,
    East,
    South,
    North,
}

private enum class DemonicElement(
    val launchSpotanim: String,
    val pathSpotanim: String,
    val impactSpotanim: String,
    val prayerVarbit: String,
) {
    Melee(
        launchSpotanim = "spotanim.vfx_cowboss_hm_launch_melee_01",
        pathSpotanim = "spotanim.vfx_cowboss_hm_melee_01",
        impactSpotanim = "spotanim.vfx_cowboss_hm_impact_melee_01",
        prayerVarbit = "varbit.prayer_protectfrommelee",
    ),
    Ranged(
        launchSpotanim = "spotanim.vfx_cowboss_hm_launch_ranged_01",
        pathSpotanim = "spotanim.vfx_cowboss_hm_ranged_01",
        impactSpotanim = "spotanim.vfx_cowboss_hm_impact_ranged_01",
        prayerVarbit = "varbit.prayer_protectfrommissiles",
    ),
    Magic(
        launchSpotanim = "spotanim.vfx_cowboss_hm_launch_magic_01",
        pathSpotanim = "spotanim.vfx_cowboss_hm_magic_01",
        impactSpotanim = "spotanim.vfx_cowboss_hm_impact_magic_01",
        prayerVarbit = "varbit.prayer_protectfrommagic",
    ),
}

private data class GridStep(val dx: Int, val dz: Int)

private data class DemonicChargePath(
    val destination: CoordGrid,
    val effects: List<DemonicChargeEffectTile>,
    val dangerTiles: Set<CoordGrid>,
)

private data class DemonicChargeEffectTile(
    val tile: CoordGrid,
    val delay: Int,
)

private fun elements(clockwise: Boolean): List<DemonicElement> {
    val elements = listOf(DemonicElement.Melee, DemonicElement.Ranged, DemonicElement.Magic)
    return if (clockwise) elements else elements.asReversed()
}

private fun CoordGrid.coerceToDemonicArena(): CoordGrid {
    val baseX = x - lx
    val baseZ = z - lz
    return CoordGrid(
        x = baseX + lx.coerceIn(DEMONIC_ARENA_MIN_LOCAL_X, DEMONIC_ARENA_MAX_LOCAL_X),
        z = baseZ + lz.coerceIn(DEMONIC_ARENA_MIN_LOCAL_Z, DEMONIC_ARENA_MAX_LOCAL_Z),
        level = level,
    )
}

private fun CoordGrid.coerceNpcOriginToDemonicArena(size: Int): CoordGrid {
    val baseX = x - lx
    val baseZ = z - lz
    val maxLocalX = DEMONIC_ARENA_MAX_LOCAL_X - size + 1
    val maxLocalZ = DEMONIC_ARENA_MAX_LOCAL_Z - size + 1
    return CoordGrid(
        x = baseX + lx.coerceIn(DEMONIC_ARENA_MIN_LOCAL_X, maxLocalX),
        z = baseZ + lz.coerceIn(DEMONIC_ARENA_MIN_LOCAL_Z, maxLocalZ),
        level = level,
    )
}

private fun CoordGrid.inDemonicArena(): Boolean =
    lx in DEMONIC_ARENA_MIN_LOCAL_X..DEMONIC_ARENA_MAX_LOCAL_X &&
        lz in DEMONIC_ARENA_MIN_LOCAL_Z..DEMONIC_ARENA_MAX_LOCAL_Z

private fun chargeOrigins(
    markedTile: CoordGrid,
    axis: Axis,
    side: Side,
    clockwise: Boolean,
    size: Int,
): List<CoordGrid> {
    val offsets = if (clockwise) listOf(-3, -1, 1, 3) else listOf(3, 1, -1, -3)
    return when (axis) {
        Axis.Horizontal ->
            offsets.map { offset ->
                val x =
                    when (side) {
                        Side.West -> markedTile.x - DEMONIC_CHARGE_START_DISTANCE
                        Side.East -> markedTile.x + DEMONIC_CHARGE_START_DISTANCE
                        else -> markedTile.x - DEMONIC_CHARGE_START_DISTANCE
                }
                CoordGrid(x = x, z = markedTile.z + offset - 1, level = markedTile.level)
                    .coerceNpcOriginToDemonicArena(size)
            }.distinct()
        Axis.Vertical ->
            offsets.map { offset ->
                val z =
                    when (side) {
                        Side.South -> markedTile.z - DEMONIC_CHARGE_START_DISTANCE
                        Side.North -> markedTile.z + DEMONIC_CHARGE_START_DISTANCE
                        else -> markedTile.z - DEMONIC_CHARGE_START_DISTANCE
                }
                CoordGrid(x = markedTile.x + offset - 1, z = z, level = markedTile.level)
                    .coerceNpcOriginToDemonicArena(size)
            }.distinct()
        Axis.Unset -> error("Charge layout axis was not set.")
    }
}

private fun stompOrigins(markedTile: CoordGrid, clockwise: Boolean, size: Int): List<CoordGrid> {
    val sides =
        if (clockwise) {
            listOf(GridStep(-1, 0), GridStep(0, 1), GridStep(1, 0), GridStep(0, -1))
        } else {
            listOf(GridStep(-1, 0), GridStep(0, -1), GridStep(1, 0), GridStep(0, 1))
        }
    val centerOffset = size / 2
    return sides.map { side ->
        when {
            side.dx < 0 ->
                CoordGrid(
                    x = markedTile.x - size - DEMONIC_STOMP_GAP,
                    z = markedTile.z - centerOffset,
                    level = markedTile.level,
                )
            side.dx > 0 ->
                CoordGrid(
                    x = markedTile.x + DEMONIC_STOMP_GAP + 1,
                    z = markedTile.z - centerOffset,
                    level = markedTile.level,
                )
            side.dz < 0 ->
                CoordGrid(
                    x = markedTile.x - centerOffset,
                    z = markedTile.z - size - DEMONIC_STOMP_GAP,
                    level = markedTile.level,
                )
            else ->
                CoordGrid(
                    x = markedTile.x - centerOffset,
                    z = markedTile.z + DEMONIC_STOMP_GAP + 1,
                    level = markedTile.level,
                )
        }
    }
}

private fun directionFromSide(side: Side): GridStep =
    when (side) {
        Side.West -> GridStep(1, 0)
        Side.East -> GridStep(-1, 0)
        Side.South -> GridStep(0, 1)
        Side.North -> GridStep(0, -1)
        Side.Unset -> GridStep(1, 0)
    }

private fun directionFrom(source: CoordGrid, target: CoordGrid): GridStep {
    val dx = target.x - source.x
    val dz = target.z - source.z
    return when {
        dx == 0 && dz == 0 -> GridStep(0, 1)
        kotlin.math.abs(dx) >= kotlin.math.abs(dz) -> GridStep(dx.signStep(), 0)
        else -> GridStep(0, dz.signStep())
    }
}

private fun chargePath(
    origin: CoordGrid,
    size: Int,
    direction: GridStep,
    distance: Int,
    markedTile: CoordGrid,
): DemonicChargePath {
    val destination =
        origin.translate(direction.dx * distance, direction.dz * distance)
            .coerceNpcOriginToDemonicArena(size)
    val effects =
        (chargeLane(origin, size, direction, distance).filter { it.tile.inDemonicArena() } +
            playerCenteredChargeLane(markedTile, direction))
            .distinctBy { it.tile }
    val dangerTiles = effects.mapTo(mutableSetOf()) { it.tile }
    dangerTiles += markedTile
    return DemonicChargePath(destination, effects, dangerTiles)
}

private fun chargeAtTargetPath(
    origin: CoordGrid,
    size: Int,
    direction: GridStep,
    markedTile: CoordGrid,
): DemonicChargePath {
    val destination = chargeDestinationBesideTarget(origin, size, direction, markedTile)
    val distance = origin.chebyshevDistance(destination).coerceAtLeast(1)
    val effects =
        (chargeLane(origin, size, direction, distance).filter { it.tile.inDemonicArena() } +
            playerCenteredChargeLane(markedTile, direction))
            .distinctBy { it.tile }
    val dangerTiles = effects.mapTo(mutableSetOf()) { it.tile }
    dangerTiles += markedTile
    return DemonicChargePath(destination, effects, dangerTiles)
}

private fun chargeDestinationBesideTarget(
    origin: CoordGrid,
    size: Int,
    direction: GridStep,
    markedTile: CoordGrid,
): CoordGrid =
    when {
            direction.dx > 0 ->
                CoordGrid(
                    x = markedTile.x - size,
                    z = origin.z.alignOriginTo(markedTile.z, size),
                    level = markedTile.level,
                )
            direction.dx < 0 ->
                CoordGrid(
                    x = markedTile.x + 1,
                    z = origin.z.alignOriginTo(markedTile.z, size),
                    level = markedTile.level,
                )
            direction.dz > 0 ->
                CoordGrid(
                    x = origin.x.alignOriginTo(markedTile.x, size),
                    z = markedTile.z - size,
                    level = markedTile.level,
                )
            direction.dz < 0 ->
                CoordGrid(
                    x = origin.x.alignOriginTo(markedTile.x, size),
                    z = markedTile.z + 1,
                    level = markedTile.level,
                )
            else -> origin
        }
        .coerceNpcOriginToDemonicArena(size)

private fun Int.alignOriginTo(target: Int, size: Int): Int =
    when {
        target < this -> target
        target > this + size - 1 -> target - size + 1
        else -> this
    }

private fun chargeLane(
    origin: CoordGrid,
    size: Int,
    direction: GridStep,
    distance: Int,
): List<DemonicChargeEffectTile> =
    buildList {
        for (step in 1..distance) {
            val delay = step - 1
            when {
                direction.dx > 0 ->
                    repeat(size) { offset ->
                        add(DemonicChargeEffectTile(origin.translate(size + step - 1, offset), delay))
                    }
                direction.dx < 0 ->
                    repeat(size) { offset ->
                        add(DemonicChargeEffectTile(origin.translate(-step, offset), delay))
                    }
                direction.dz > 0 ->
                    repeat(size) { offset ->
                        add(DemonicChargeEffectTile(origin.translate(offset, size + step - 1), delay))
                    }
                direction.dz < 0 ->
                    repeat(size) { offset ->
                        add(DemonicChargeEffectTile(origin.translate(offset, -step), delay))
                    }
            }
        }
    }

private fun lShapeDangerTiles(
    origin: CoordGrid,
    direction: GridStep,
    markedTile: CoordGrid,
    clockwise: Boolean,
): Set<CoordGrid> {
    val center = origin.translate(1, 1)
    val side = direction.perpendicular(clockwise)
    return buildSet {
        for (distance in 0..DEMONIC_STOMP_REACH) {
            add(center.translate(direction.dx * distance, direction.dz * distance))
            add(center.translate(side.dx * distance, side.dz * distance))
        }
        add(markedTile)
        add(center.translate(direction.dx + side.dx, direction.dz + side.dz))
        add(center.translate((direction.dx * 2) + side.dx, (direction.dz * 2) + side.dz))
    }
}

private fun playerCenteredChargeLane(
    markedTile: CoordGrid,
    direction: GridStep,
): List<DemonicChargeEffectTile> =
    buildList {
        val side = direction.perpendicular(clockwise = true)
        for (forward in -2..3) {
            for (width in -1..1) {
                val tile =
                    markedTile.translate(
                        (direction.dx * forward) + (side.dx * width),
                        (direction.dz * forward) + (side.dz * width),
                    )
                add(DemonicChargeEffectTile(tile, delay = 0))
            }
        }
    }

private fun playerCenteredStompTiles(
    markedTile: CoordGrid,
    direction: GridStep,
    clockwise: Boolean,
): Set<CoordGrid> {
    val side = direction.perpendicular(clockwise)
    return buildSet {
        add(markedTile)
        add(markedTile.translate(side.dx, side.dz))
        add(markedTile.translate(-side.dx, -side.dz))
        for (distance in 1..DEMONIC_STOMP_REACH) {
            add(markedTile.translate(direction.dx * distance, direction.dz * distance))
        }
        add(markedTile.translate(direction.dx + side.dx, direction.dz + side.dz))
        add(markedTile.translate(direction.dx - side.dx, direction.dz - side.dz))
    }
}

private fun CoordGrid.distanceSqTo(other: CoordGrid): Int {
    val dx = x - other.x
    val dz = z - other.z
    return (dx * dx) + (dz * dz)
}

private fun GridStep.perpendicular(clockwise: Boolean): GridStep =
    if (clockwise) GridStep(dz, -dx) else GridStep(-dz, dx)

private fun Int.signStep(): Int =
    when {
        this > 0 -> 1
        this < 0 -> -1
        else -> 0
    }
