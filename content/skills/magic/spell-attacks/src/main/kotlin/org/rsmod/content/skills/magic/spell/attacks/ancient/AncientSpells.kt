package org.rsmod.content.skills.magic.spell.attacks.ancient

import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.CombatEffects
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.spells.attack.SpellAttack
import org.rsmod.api.spells.attack.SpellAttackManager
import org.rsmod.api.spells.attack.SpellAttackMap
import org.rsmod.api.spells.attack.SpellAttackRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.type.getOrNull
import org.rsmod.map.zone.ZoneKey

class AncientSpells @Inject constructor(private val npcRepo: NpcRepository) : SpellAttackMap {
    override fun SpellAttackRepository.register(manager: SpellAttackManager) {
        registerSmokeSpells(manager)
        registerShadowSpells(manager)
        registerBloodSpells(manager)
        registerIceSpells(manager)
    }

    private fun SpellAttackRepository.registerSmokeSpells(manager: SpellAttackManager) {
        registerAncient(manager, "obj.50_smoke_rush", 13, AncientEffect.Smoke(poisonDamage = 2), Fx.FireBlast)
        registerAncient(manager, "obj.62_smoke_burst", 17, AncientEffect.Smoke(poisonDamage = 2), Fx.FireBlast, areaRadius = 1)
        registerAncient(manager, "obj.74_smoke_blitz", 23, AncientEffect.Smoke(poisonDamage = 4), Fx.FireWave)
        registerAncient(manager, "obj.86_smoke_barrage", 27, AncientEffect.Smoke(poisonDamage = 4), Fx.FireWave, areaRadius = 1)
    }

    private fun SpellAttackRepository.registerShadowSpells(manager: SpellAttackManager) {
        registerAncient(manager, "obj.52_shadow_rush", 14, AncientEffect.Shadow(drainAmount = 1), Fx.EarthBlast)
        registerAncient(manager, "obj.64_shadow_burst", 18, AncientEffect.Shadow(drainAmount = 2), Fx.EarthBlast, areaRadius = 1)
        registerAncient(manager, "obj.76_shadow_blitz", 24, AncientEffect.Shadow(drainAmount = 3), Fx.EarthWave)
        registerAncient(manager, "obj.88_shadow_barrage", 28, AncientEffect.Shadow(drainAmount = 4), Fx.EarthWave, areaRadius = 1)
    }

    private fun SpellAttackRepository.registerBloodSpells(manager: SpellAttackManager) {
        registerAncient(manager, "obj.56_blood_rush", 15, AncientEffect.Blood, Fx.WaterBlast)
        registerAncient(manager, "obj.68_blood_burst", 21, AncientEffect.Blood, Fx.WaterBlast, areaRadius = 1)
        registerAncient(manager, "obj.80_blood_blitz", 25, AncientEffect.Blood, Fx.WaterWave)
        registerAncient(manager, "obj.92_blood_barrage", 29, AncientEffect.Blood, Fx.WaterWave, areaRadius = 1)
    }

    private fun SpellAttackRepository.registerIceSpells(manager: SpellAttackManager) {
        registerAncient(manager, "obj.58_ice_rush", 16, AncientEffect.Ice(freezeTicks = 8), Fx.WaterBlast)
        registerAncient(manager, "obj.70_ice_burst", 22, AncientEffect.Ice(freezeTicks = 16), Fx.WaterBlast, areaRadius = 1)
        registerAncient(manager, "obj.82_ice_blitz", 26, AncientEffect.Ice(freezeTicks = 24), Fx.WaterWave)
        registerAncient(manager, "obj.94_ice_barrage", 30, AncientEffect.Ice(freezeTicks = 32), Fx.WaterWave, areaRadius = 1)
    }

