package org.rsmod.content.other.special.attacks.melee

import jakarta.inject.Inject
import kotlin.math.max
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.CombatEffects
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.config.constants
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.player.PlayerRepository
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.api.specials.instant.InstantSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.map.zone.ZoneKey

class CommonMeleeSpecialAttacks
@Inject
constructor(
    private val npcRepo: NpcRepository,
    private val playerRepo: PlayerRepository,
) : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        for (obj in DragonDaggers) {
            registerMelee(obj, DoubleHit(manager, DragonDaggerSpec))
        }
        for (obj in AbyssalDaggers) {
            registerMelee(obj, DoubleHit(manager, AbyssalDaggerSpec))
        }
        for (obj in BountyAbyssalDaggers) {
            registerMelee(obj, DoubleHit(manager, BountyAbyssalDaggerSpec))
        }
        for (obj in Whips) {
            registerMelee(obj, SingleHit(manager, WhipSpec))
        }
        for (obj in TentacleWhips) {
            registerMelee(obj, SingleHit(manager, TentacleSpec))
        }
        for (obj in DragonScimitars) {
            registerMelee(obj, SingleHit(manager, DragonScimitarSpec))
        }
        for (obj in DragonWarhammers) {
            registerMelee(obj, SingleHit(manager, DragonWarhammerSpec))
        }
        for (obj in StatiusWarhammers) {
            registerMelee(obj, SingleHit(manager, StatiusWarhammerSpec))
        }
        for (obj in BountyStatiusWarhammers) {
            registerMelee(obj, SingleHit(manager, BountyStatiusWarhammerSpec))
        }
        for (obj in ArkanBlades) {
            registerMelee(obj, SingleHit(manager, ArkanBladeSpec))
        }
        for (obj in FrostmoonSpears) {
            registerMelee(obj, SingleHit(manager, FrostmoonSpearSpec))
        }
        for (obj in CrimsonKistens) {
            registerMelee(obj, BrutalSwing(manager, CrimsonKistenSpec))
        }
        for (obj in AncientMaces) {
            registerMelee(obj, SingleHit(manager, AncientMaceSpec))
        }
        for (obj in DragonMaces) {
            registerMelee(obj, SingleHit(manager, DragonMaceSpec))
        }
        for (obj in BoneDaggers) {
            registerMelee(obj, SingleHit(manager, BoneDaggerSpec))
        }
        for (obj in EchoCrystalDaggers) {
            registerMelee(obj, SingleHit(manager, EchoCrystalDaggerSpec))
        }
        for (obj in DragonCandleDaggers) {
            registerMelee(obj, SingleHit(manager, DragonCandleDaggerSpec))
        }
        for (obj in BrineSabres) {
            registerMelee(obj, SingleHit(manager, BrineSabreSpec))
        }
        for (obj in FangOfTheHounds) {
            registerMelee(obj, SingleHit(manager, FangOfTheHoundSpec))
        }
        for (obj in InfernalTecpatls) {
            registerMelee(obj, SingleHit(manager, InfernalTecpatlSpec))
        }
        for (obj in DualMacuahuitls) {
            registerMelee(obj, BloodInfusion(manager, DualMacuahuitlSpec))
        }
        for (obj in DragonSwords) {
            registerMelee(obj, SingleHit(manager, DragonSwordSpec))
        }
        for (obj in VestasLongswords) {
            registerMelee(obj, SingleHit(manager, VestasLongswordSpec))
        }
        for (obj in DragonTwoHanders) {
            registerMelee(obj, SingleHit(manager, DragonTwoHanderSpec))
        }
        for (obj in DinhsBulwarks) {
            registerMelee(obj, AreaMeleeHit(manager, npcRepo, playerRepo, DinhsBulwarkSpec, radius = 5))
        }
        for (obj in DragonHalberds) {
            registerMelee(obj, Sweep(manager, DragonHalberdSpec))
        }
        for (obj in CrystalHalberds) {
            registerMelee(obj, Sweep(manager, CrystalHalberdSpec))
        }
        for (obj in DragonClaws) {
            registerMelee(obj, Claws(manager))
        }
        for (obj in RuneClaws) {
            registerMelee(obj, SingleHit(manager, RuneClawsSpec))
        }
        for (obj in Arclights) {
            registerMelee(obj, SingleHit(manager, ArclightSpec))
        }
        for (obj in Voidwakers) {
            registerMelee(obj, Voidwaker(manager))
        }
        for (obj in ShoveSpears) {
            registerMelee(obj, Shove(manager))
        }
        for (obj in DragonHastas) {
            registerMelee(obj, DragonHasta(manager))
        }
        for (obj in DragonBattleaxes) {
            registerInstant(obj, DragonBattleaxe)
        }
        for (obj in GraniteMauls) {
            registerMelee(obj, SingleHit(manager, GraniteMaulSpec))
        }
        for (obj in GraniteHammers) {
            registerMelee(obj, FlatBonusHit(manager, GraniteHammerSpec, extraDamage = 5))
        }
        for (obj in ElderMauls) {
            registerMelee(obj, SingleHit(manager, ElderMaulSpec))
        }
        for (obj in BarrelchestAnchors) {
            registerMelee(obj, SingleHit(manager, BarrelchestAnchorSpec))
        }
        for (obj in AbyssalBludgeons) {
            registerMelee(obj, SingleHit(manager, AbyssalBludgeonSpec))
        }
        for (obj in NoxiousHalberds) {
            registerMelee(obj, SingleHit(manager, NoxiousHalberdSpec))
        }
        for (obj in UrsineChainmaces) {
            registerMelee(obj, SingleHit(manager, UrsineChainmaceSpec))
        }
        for (obj in SaradominSwords) {
            registerMelee(obj, SaradominSword(manager, blessed = false))
        }
        for (obj in BlessedSaradominSwords) {
            registerMelee(obj, SaradominSword(manager, blessed = true))
        }
        for (obj in OsmumtensFangs) {
            registerMelee(obj, ExactMaxHit(manager, OsmumtensFangSpec))
        }
        for (obj in KerisPartisansOfCorruption) {
            registerMelee(obj, SingleHit(manager, KerisPartisanCorruptionSpec))
        }
        for (obj in KerisPartisansOfTheSun) {
            registerMelee(obj, KerisPartisanSun(manager))
        }
        for (obj in RetainerWeapons) {
            registerMelee(obj, Retainer(manager))
        }
        for (obj in Soulreapers) {
            registerMelee(obj, SoulreaperBehead(manager))
        }
        for (obj in SoulflameHorns) {
            registerInstant(obj, SoulflameHorn)
        }
        for (obj in Sunspears) {
            registerMelee(obj, SeekingLunge(manager, SunspearSpec))
        }
        for (obj in ThunderKhopeshes) {
            registerMelee(obj, DoubleHit(manager, ThunderKhopeshSpec))
        }
        for (obj in DeadmanThunderKhopeshes) {
            registerMelee(obj, DoubleHit(manager, DeadmanThunderKhopeshSpec))
        }
        for (obj in SunlightSpears) {
            registerMelee(obj, AreaMeleeHit(manager, npcRepo, playerRepo, SunlightSpearSpec, radius = 3))
        }
        for (obj in VestaSpears) {
            registerMelee(
                obj,
                AreaMeleeHit(manager, npcRepo, playerRepo, VestaSpearSpec, radius = 1, maxTargets = 16, damageScale = 0.5),
            )
        }
        for (obj in BountyVestaSpears) {
            registerMelee(obj, VestaSpearBounty(manager))
        }
        for (obj in BurningClaws) {
            registerMelee(obj, BurningClawBarrage(manager))
        }
        for (obj in Excaliburs) {
            registerInstant(obj, Excalibur)
        }
    }

    private class SingleHit(
        private val manager: SpecialAttackManager,
        private val spec: SpecFx,
    ) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            swing(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            swing(target, attack)
            return true
        }

        private fun ProtectedAccess.swing(target: PathingEntity, attack: CombatAttack.Melee) {
            playSpecFx(spec)
            val damage =
                manager.rollMeleeDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = spec.accuracyMultiplier,
                    maxHitMultiplier = spec.maxHitMultiplier,
                    blockType = spec.blockType,
                )
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(this, target, damage)
            spec.onHit(this, target, damage)
            if (spec.nextAttackDelay != null) {
                manager.setNextAttackDelay(this, spec.nextAttackDelay)
            }
            manager.continueCombat(this, target)
        }
    }

    private class DoubleHit(
        private val manager: SpecialAttackManager,
        private val spec: SpecFx,
    ) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            swing(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            swing(target, attack)
            return true
        }

        private fun ProtectedAccess.swing(target: PathingEntity, attack: CombatAttack.Melee) {
            playSpecFx(spec)
            val first = roll(target, attack)
            val second = roll(target, attack)
            manager.giveCombatXp(this, target, attack, first + second)
            manager.queueMeleeHit(this, target, first)
            manager.queueMeleeHit(this, target, second)
            spec.onHit(this, target, first + second)
            manager.continueCombat(this, target)
        }

        private fun ProtectedAccess.roll(target: PathingEntity, attack: CombatAttack.Melee): Int =
            manager.rollMeleeDamage(
                source = this,
                target = target,
                attack = attack,
                accuracyMultiplier = spec.accuracyMultiplier,
                maxHitMultiplier = spec.maxHitMultiplier,
                blockType = spec.blockType,
            )
    }

    private class Sweep(
        private val manager: SpecialAttackManager,
        private val spec: SpecFx,
    ) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            swing(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            swing(target, attack)
            return true
        }

        private fun ProtectedAccess.swing(target: PathingEntity, attack: CombatAttack.Melee) {
            playSpecFx(spec)
            val hits = if (target.size > 1) 2 else 1
            var total = 0
            repeat(hits) {
                val damage =
                    manager.rollMeleeDamage(
                        source = this,
                        target = target,
                        attack = attack,
                        accuracyMultiplier = spec.accuracyMultiplier,
                        maxHitMultiplier = spec.maxHitMultiplier,
                        blockType = spec.blockType,
                    )
                total += damage
                manager.queueMeleeHit(this, target, damage)
            }
            manager.giveCombatXp(this, target, attack, total)
            spec.onHit(this, target, total)
            manager.continueCombat(this, target)
        }
    }

    private class FlatBonusHit(
        private val manager: SpecialAttackManager,
        private val spec: SpecFx,
        private val extraDamage: Int,
    ) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            swing(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            swing(target, attack)
            return true
        }

        private fun ProtectedAccess.swing(target: PathingEntity, attack: CombatAttack.Melee) {
            playSpecFx(spec)
            val damage =
                manager.rollMeleeDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = spec.accuracyMultiplier,
                    maxHitMultiplier = spec.maxHitMultiplier,
                    blockType = spec.blockType,
                )
            val boosted = if (damage > 0) damage + extraDamage else 0
            manager.giveCombatXp(this, target, attack, boosted)
            manager.queueMeleeHit(this, target, boosted)
            spec.onHit(this, target, boosted)
            manager.continueCombat(this, target)
        }
    }

    private class ExactMaxHit(
        private val manager: SpecialAttackManager,
        private val spec: SpecFx,
    ) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            strike(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            strike(target, attack)
            return true
        }

        private fun ProtectedAccess.strike(target: PathingEntity, attack: CombatAttack.Melee) {
            playSpecFx(spec)
            val accurate =
                manager.rollMeleeAccuracy(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    blockType = spec.blockType ?: attack.type,
                    multiplier = spec.accuracyMultiplier,
                )
            val damage =
                if (accurate) {
                    manager.calculateMeleeMaxHit(
                        source = this,
                        target = target,
                        attackType = attack.type,
                        attackStyle = attack.style,
                        multiplier = spec.maxHitMultiplier,
                    )
                } else {
                    0
                }
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(this, target, damage)
            spec.onHit(this, target, damage)
            if (spec.nextAttackDelay != null) {
                manager.setNextAttackDelay(this, spec.nextAttackDelay)
            }
            manager.continueCombat(this, target)
        }
    }

    private class BrutalSwing(
        private val manager: SpecialAttackManager,
        private val spec: SpecFx,
    ) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            swing(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            swing(target, attack)
            return true
        }

        private fun ProtectedAccess.swing(target: PathingEntity, attack: CombatAttack.Melee) {
            playSpecFx(spec)
            val landed =
                (1..4).count {
                    manager.rollMeleeAccuracy(
                        source = this,
                        target = target,
                        attackType = attack.type,
                        attackStyle = attack.style,
                        blockType = spec.blockType ?: attack.type,
                        multiplier = spec.accuracyMultiplier,
                    )
                }
            val damage =
                if (landed == 0) {
                    0
                } else {
                    val maxHit =
                        manager.calculateMeleeMaxHit(
                            source = this,
                            target = target,
                            attackType = attack.type,
                            attackStyle = attack.style,
                            multiplier = spec.maxHitMultiplier,
                        )
                    val minRoll = (maxHit * (50 + landed * 20)) / 100
                    val maxRoll = (maxHit * (90 + landed * 20)) / 100
                    random.of(minRoll..max(minRoll, maxRoll))
                }
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(this, target, damage)
            manager.continueCombat(this, target)
        }
    }

    private class BloodInfusion(
        private val manager: SpecialAttackManager,
        private val spec: SpecFx,
    ) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            strike(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            strike(target, attack)
            return true
        }

        private fun ProtectedAccess.strike(target: PathingEntity, attack: CombatAttack.Melee) {
            val hitpoints = player.statValue("stat.hitpoints")
            if (hitpoints > 1) {
                val sacrifice = max(1, hitpoints / 4).coerceAtMost(hitpoints - 1)
                statSub("stat.hitpoints", constant = sacrifice, percent = 0)
            }

            playSpecFx(spec)
            val damage =
                manager.rollMeleeDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = spec.accuracyMultiplier,
                    maxHitMultiplier = spec.maxHitMultiplier,
                    blockType = spec.blockType,
                )
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(this, target, damage)
            spec.onHit(this, target, damage)
            manager.continueCombat(this, target)
        }
    }

    private class AreaMeleeHit(
        private val manager: SpecialAttackManager,
        private val npcRepo: NpcRepository,
        private val playerRepo: PlayerRepository,
        private val spec: SpecFx,
        private val radius: Int,
        private val maxTargets: Int = Int.MAX_VALUE,
        private val damageScale: Double = 1.0,
    ) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            sweep(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            sweep(target, attack)
            return true
        }

        private fun ProtectedAccess.sweep(primary: PathingEntity, attack: CombatAttack.Melee) {
            playSpecFx(spec)
            var total = 0
            for (target in targets(primary).take(maxTargets)) {
                val rawDamage =
                    manager.rollMeleeDamage(
                        source = this,
                        target = target,
                        attack = attack,
                        accuracyMultiplier = spec.accuracyMultiplier,
                        maxHitMultiplier = spec.maxHitMultiplier,
                        blockType = spec.blockType,
                    )
                val damage = (rawDamage * damageScale).toInt()
                total += damage
                manager.queueMeleeHit(this, target, damage)
                spec.onHit(this, target, damage)
            }
            manager.giveCombatXp(this, primary, attack, total)
            manager.continueCombat(this, primary)
        }

        private fun ProtectedAccess.targets(primary: PathingEntity): Sequence<PathingEntity> {
            val zone = ZoneKey.from(player.coords)
            val nearby =
                when (primary) {
                    is Npc ->
                        npcRepo
                            .findAll(zone, zoneRadius = 1)
                            .filter {
                                it !== primary &&
                                    it.isValidTarget() &&
                                    it.coords.level == player.coords.level &&
                                    it.coords.chebyshevDistance(player.coords) <= radius
                            }
                    is Player ->
                        playerRepo
                            .findAll(zone, zoneRadius = 1)
                            .filter {
                                it !== player &&
                                    it !== primary &&
                                    it.isValidTarget() &&
                                    it.coords.level == player.coords.level &&
                                    it.coords.chebyshevDistance(player.coords) <= radius
                            }
                }
            return sequenceOf(primary) + nearby
        }
    }

    private class VestaSpearBounty(private val manager: SpecialAttackManager) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            strike(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            strike(target, attack)
            return true
        }

        private fun ProtectedAccess.strike(target: PathingEntity, attack: CombatAttack.Melee) {
            playSpecFx(VestaSpearSpec)
            val first =
                manager.rollMeleeDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = VestaSpearSpec.accuracyMultiplier,
                    maxHitMultiplier = VestaSpearSpec.maxHitMultiplier,
                    blockType = VestaSpearSpec.blockType,
                )
            val second = if (first > 0) random.of((first / 2)..max(first / 2, first * 3 / 4)) else 0
            manager.giveCombatXp(this, target, attack, first + second)
            manager.queueMeleeHit(this, target, first)
            manager.queueMeleeHit(this, target, second)
            manager.setNextAttackDelay(this, 4)
            manager.continueCombat(this, target)
        }
    }

    private class SeekingLunge(
        private val manager: SpecialAttackManager,
        private val spec: SpecFx,
    ) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            lunge(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            lunge(target, attack)
            return true
        }

        private fun ProtectedAccess.lunge(target: PathingEntity, attack: CombatAttack.Melee) {
            playSpecFx(spec)
            val accurate =
                manager.rollMeleeAccuracy(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    blockType = spec.blockType ?: attack.type,
                    multiplier = spec.accuracyMultiplier,
                )
            val damage =
                if (accurate) {
                    val maxHit =
                        manager.calculateMeleeMaxHit(
                            source = this,
                            target = target,
                            attackType = attack.type,
                            attackStyle = attack.style,
                            multiplier = spec.maxHitMultiplier,
                        )
                    max(1, (maxHit * 70) / 100)
                } else {
                    0
                }
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(this, target, damage)
            manager.continueCombat(this, target)
        }
    }

    private class KerisPartisanSun(private val manager: SpecialAttackManager) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean = restore(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean = restore(target, attack)

        private fun ProtectedAccess.restore(target: PathingEntity, attack: CombatAttack.Melee): Boolean {
            if (player.statValue("stat.prayer") < 50) {
                mes("You need at least 50 Prayer points to use this special attack.")
                manager.stopCombat(this)
                return false
            }
            playSpecFx(KerisPartisanSunSpec)
            statSub("stat.prayer", constant = 50, percent = 0)
            statAdd("stat.hitpoints", constant = 0, percent = 20)
            for (stat in RestorableStats) {
                statHeal(stat, constant = 0, percent = 100)
            }
            player.runEnergy = constants.run_max_energy
            manager.giveCombatXp(this, target, attack, 0)
            manager.continueCombat(this, target)
            return true
        }
    }

    private class SoulreaperBehead(private val manager: SpecialAttackManager) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean = behead(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean = behead(target, attack)

        private fun ProtectedAccess.behead(target: PathingEntity, attack: CombatAttack.Melee): Boolean {
            val stacks = vars["varp.soulreaper_stacks"].coerceIn(0, SoulreaperMaxStacks)
            if (stacks <= 0) {
                mes("You have no Soul Stacks to consume.")
                manager.stopCombat(this)
                return false
            }

            vars["varp.soulreaper_stacks"] = 0
            playSpecFx(SoulreaperBeheadSpec)
            val damage =
                manager.rollMeleeDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = 1.0 + stacks * 0.06,
                    maxHitMultiplier = 1.0 + stacks * 0.06,
                    blockType = MeleeAttackType.Slash,
                )
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(this, target, damage)
            statHeal("stat.hitpoints", constant = stacks * 8, percent = 0)
            manager.continueCombat(this, target)
            return true
        }
    }

    private class Retainer(private val manager: SpecialAttackManager) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean = retain(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean = retain(target, attack)

        private fun ProtectedAccess.retain(target: PathingEntity, attack: CombatAttack.Melee): Boolean {
            faceEntitySquare(target)
            anim(RetainerSpec.anim)
            if (RetainerSpec.spotanim != null) {
                spotanim(RetainerSpec.spotanim, height = 96, slot = constants.spotanim_slot_combat)
            }

            if (target !is Npc || !target.isRetainableVampyre()) {
                mes("The weapon has no effect.")
                manager.stopCombat(this)
                return false
            }

            if (target.hitpoints * 2 > target.baseHitpointsLvl) {
                mes("The vampyre is too strong to be restrained.")
                manager.stopCombat(this)
                return false
            }

            target.resetMovement()
            target.delay(RetainerHoldTicks)
            target.anim("seq.ivandis_flail_defend")
            target.spotanim("spotanim.sp_attackglow_red", height = 96, delay = 20)
            mes("You temporarily restrain the vampyre.")
            manager.giveCombatXp(this, target, attack, 0)
            manager.stopCombat(this)
            return true
        }
    }

    private class Shove(private val manager: SpecialAttackManager) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            shove(target)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            shove(target)
            return true
        }

        private fun ProtectedAccess.shove(target: PathingEntity) {
            faceEntitySquare(target)
            playSpecFx(ShoveSpec)
            if (target is Player) {
                CombatEffects.freeze(target, ShoveFreezeTicks)
            }
            manager.stopCombat(this)
        }
    }

    private class DragonHasta(private val manager: SpecialAttackManager) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean = unleash(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean = unleash(target, attack)

        private fun ProtectedAccess.unleash(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ): Boolean {
            val energy = availableDragonHastaEnergy()
            if (energy <= 0) {
                mes("You don't have enough power left.")
                manager.stopCombat(this)
                return false
            }

            manager.takeSpecialEnergy(this, energy)
            val steps = energy / DragonHastaEnergyStep
            playSpecFx(DragonHastaSpec)
            val damage =
                manager.rollMeleeDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = 1.0 + (steps * 0.05),
                    maxHitMultiplier = 1.0 + (steps * 0.025),
                    blockType = MeleeAttackType.Stab,
                )
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(this, target, damage)
            manager.continueCombat(this, target)
            return false
        }

        private fun ProtectedAccess.availableDragonHastaEnergy(): Int {
            for (energy in DragonHastaMaxSpend downTo DragonHastaEnergyStep step DragonHastaEnergyStep) {
                if (manager.hasSpecialEnergy(this, energy)) {
                    return energy
                }
            }
            return 0
        }
    }

    private class Claws(private val manager: SpecialAttackManager) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            slice(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            slice(target, attack)
            return true
        }

        private fun ProtectedAccess.slice(target: PathingEntity, attack: CombatAttack.Melee) {
            playSpecFx(DragonClawsSpec)
            val maxHit =
                manager.calculateMeleeMaxHit(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    multiplier = 1.0,
                )
            val hit =
                manager.rollMeleeAccuracy(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    blockType = MeleeAttackType.Slash,
                    multiplier = 1.25,
                )
            val hits =
                if (hit) {
                    val first = random.of(0..maxHit)
                    val second = first / 2
                    val third = second / 2
                    intArrayOf(first, second, third, first - second - third)
                } else {
                    intArrayOf(0, 0, random.of(0..max(1, maxHit / 2)), random.of(0..max(1, maxHit / 2)))
                }
            for (damage in hits) {
                manager.queueMeleeHit(this, target, damage)
            }
            manager.giveCombatXp(this, target, attack, hits.sum())
            manager.continueCombat(this, target)
        }
    }

    private class BurningClawBarrage(private val manager: SpecialAttackManager) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            burn(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            burn(target, attack)
            return true
        }

        private fun ProtectedAccess.burn(target: PathingEntity, attack: CombatAttack.Melee) {
            playSpecFx(BurningClawsSpec)
            var total = 0
            repeat(3) {
                val damage =
                    manager.rollMeleeDamage(
                        source = this,
                        target = target,
                        attack = attack,
                        accuracyMultiplier = 1.25,
                        maxHitMultiplier = 1.25,
                        blockType = MeleeAttackType.Slash,
                    )
                total += damage
                manager.queueMeleeHit(this, target, damage)
            }
            manager.giveCombatXp(this, target, attack, total)
            manager.continueCombat(this, target)
        }
    }

    private class SaradominSword(
        private val manager: SpecialAttackManager,
        private val blessed: Boolean,
    ) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            lightning(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            lightning(target, attack)
            return true
        }

        private fun ProtectedAccess.lightning(target: PathingEntity, attack: CombatAttack.Melee) {
            val spec = if (blessed) BlessedSaradominSwordSpec else SaradominSwordSpec
            playSpecFx(spec)
            val meleeDamage =
                manager.rollMeleeDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = spec.accuracyMultiplier,
                    maxHitMultiplier = spec.maxHitMultiplier,
                    blockType = MeleeAttackType.Slash,
                )
            manager.queueMeleeHit(this, target, meleeDamage)
            var total = meleeDamage
            if (!blessed) {
                val magicDamage = if (meleeDamage > 0) random.of(1..16) else 0
                total += magicDamage
                target.spotanim("spotanim.godwars_saradomin_light_attk_spot", delay = 20, height = 96)
                manager.queueMagicHit(this, target, magicDamage, clientDelay = 20, hitDelay = 1)
            }
            manager.giveCombatXp(this, target, attack, total)
            manager.continueCombat(this, target)
        }
    }

    private class Voidwaker(private val manager: SpecialAttackManager) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            disrupt(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            disrupt(target, attack)
            return true
        }

        private fun ProtectedAccess.disrupt(target: PathingEntity, attack: CombatAttack.Melee) {
            playSpecFx(VoidwakerSpec)
            target.spotanim("spotanim.fx_voidwaker_impact", height = 96, delay = 20)
            val maxHit =
                manager.calculateMeleeMaxHit(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    multiplier = 1.0,
                )
            val damage = random.of(max(1, maxHit / 2)..max(1, (maxHit * 3) / 2))
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMagicHit(this, target, damage, clientDelay = 20, hitDelay = 1)
            manager.continueCombat(this, target)
        }
    }

    private object DragonBattleaxe : InstantSpecialAttack {
        override suspend fun ProtectedAccess.activate(): Boolean {
            anim("seq.rampage")
            spotanim("spotanim.sp_attackglow_red", height = 96, slot = constants.spotanim_slot_combat)
            val beforeAttack = player.statValue("stat.attack")
            val beforeDefence = player.statValue("stat.defence")
            val beforeRanged = player.statValue("stat.ranged")
            val beforeMagic = player.statValue("stat.magic")
            statSub("stat.attack", constant = 0, percent = 10)
            statSub("stat.defence", constant = 0, percent = 10)
            statSub("stat.ranged", constant = 0, percent = 10)
            statSub("stat.magic", constant = 0, percent = 10)
            val drained =
                (beforeAttack - player.statValue("stat.attack")) +
                    (beforeDefence - player.statValue("stat.defence")) +
                    (beforeRanged - player.statValue("stat.ranged")) +
                    (beforeMagic - player.statValue("stat.magic"))
            statAdd("stat.strength", constant = 10 + drained / 4, percent = 0)
            return true
        }
    }

    private object Excalibur : InstantSpecialAttack {
        override suspend fun ProtectedAccess.activate(): Boolean {
            anim("seq.sanctuary")
            spotanim("spotanim.sp_attackglow_red", height = 96, slot = constants.spotanim_slot_combat)
            statAdd("stat.defence", constant = 8, percent = 0)
            return true
        }
    }

    private object SoulflameHorn : InstantSpecialAttack {
        override suspend fun ProtectedAccess.activate(): Boolean {
            anim("seq.human_blunt_pound")
            spotanim("spotanim.sp_attackglow_red", height = 96, slot = constants.spotanim_slot_combat)
            statBoost("stat.attack", constant = 0, percent = 15)
            mes("You empower your next melee attacks with soulflame.")
            return true
        }
    }

    private data class SpecFx(
        val anim: String,
        val spotanim: String? = null,
        val accuracyMultiplier: Double = 1.0,
        val maxHitMultiplier: Double = 1.0,
        val blockType: MeleeAttackType? = null,
        val nextAttackDelay: Int? = null,
        val onHit: ProtectedAccess.(PathingEntity, Int) -> Unit = { _, _ -> },
    )

    private companion object {
        private val DragonDaggerSpec =
            SpecFx(
                anim = "seq.puncture",
                spotanim = "spotanim.sp_attack_puncture_spotanim",
                accuracyMultiplier = 1.15,
                maxHitMultiplier = 1.15,
                blockType = MeleeAttackType.Stab,
            )
        private val AbyssalDaggerSpec =
            SpecFx(
                anim = "seq.abyssal_dagger_special",
                spotanim = "spotanim.abyssal_dagger_special_spotanim",
                accuracyMultiplier = 1.25,
                maxHitMultiplier = 0.85,
                blockType = MeleeAttackType.Stab,
            )
        private val BountyAbyssalDaggerSpec = AbyssalDaggerSpec.copy(maxHitMultiplier = 0.95)
        private val WhipSpec =
            SpecFx(
                anim = "seq.slayer_whip_sp_attack",
                spotanim = "spotanim.sp_attackglow_red",
                accuracyMultiplier = 1.25,
                blockType = MeleeAttackType.Slash,
                onHit = { target, damage ->
                    if (damage > 0 && target is Player) {
                        target.runEnergy = max(0, target.runEnergy - 100)
                    }
                },
            )
        private val TentacleSpec =
            WhipSpec.copy(
                onHit = { target, damage ->
                    if (damage > 0 && target is Player) {
                        CombatEffects.freeze(target, 8)
                        if (random.of(4) == 0) {
                            CombatEffects.poison(target, 4)
                        }
                    }
                }
            )
        private val DragonScimitarSpec =
            SpecFx(
                anim = "seq.sp_attack_dragon_scimitar",
                spotanim = "spotanim.sp_attack_dragon_scimitar_trail_spotanim",
                accuracyMultiplier = 1.25,
                blockType = MeleeAttackType.Slash,
            )
        private val DragonWarhammerSpec =
            SpecFx(
                anim = "seq.dragon_warhammer_sa_player",
                spotanim = "spotanim.dragon_warhammer_sa_spotanim",
                blockType = MeleeAttackType.Crush,
                onHit = { target, damage ->
                    if (damage > 0) {
                        target.drainCombatStat(CombatStat.Defence, percent = 30)
                    }
                },
            )
        private val StatiusWarhammerSpec =
            SpecFx(
                anim = "seq.human_blunt_pound",
                spotanim = "spotanim.sp_attackglow_red",
                accuracyMultiplier = 1.25,
                maxHitMultiplier = 1.25,
                blockType = MeleeAttackType.Crush,
                onHit = { target, damage ->
                    if (damage > 0) {
                        target.drainCombatStat(CombatStat.Defence, percent = 30)
                    }
                },
            )
        private val BountyStatiusWarhammerSpec =
            StatiusWarhammerSpec.copy(
                accuracyMultiplier = 1.50,
                onHit = { target, damage ->
                    if (damage > 0) {
                        target.drainCombatStat(CombatStat.Defence, percent = 75)
                    }
                }
            )
        private val ArkanBladeSpec =
            SpecFx(
                anim = "seq.human_sword_slash",
                spotanim = "spotanim.sp_attackglow_red",
                accuracyMultiplier = 1.50,
                maxHitMultiplier = 1.50,
                blockType = MeleeAttackType.Slash,
            )
        private val FrostmoonSpearSpec =
            SpecFx(
                anim = "seq.human_dspear_lunge",
                spotanim = "spotanim.sp_attackglow_red",
                accuracyMultiplier = 1.25,
                maxHitMultiplier = 1.25,
                blockType = MeleeAttackType.Stab,
            )
        private val CrimsonKistenSpec =
            SpecFx(
                anim = "seq.human_blunt_pound",
                spotanim = "spotanim.sp_attackglow_red",
                blockType = MeleeAttackType.Crush,
            )
        private val AncientMaceSpec =
            SpecFx(
                anim = "seq.human_blunt_spike",
                spotanim = "spotanim.sp_attackglow_red",
                accuracyMultiplier = 1.10,
                blockType = MeleeAttackType.Crush,
                onHit = { target, damage ->
                    if (damage > 0) {
                        if (target is Player) {
                            target.statSub("stat.prayer", constant = damage, percent = 0)
                        }
                        statAdd("stat.prayer", constant = damage, percent = 0)
                    }
                },
            )
        private val DragonMaceSpec =
            SpecFx(
                anim = "seq.shatter",
                spotanim = "spotanim.sp_attack_shatter_spotanim",
                accuracyMultiplier = 1.25,
                maxHitMultiplier = 1.50,
                blockType = MeleeAttackType.Crush,
            )
        private val BoneDaggerSpec =
            SpecFx(
                anim = "seq.human_sword_stab",
                spotanim = "spotanim.sp_attackglow_red",
                accuracyMultiplier = 2.0,
                blockType = MeleeAttackType.Stab,
                onHit = { target, damage ->
                    if (damage > 0) {
                        target.drainCombatStatFlat(CombatStat.Defence, damage)
                    }
                },
            )
        private val EchoCrystalDaggerSpec =
            SpecFx(
                anim = "seq.human_sword_stab",
                spotanim = "spotanim.sp_attackglow_red",
                accuracyMultiplier = 1.25,
                maxHitMultiplier = 1.25,
                blockType = MeleeAttackType.Stab,
            )
        private val DragonCandleDaggerSpec =
            SpecFx(
                anim = "seq.human_sword_stab",
                spotanim = "spotanim.sp_attackglow_red",
                accuracyMultiplier = 1.10,
                maxHitMultiplier = 1.10,
                blockType = MeleeAttackType.Stab,
            )
        private val BrineSabreSpec =
            SpecFx(
                anim = "seq.human_sword_slash",
                spotanim = "spotanim.sp_attackglow_red",
                accuracyMultiplier = 2.0,
                blockType = MeleeAttackType.Slash,
                onHit = { _, damage ->
                    if (damage > 0) {
                        val boost = max(1, damage / 4)
                        statAdd("stat.attack", constant = boost, percent = 0)
                        statAdd("stat.strength", constant = boost, percent = 0)
                        statAdd("stat.defence", constant = boost, percent = 0)
                    }
                },
            )
        private val FangOfTheHoundSpec =
            SpecFx(
                anim = "seq.human_sword_slash",
                spotanim = "spotanim.sp_attackglow_red",
                accuracyMultiplier = 1.25,
                maxHitMultiplier = 1.10,
                blockType = MeleeAttackType.Slash,
            )
        private val InfernalTecpatlSpec =
            SpecFx(
                anim = "seq.human_sword_stab",
                spotanim = "spotanim.sp_attackglow_red",
                accuracyMultiplier = 1.25,
                maxHitMultiplier = 1.25,
                blockType = MeleeAttackType.Stab,
            )
        private val DualMacuahuitlSpec =
            SpecFx(
                anim = "seq.human_blunt_pound",
                spotanim = "spotanim.sp_attackglow_red",
                accuracyMultiplier = 1.25,
                maxHitMultiplier = 1.25,
                blockType = MeleeAttackType.Crush,
            )
        private val DragonSwordSpec =
            SpecFx(
                anim = "seq.human_dragon_sword_spec",
                spotanim = "spotanim.dragon_sword_spec_spotanim",
                accuracyMultiplier = 1.25,
                maxHitMultiplier = 1.25,
                blockType = MeleeAttackType.Stab,
            )
        private val VestasLongswordSpec =
            SpecFx(
                anim = "seq.human_sword_slash",
                spotanim = "spotanim.sp_attackglow_red",
                accuracyMultiplier = 2.0,
                maxHitMultiplier = 1.20,
                blockType = MeleeAttackType.Slash,
            )
        private val DragonTwoHanderSpec =
            SpecFx(
                anim = "seq.specialattack_disembowel",
                spotanim = "spotanim.dh_sword_update_dragon_2h_sword_special_spotanim",
                accuracyMultiplier = 1.25,
                maxHitMultiplier = 1.25,
                blockType = MeleeAttackType.Slash,
            )
        private val DinhsBulwarkSpec =
            SpecFx(
                anim = "seq.human_blunt_pound",
                spotanim = "spotanim.sp_attackglow_red",
                accuracyMultiplier = 1.20,
                blockType = MeleeAttackType.Crush,
            )
        private val DragonHalberdSpec =
            SpecFx(
                anim = "seq.dragon_halberd_special_attack",
                spotanim = "spotanim.dragon_halberd_special_south_red",
                maxHitMultiplier = 1.10,
                blockType = MeleeAttackType.Slash,
            )
        private val CrystalHalberdSpec =
            DragonHalberdSpec.copy(maxHitMultiplier = 1.10)
        private val DragonClawsSpec =
            SpecFx(
                anim = "seq.human_dragon_claws_spec",
                spotanim = "spotanim.dragon_claws_spot",
                blockType = MeleeAttackType.Slash,
            )
        private val RuneClawsSpec =
            SpecFx(
                anim = "seq.d_claws_punch",
                spotanim = "spotanim.sp_attackglow_red",
                accuracyMultiplier = 1.10,
                maxHitMultiplier = 1.10,
                blockType = MeleeAttackType.Slash,
                nextAttackDelay = 5,
            )
        private val ArclightSpec =
            SpecFx(
                anim = "seq.human_weapon_emberlight_01_spec",
                spotanim = "spotanim.vfx_emberlight_spec_02",
                blockType = MeleeAttackType.Slash,
                onHit = { target, damage ->
                    if (damage > 0) {
                        target.drainCombatStat(CombatStat.Attack, percent = 5)
                        target.drainCombatStat(CombatStat.Strength, percent = 5)
                        target.drainCombatStat(CombatStat.Defence, percent = 5)
                    }
                },
            )
        private val VoidwakerSpec =
            SpecFx(
                anim = "seq.human_special_voidwaker",
                spotanim = "spotanim.fx_voidwaker02_special",
            )
        private val GraniteMaulSpec =
            SpecFx(
                anim = "seq.slayer_granite_maul_special_attack",
                spotanim = "spotanim.sp_attack_maul_spotanim",
                blockType = MeleeAttackType.Crush,
            )
        private val GraniteHammerSpec =
            SpecFx(
                anim = "seq.slayer_maul_sp_attack",
                spotanim = "spotanim.granite_hammer_sa_spotanim",
                accuracyMultiplier = 1.50,
                blockType = MeleeAttackType.Crush,
            )
        private val ElderMaulSpec =
            SpecFx(
                anim = "seq.human_elder_maul_spec",
                spotanim = "spotanim.spotanim_elder_maul_special",
                accuracyMultiplier = 1.25,
                blockType = MeleeAttackType.Crush,
                onHit = { target, damage ->
                    if (damage > 0) {
                        target.spotanim("spotanim.spotanim_elder_maul_special_impact", height = 96, delay = 20)
                        target.drainCombatStat(CombatStat.Defence, percent = 35)
                    }
                },
            )
        private val BarrelchestAnchorSpec =
            SpecFx(
                anim = "seq.brain_player_anchor_special_attack",
                spotanim = "spotanim.brain_anchor_special_attack_spot",
                accuracyMultiplier = 2.0,
                maxHitMultiplier = 1.10,
                blockType = MeleeAttackType.Crush,
                onHit = { target, damage ->
                    if (damage > 0) {
                        target.drainCombatStat(CombatStat.Defence, percent = max(1, damage / 10))
                    }
                },
            )
        private val AbyssalBludgeonSpec =
            SpecFx(
                anim = "seq.abyssal_bludgeon_special_attack",
                spotanim = "spotanim.abyssal_miasma_spotanim_bludgeon",
                maxHitMultiplier = 1.20,
                blockType = MeleeAttackType.Crush,
            )
        private val NoxiousHalberdSpec =
            SpecFx(
                anim = "seq.dragon_halberd_special_attack",
                spotanim = "spotanim.sp_attackglow_red",
                accuracyMultiplier = 1.25,
                maxHitMultiplier = 1.25,
                blockType = MeleeAttackType.Slash,
            )
        private val UrsineChainmaceSpec =
            SpecFx(
                anim = "seq.wild_cave_chainmace_crush",
                spotanim = "spotanim.sp_attackglow_red",
                accuracyMultiplier = 2.0,
                maxHitMultiplier = 1.10,
                blockType = MeleeAttackType.Crush,
                onHit = { target, damage ->
                    if (damage > 0 && target is Player) {
                        target.statSub("stat.agility", constant = max(1, damage / 4), percent = 0)
                        target.runEnergy = 0
                        CombatEffects.freeze(target, 5)
                    }
                },
            )
        private val SaradominSwordSpec =
            SpecFx(
                anim = "seq.saradomin_sword_special_player",
                spotanim = "spotanim.dh_sword_update_saradomin_god_special_spotanim",
                maxHitMultiplier = 1.10,
                blockType = MeleeAttackType.Slash,
            )
        private val BlessedSaradominSwordSpec =
            SpecFx(
                anim = "seq.blessed_saradomin_sword_special_player",
                spotanim = "spotanim.saradomin_special_spotanim_gold",
                maxHitMultiplier = 1.25,
                blockType = MeleeAttackType.Slash,
            )
        private val OsmumtensFangSpec =
            SpecFx(
                anim = "seq.human_osmumtens_fang",
                accuracyMultiplier = 1.50,
                blockType = MeleeAttackType.Stab,
            )
        private val KerisPartisanCorruptionSpec =
            SpecFx(
                anim = "seq.human_osmumtens_fang",
                spotanim = "spotanim.sp_attackglow_red",
                accuracyMultiplier = 2.0,
                maxHitMultiplier = 1.25,
                blockType = MeleeAttackType.Stab,
            )
        private val KerisPartisanSunSpec =
            SpecFx(
                anim = "seq.human_osmumtens_fang",
                spotanim = "spotanim.sp_attackglow_red",
                blockType = MeleeAttackType.Stab,
            )
        private val SoulreaperBeheadSpec =
            SpecFx(
                anim = "seq.human_sword_slash",
                spotanim = "spotanim.sp_attackglow_red",
                blockType = MeleeAttackType.Slash,
            )
        private val SunspearSpec =
            SpecFx(
                anim = "seq.human_dspear_lunge",
                spotanim = "spotanim.sp_attackglow_red",
                accuracyMultiplier = 1.0,
                blockType = MeleeAttackType.Stab,
            )
        private val ThunderKhopeshSpec =
            SpecFx(
                anim = "seq.human_sword_slash",
                spotanim = "spotanim.sp_attackglow_red",
                accuracyMultiplier = 1.15,
                maxHitMultiplier = 1.10,
                blockType = MeleeAttackType.Slash,
            )
        private val DeadmanThunderKhopeshSpec =
            ThunderKhopeshSpec.copy(accuracyMultiplier = 1.30, maxHitMultiplier = 1.30)
        private val SunlightSpearSpec =
            SpecFx(
                anim = "seq.human_dspear_lunge",
                spotanim = "spotanim.sp_attackglow_red",
                blockType = MeleeAttackType.Stab,
            )
        private val VestaSpearSpec =
            SpecFx(
                anim = "seq.human_dspear_lunge",
                spotanim = "spotanim.sp_attackglow_red",
                accuracyMultiplier = 1.20,
                blockType = MeleeAttackType.Stab,
            )
        private val BurningClawsSpec =
            SpecFx(
                anim = "seq.human_weapon_burning_claws_02_spec",
                spotanim = "spotanim.vfx_burning_claws_spec_02",
                blockType = MeleeAttackType.Slash,
            )
        private val ShoveSpec =
            SpecFx(
                anim = "seq.human_dspear_lunge",
                spotanim = "spotanim.sp_attackglow_red",
                blockType = MeleeAttackType.Stab,
            )
        private val DragonHastaSpec =
            SpecFx(
                anim = "seq.human_dspear_lunge",
                spotanim = "spotanim.sp_attackglow_red",
                blockType = MeleeAttackType.Stab,
            )

        private val DragonDaggers =
            arrayOf(
                "obj.dragon_dagger",
                "obj.dragon_dagger_p",
                "obj.dragon_dagger_p+",
                "obj.dragon_dagger_p++",
                "obj.br_dragon_dagger",
                "obj.bh_dragon_dagger_corrupted",
                "obj.bh_dragon_dagger_p_corrupted",
                "obj.bh_dragon_dagger_p+_corrupted",
                "obj.bh_dragon_dagger_p++_corrupted",
            )
        private val AbyssalDaggers =
            arrayOf(
                "obj.abyssal_dagger",
                "obj.abyssal_dagger_p",
                "obj.abyssal_dagger_p+",
                "obj.abyssal_dagger_p++",
            )
        private val BountyAbyssalDaggers =
            arrayOf(
                "obj.bh_abyssal_dagger_imbue",
                "obj.bh_abyssal_dagger_p_imbue",
                "obj.bh_abyssal_dagger_p+_imbue",
                "obj.bh_abyssal_dagger_p++_imbue",
            )
        private val Whips =
            arrayOf(
                "obj.abyssal_whip",
                "obj.br_abyssal_whip",
                "obj.league_3_whip",
                "obj.abyssal_whip_ice",
                "obj.abyssal_whip_lava",
            )
        private val TentacleWhips =
            arrayOf("obj.abyssal_tentacle", "obj.league_3_whip_tentacle")
        private val DragonScimitars =
            arrayOf(
                "obj.dragon_scimitar",
                "obj.dragon_scimitar_ornament",
                "obj.br_dragon_scimitar",
                "obj.bh_dragon_scimitar_corrupted",
            )
        private val DragonWarhammers =
            arrayOf(
                "obj.dragon_warhammer",
                "obj.dragon_warhammer_ornament",
                "obj.br_dragon_warhammer",
                "obj.bh_dragon_warhammer_corrupted",
            )
        private val StatiusWarhammers =
            arrayOf("obj.statius_warhammer", "obj.br_statius_warhammer")
        private val BountyStatiusWarhammers = arrayOf("obj.statius_warhammer_bh")
        private val ArkanBlades = arrayOf("obj.arkan_blade")
        private val FrostmoonSpears = arrayOf("obj.frostmoon_spear", "obj.br_frostmoon_spear")
        private val CrimsonKistens = arrayOf("obj.crimson_kisten")
        private val AncientMaces = arrayOf("obj.ancient_goblin_mace")
        private val DragonMaces =
            arrayOf("obj.dragon_mace", "obj.bh_dragon_mace_imbue", "obj.bh_dragon_mace_corrupted")
        private val BoneDaggers =
            arrayOf(
                "obj.dttd_bone_dagger",
                "obj.dttd_bone_dagger_p",
                "obj.dttd_bone_dagger_p+",
                "obj.dttd_bone_dagger_p++",
            )
        private val EchoCrystalDaggers = arrayOf("obj.echo_gauntlet_crystal_dagger_t3")
        private val DragonCandleDaggers = arrayOf("obj.osb10_dragon_candle")
        private val BrineSabres = arrayOf("obj.olaf2_brine_sabre")
        private val FangOfTheHounds = arrayOf("obj.fang_of_the_hound")
        private val InfernalTecpatls = arrayOf("obj.infernal_tecpatl")
        private val DualMacuahuitls = arrayOf("obj.dual_macuahuitl", "obj.br_dual_macuahuitl")
        private val DragonBattleaxes =
            arrayOf("obj.dragon_battleaxe", "obj.bh_dragon_battleaxe_corrupted")
        private val DragonSwords =
            arrayOf("obj.dragon_shortsword", "obj.br_dragon_sword", "obj.bh_dragon_shortsword_corrupted")
        private val VestasLongswords =
            arrayOf(
                "obj.vestas_longsword",
                "obj.br_vestas_longsword",
                "obj.bh_vestas_longsword",
                "obj.vestas_longsword_bh",
            )
        private val DragonTwoHanders =
            arrayOf("obj.dragon_2h_sword", "obj.br_dragon_2h", "obj.bh_dragon_2h_sword_corrupted")
        private val DinhsBulwarks = arrayOf("obj.dinhs_bulwark", "obj.dinhs_bulwark_ornament")
        private val DragonHalberds =
            arrayOf("obj.dragon_halberd", "obj.bh_dragon_halberd_corrupted")
        private val CrystalHalberds =
            arrayOf("obj.crystal_halberd", "obj.crystal_halberd_2500")
        private val DragonClaws =
            intArrayOf(13652, 20784, 26708, 28039, 28534)
        private val RuneClaws = arrayOf("obj.rune_claws")
        private val Arclights = arrayOf("obj.darklight", "obj.arclight", "obj.arclight_inactive", "obj.emberlight")
        private val Voidwakers =
            arrayOf(
                "obj.voidwaker",
                "obj.br_voidwaker",
                "obj.deadman_voidwaker",
                "obj.deadman_blighted_voidwaker",
            )
        private val ShoveSpears =
            arrayOf(
                "obj.dragon_spear",
                "obj.dragon_spear_p",
                "obj.dragon_spear_p+",
                "obj.dragon_spear_p++",
                "obj.tbwt_dragon_spear_kp",
                "obj.bh_dragon_spear_corrupted",
                "obj.bh_dragon_spear_p_corrupted",
                "obj.bh_dragon_spear_p+_corrupted",
                "obj.bh_dragon_spear_p++_corrupted",
                "obj.zamorak_spear",
                "obj.zamorak_hasta",
            )
        private val DragonHastas =
            arrayOf(
                "obj.brut_dragon_spear",
                "obj.brut_dragon_spear_p",
                "obj.brut_dragon_spear_p+",
                "obj.brut_dragon_spear_p++",
                "obj.brut_dragon_spear_kp",
            )
        private val GraniteMauls =
            arrayOf(
                "obj.granite_maul",
                "obj.granite_maul_pretty",
                "obj.br_granite_maul",
                "obj.granite_maul_plus",
                "obj.granite_maul_pretty_plus",
            )
        private val GraniteHammers = arrayOf("obj.granite_hammer")
        private val ElderMauls =
            arrayOf("obj.elder_maul", "obj.br_elder_maul", "obj.elder_maul_ornament")
        private val BarrelchestAnchors =
            arrayOf("obj.brain_anchor", "obj.bh_brain_anchor_imbue")
        private val AbyssalBludgeons = arrayOf("obj.abyssal_bludgeon")
        private val NoxiousHalberds = arrayOf("obj.noxious_halberd", "obj.br_noxious_halberd")
        private val UrsineChainmaces = arrayOf("obj.wild_cave_ursine_charged")
        private val SaradominSwords = arrayOf("obj.saradomin_sword")
        private val BlessedSaradominSwords =
            arrayOf("obj.blessed_saradomin_sword", "obj.blessed_saradomin_sword_degraded")
        private val OsmumtensFangs =
            arrayOf("obj.osmumtens_fang", "obj.br_osmumtens_fang", "obj.osmumtens_fang_ornament")
        private val KerisPartisansOfCorruption = arrayOf("obj.keris_partisan_corruption")
        private val KerisPartisansOfTheSun = arrayOf("obj.keris_partisan_sun")
        private val RetainerWeapons =
            arrayOf(
                "obj.blisterwood_flail",
                "obj.hallowed_flail",
                "obj.ivandis_flail",
                "obj.burgh_rod_command_final_1",
                "obj.burgh_rod_command_final_2",
                "obj.burgh_rod_command_final_3",
                "obj.burgh_rod_command_final_4",
                "obj.burgh_rod_command_final_5",
                "obj.burgh_rod_command_final_6",
                "obj.burgh_rod_command_final_7",
                "obj.burgh_rod_command_final_8",
                "obj.burgh_rod_command_final_9",
                "obj.burgh_rod_command_final_10",
            )
        private val Soulreapers = arrayOf("obj.soulreaper", "obj.soulreaper_axe_orn")
        private val SoulflameHorns = arrayOf("obj.soulflame_horn")
        private val Sunspears = arrayOf("obj.sunspear")
        private val ThunderKhopeshes = arrayOf("obj.thunder_khopesh")
        private val DeadmanThunderKhopeshes = arrayOf("obj.deadman_thunder_khopesh")
        private val SunlightSpears = arrayOf("obj.weapon_of_sol")
        private val VestaSpears = arrayOf("obj.vestas_spear")
        private val BountyVestaSpears = arrayOf("obj.vestas_spear_bh")
        private val BurningClaws = arrayOf("obj.bone_claws", "obj.br_bone_claws")
        private val Excaliburs = arrayOf("obj.excalibur")
        private val RestorableStats =
            arrayOf(
                "stat.attack",
                "stat.strength",
                "stat.defence",
                "stat.ranged",
                "stat.magic",
                "stat.hitpoints",
            )
        private val RetainerSpec =
            SpecFx(
                anim = "seq.ivandis_flail_attack",
                spotanim = "spotanim.sp_attackglow_red",
                blockType = MeleeAttackType.Crush,
            )
        private const val ShoveFreezeTicks = 5
        private const val DragonHastaEnergyStep = 50
        private const val DragonHastaMaxSpend = 1000
        private const val SoulreaperMaxStacks = 5
        private const val RetainerHoldTicks = 8

        private fun ProtectedAccess.playSpecFx(spec: SpecFx) {
            anim(spec.anim)
            if (spec.spotanim != null) {
                spotanim(spec.spotanim, height = 96, slot = constants.spotanim_slot_combat)
            }
        }

        private fun PathingEntity.drainCombatStatFlat(stat: CombatStat, amount: Int) {
            when (this) {
                is Npc -> drainNpcStatFlat(stat, amount)
                is Player -> statSub(stat.playerStat, constant = amount, percent = 0)
            }
        }

        private fun PathingEntity.drainCombatStat(stat: CombatStat, percent: Int) {
            when (this) {
                is Npc -> drainNpcStat(stat, percent)
                is Player -> statSub(stat.playerStat, constant = 0, percent = percent)
            }
        }

        private fun Npc.drainNpcStat(stat: CombatStat, percent: Int) {
            fun drain(current: Int, base: Int): Int {
                val amount = max(1, (base * percent) / 100)
                return max(0, current - amount)
            }
            when (stat) {
                CombatStat.Attack -> attackLvl = drain(attackLvl, baseAttackLvl)
                CombatStat.Strength -> strengthLvl = drain(strengthLvl, baseStrengthLvl)
                CombatStat.Defence -> defenceLvl = drain(defenceLvl, baseDefenceLvl)
                CombatStat.Ranged -> rangedLvl = drain(rangedLvl, baseRangedLvl)
                CombatStat.Magic -> magicLvl = drain(magicLvl, baseMagicLvl)
            }
        }

        private fun Npc.drainNpcStatFlat(stat: CombatStat, amount: Int) {
            fun drain(current: Int): Int = max(0, current - amount)
            when (stat) {
                CombatStat.Attack -> attackLvl = drain(attackLvl)
                CombatStat.Strength -> strengthLvl = drain(strengthLvl)
                CombatStat.Defence -> defenceLvl = drain(defenceLvl)
                CombatStat.Ranged -> rangedLvl = drain(rangedLvl)
                CombatStat.Magic -> magicLvl = drain(magicLvl)
            }
        }

        private fun Npc.isRetainableVampyre(): Boolean {
            val name = visType.name
            val isVampyre = name.contains("Vampyre", ignoreCase = true)
            val isWeakType =
                name.contains("Juvenile", ignoreCase = true) ||
                    name.contains("Juvinate", ignoreCase = true)
            return isVampyre && isWeakType
        }

        private enum class CombatStat(val playerStat: String) {
            Attack("stat.attack"),
            Strength("stat.strength"),
            Defence("stat.defence"),
            Ranged("stat.ranged"),
            Magic("stat.magic"),
        }

        private fun Player.statValue(stat: String): Int = stat(stat)
    }
}