    private fun SpellAttackRepository.registerAncient(
        manager: SpellAttackManager,
        spell: String,
        maxHit: Int,
        effect: AncientEffect,
        fx: Fx,
        areaRadius: Int = 0,
    ) {
        register(
            spell = spell,
            attack =
                AncientSpellAttack(
                    npcRepo = npcRepo,
                    manager = manager,
                    maxHit = maxHit,
                    effect = effect,
                    areaRadius = areaRadius,
                    staffAnim = "seq.human_castwave_staff",
                    unarmedAnim = "seq.human_castwave",
                    launch = fx.launch,
                    travel = fx.travel,
                    impact = fx.impact,
                    castSound = fx.castSound,
                    hitSound = fx.hitSound,
                ),
        )
    }

    private class AncientSpellAttack(
        private val npcRepo: NpcRepository,
        private val manager: SpellAttackManager,
        private val maxHit: Int,
        private val effect: AncientEffect,
        private val areaRadius: Int,
        private val staffAnim: String,
        private val unarmedAnim: String,
        private val launch: String,
        private val travel: String,
        private val impact: String,
        private val castSound: String,
        private val hitSound: String,
    ) : SpellAttack {
        override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Spell) {
            val context =
                castPrimary(target = target, attack = attack, targetHitpoints = target.hitpoints)
            if (context != null) {
                castNearbyNpcs(primary = target, attack = attack, context = context)
                manager.continueCombatIfAutocast(this, target)
            }
        }

        override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Spell) {
            val context =
                castPrimary(
                    target = target,
                    attack = attack,
                    targetHitpoints = target.hitpoints,
                ) { damage ->
                    applyPlayerEffect(target, damage)
                }
            if (context != null) {
                manager.continueCombatIfAutocast(this, target)
            }
        }

        private fun ProtectedAccess.castPrimary(
            target: PathingEntity,
            attack: CombatAttack.Spell,
            targetHitpoints: Int,
            applyEffect: (ProtectedAccess.(Int) -> Unit)? = null,
        ): CastContext? {
            val castResult = manager.attemptCast(this, attack)
            if (castResult.isFailure()) {
                return null
            }

            val weaponType = getOrNull(attack.weapon)
            anim(weaponType.castAnim())
            spotanim(launch, height = 92)

            val proj = manager.spawnProjectile(this, target, travel, "projanim.magic_spell")
            val (serverDelay, clientDelay) = proj.durations
            val spell = attack.spell.obj

            resolveHit(
                target = target,
                attack = attack,
                castResult = castResult,
                spell = spell,
                clientDelay = clientDelay,
                serverDelay = serverDelay,
                targetHitpoints = targetHitpoints,
                applyEffect = applyEffect,
            )
            return CastContext(castResult, spell, clientDelay, serverDelay)
        }

        private fun ProtectedAccess.castNearbyNpcs(
            primary: Npc,
            attack: CombatAttack.Spell,
            context: CastContext,
        ) {
            if (areaRadius <= 0) {
                return
            }
            npcRepo
                .findAll(ZoneKey.from(primary.coords), zoneRadius = 1)
                .filter { it !== primary }
                .filter { it.isValidTarget() }
                .filter { it.isWithinDistance(primary, areaRadius) }
                .take(MaxSecondaryTargets)
                .forEach { target ->
                    resolveHit(
                        target = target,
                        attack = attack,
                        castResult = context.castResult,
                        spell = context.spell,
                        clientDelay = context.clientDelay,
                        serverDelay = context.serverDelay,
                        targetHitpoints = target.hitpoints,
                    )
                }
        }

        private fun ProtectedAccess.resolveHit(
            target: PathingEntity,
            attack: CombatAttack.Spell,
            castResult: MagicRuneManager.CastResult,
            spell: ItemServerType,
            clientDelay: Int,
            serverDelay: Int,
            targetHitpoints: Int,
            applyEffect: (ProtectedAccess.(Int) -> Unit)? = null,
        ) {
            val splash = manager.rollSplash(this, target, attack, castResult)
            if (splash) {
                manager.playSplashFx(this, target, clientDelay, castSound, soundRadius = 8)
                manager.queueSplashHit(this, target, spell, clientDelay, serverDelay)
                return
            }

            val damage = manager.rollMaxHit(this, target, attack, castResult, maxHit)
            val cappedDamage = minOf(damage, targetHitpoints)
            manager.playHitFx(
                source = this,
                target = target,
                clientDelay = clientDelay,
                castSound = castSound,
                soundRadius = 8,
                hitSpot = impact,
                hitSpotHeight = 124,
                hitSound = hitSound,
            )
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMagicHit(this, target, spell, damage, clientDelay, serverDelay)
            healFromBloodSpell(cappedDamage)
            applyEffect?.invoke(this, cappedDamage)
        }

        private fun ProtectedAccess.healFromBloodSpell(damage: Int) {
            if (effect !is AncientEffect.Blood || damage <= 0) {
                return
            }
            val heal = damage / 4
            if (heal > 0) {
                statHeal("stat.hitpoints", constant = heal, percent = 0)
            }
        }

        private fun ProtectedAccess.applyPlayerEffect(target: Player, damage: Int) {
            if (damage <= 0) {
                return
            }
            when (val spellEffect = effect) {
                is AncientEffect.Blood -> Unit
                is AncientEffect.Ice -> CombatEffects.freeze(target, spellEffect.freezeTicks)
                is AncientEffect.Shadow -> {
                    CombatEffects.statDrain(target, listOf("stat.attack"), spellEffect.drainAmount)
                }
                is AncientEffect.Smoke -> CombatEffects.poison(target, spellEffect.poisonDamage)
            }
        }

        private fun ItemServerType?.castAnim(): String =
            if (this != null && isCategoryType("category.staff")) {
                staffAnim
            } else {
                unarmedAnim
            }

        private data class CastContext(
            val castResult: MagicRuneManager.CastResult,
            val spell: ItemServerType,
            val clientDelay: Int,
            val serverDelay: Int,
        )

        private companion object {
            private const val MaxSecondaryTargets: Int = 8
        }
    }

    private sealed interface AncientEffect {
        data class Smoke(val poisonDamage: Int) : AncientEffect

        data class Shadow(val drainAmount: Int) : AncientEffect

        data object Blood : AncientEffect

        data class Ice(val freezeTicks: Int) : AncientEffect
    }

    private data class Fx(
        val launch: String,
        val travel: String,
        val impact: String,
        val castSound: String,
        val hitSound: String,
    ) {
        companion object {
            val FireBlast =
                Fx(
                    launch = "spotanim.fireblast_casting",
                    travel = "spotanim.fireblast_travel",
                    impact = "spotanim.fireblast_impact",
                    castSound = "synth.fireblast_cast_and_fire",
                    hitSound = "synth.fireblast_hit",
                )

            val FireWave =
                Fx(
                    launch = "spotanim.firewave_casting",
                    travel = "spotanim.firewave_travel",
                    impact = "spotanim.firewave_impact",
                    castSound = "synth.firewave_cast_and_fire",
                    hitSound = "synth.firewave_hit",
                )

            val EarthBlast =
                Fx(
                    launch = "spotanim.earthblast_casting",
                    travel = "spotanim.earthblast_travel",
                    impact = "spotanim.earthblast_impact",
                    castSound = "synth.earthblast_cast_and_fire",
                    hitSound = "synth.earthblast_hit",
                )

            val EarthWave =
                Fx(
                    launch = "spotanim.earthwave_casting",
                    travel = "spotanim.earthwave_travel",
                    impact = "spotanim.earthwave_impact",
                    castSound = "synth.earthwave_cast_and_fire",
                    hitSound = "synth.earthwave_hit",
                )

            val WaterBlast =
                Fx(
                    launch = "spotanim.waterblast_casting",
                    travel = "spotanim.waterblast_travel",
                    impact = "spotanim.waterblast_impact",
                    castSound = "synth.waterblast_cast_and_fire",
                    hitSound = "synth.waterblast_hit",
                )

            val WaterWave =
                Fx(
                    launch = "spotanim.waterwave_casting",
                    travel = "spotanim.waterwave_travel",
                    impact = "spotanim.waterwave_impact",
                    castSound = "synth.waterwave_cast_and_fire",
                    hitSound = "synth.waterwave_hit",
                )
        }
    }
}
