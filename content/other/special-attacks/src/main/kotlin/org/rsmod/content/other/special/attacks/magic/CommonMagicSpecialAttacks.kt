package org.rsmod.content.other.special.attacks.magic

import jakarta.inject.Inject
import kotlin.math.max
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.config.constants
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statAdd
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.player.PlayerRepository
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MagicSpecialAttack
import org.rsmod.api.specials.instant.InstantSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.map.zone.ZoneKey

class CommonMagicSpecialAttacks
@Inject
constructor(
    private val npcRepo: NpcRepository,
    private val playerRepo: PlayerRepository,
) : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        for (obj in VolatileNightmareStaffs) {
            registerMagic(obj, StaffDamageSpec(manager, VolatileNightmareStaffSpec))
        }
        for (obj in EldritchNightmareStaffs) {
            registerMagic(obj, StaffDamageSpec(manager, EldritchNightmareStaffSpec))
        }
        for (obj in AccursedSceptres) {
            registerMagic(obj, StaffDamageSpec(manager, AccursedSceptreSpec))
        }
        for (obj in LithicSceptres) {
            registerMagic(obj, LithicShatter(manager, npcRepo, playerRepo))
        }
        for (obj in EyesOfAyak) {
            registerMagic(obj, StaffDamageSpec(manager, EyeOfAyakSpec))
        }
        for (obj in PurgingStaffs) {
            registerMagic(obj, StaffDamageSpec(manager, PurgingStaffSpec))
        }
        for (obj in Dawnbringers) {
            registerMagic(obj, StaffDamageSpec(manager, DawnbringerSpec))
        }
        for (obj in StaffsOfLight) {
            registerInstant(obj, StaffOfLight)
        }
    }

    private class LithicShatter(
        private val manager: SpecialAttackManager,
        private val npcRepo: NpcRepository,
        private val playerRepo: PlayerRepository,
    ) : MagicSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Staff,
        ): Boolean {
            shatter(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Staff,
        ): Boolean {
            shatter(target, attack)
            return true
        }

        private fun ProtectedAccess.shatter(primary: PathingEntity, attack: CombatAttack.Staff) {
            anim(LithicShatterAnim)
            spotanim(LithicShatterCastSpotanim, height = 96, slot = constants.spotanim_slot_combat)

            var total = 0
            for (target in targets(primary).take(LithicShatterMaxTargets)) {
                val multiplier = if (target === primary) LithicPrimaryMultiplier else LithicSplashMultiplier
                val maxHit =
                    manager.calculateStaffMaxHit(
                        source = this,
                        target = target,
                        baseMaxHit = LithicBaseMaxHit,
                        multiplier = multiplier,
                    )
                val damage = if (maxHit > 0) random.of(0..maxHit) else 0
                target.spotanim(LithicShatterImpactSpotanim, height = 96, delay = LithicShatterClientDelay)
                manager.queueMagicHit(this, target, damage, clientDelay = LithicShatterClientDelay, hitDelay = 1)
                total += damage
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
                                    it.coords.chebyshevDistance(player.coords) <= LithicShatterRadius
                            }
                    is Player ->
                        playerRepo
                            .findAll(zone, zoneRadius = 1)
                            .filter {
                                it !== player &&
                                    it !== primary &&
                                    it.isValidTarget() &&
                                    it.coords.level == player.coords.level &&
                                    it.coords.chebyshevDistance(player.coords) <= LithicShatterRadius
                            }
                }
            return sequenceOf(primary) + nearby
        }
    }

    private class StaffDamageSpec(
        private val manager: SpecialAttackManager,
        private val spec: MagicSpec,
    ) : MagicSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Staff,
        ): Boolean {
            cast(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Staff,
        ): Boolean {
            cast(target, attack)
            return true
        }

        private fun ProtectedAccess.cast(target: PathingEntity, attack: CombatAttack.Staff) {
            anim(spec.anim)
            if (spec.castSpotanim != null) {
                spotanim(spec.castSpotanim, height = 96, slot = constants.spotanim_slot_combat)
            }

            val clientDelay =
                if (spec.travelSpotanim != null) {
                    manager.spawnProjectile(this, target, spec.travelSpotanim, "projanim.arrow").clientCycles
                } else {
                    20
                }

            val hit =
                spec.guaranteed ||
                    manager.rollStaffAccuracy(
                        source = this,
                        target = target,
                        attackStyle = attack.style,
                        multiplier = spec.accuracyMultiplier,
                    )
            val damage =
                if (hit) {
                    manager.rollStaffMaxHit(
                        source = this,
                        target = target,
                        baseMaxHit = spec.baseMaxHit,
                        multiplier = spec.maxHitMultiplier,
                    )
                } else {
                    0
                }

            if (spec.impactSpotanim != null) {
                target.spotanim(spec.impactSpotanim, height = 96, delay = clientDelay)
            }
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMagicHit(this, target, damage, clientDelay = clientDelay)
            spec.onHit(this, target, damage)
            manager.continueCombat(this, target)
        }
    }

    private object StaffOfLight : InstantSpecialAttack {
        override suspend fun ProtectedAccess.activate(): Boolean {
            anim("seq.staff_of_light_special")
            spotanim("spotanim.staff_of_light_special_start", height = 96, slot = constants.spotanim_slot_combat)
            mes("You are shielded by the power of light.")
            return true
        }
    }

    private data class MagicSpec(
        val anim: String,
        val castSpotanim: String? = null,
        val travelSpotanim: String? = null,
        val impactSpotanim: String? = null,
        val baseMaxHit: Int,
        val accuracyMultiplier: Double = 1.0,
        val maxHitMultiplier: Double = 1.0,
        val guaranteed: Boolean = false,
        val onHit: ProtectedAccess.(PathingEntity, Int) -> Unit = { _, _ -> },
    )

    private companion object {
        private val VolatileNightmareStaffSpec =
            MagicSpec(
                anim = "seq.nightmare_staff_volatile_cast",
                castSpotanim = "spotanim.nightmare_staff_volatile_cast_spotanim",
                impactSpotanim = "spotanim.nightmare_staff_volatile_hit_spotanim",
                baseMaxHit = 50,
                accuracyMultiplier = 1.50,
            )
        private val EldritchNightmareStaffSpec =
            MagicSpec(
                anim = "seq.nightmare_staff_eldritch_player_cast",
                castSpotanim = "spotanim.nightmare_staff_eldritch_cast_spotanim",
                travelSpotanim = "spotanim.eldritch_smoke_travel",
                impactSpotanim = "spotanim.nightmare_staff_eldritch_hit_spotanim",
                baseMaxHit = 44,
                onHit = { _, damage ->
                    if (damage > 0) {
                        statAdd("stat.prayer", constant = max(1, damage / 2), percent = 0)
                    }
                },
            )
        private val AccursedSceptreSpec =
            MagicSpec(
                anim = "seq.human_special_accursed",
                impactSpotanim = "spotanim.fx_ursine02_special",
                baseMaxHit = 32,
                accuracyMultiplier = 1.50,
                maxHitMultiplier = 1.20,
                onHit = { target, damage ->
                    if (damage > 0) {
                        target.drainMagicDefence()
                    }
                },
            )
        private val EyeOfAyakSpec =
            MagicSpec(
                anim = "seq.nightmare_staff_special",
                castSpotanim = "spotanim.sp_attackglow_red",
                impactSpotanim = "spotanim.fx_ursine02_special",
                baseMaxHit = 32,
                accuracyMultiplier = 2.0,
                maxHitMultiplier = 1.30,
                onHit = { target, damage ->
                    if (damage > 0) {
                        target.drainMagicDefenceFlat(damage)
                    }
                },
            )
        private val PurgingStaffSpec =
            MagicSpec(
                anim = "seq.nightmare_staff_special",
                castSpotanim = "spotanim.sp_attackglow_red",
                impactSpotanim = "spotanim.fx_ursine02_special",
                baseMaxHit = 30,
                accuracyMultiplier = 1.25,
            )
        private val DawnbringerSpec =
            MagicSpec(
                anim = "seq.nightmare_staff_special",
                castSpotanim = "spotanim.dawnbringer_casting_spec",
                travelSpotanim = "spotanim.dawnbringer_projectile_spec",
                impactSpotanim = "spotanim.dawnbringer_impact_spec",
                baseMaxHit = 75,
                accuracyMultiplier = 2.0,
            )

        private val VolatileNightmareStaffs =
            arrayOf(
                "obj.nightmare_staff_volatile",
                "obj.br_nightmare_staff_volatile",
                "obj.deadman_nightmare_staff_volatile",
                "obj.deadman_blighted_volatile_staff",
            )
        private val EldritchNightmareStaffs = arrayOf("obj.nightmare_staff_eldritch")
        private val AccursedSceptres =
            arrayOf("obj.wild_cave_accursed_charged", "obj.wild_cave_accursed_charged_recol")
        private val LithicSceptres = arrayOf("obj.lithic_sceptre_charged")
        private val EyesOfAyak = arrayOf("obj.eye_of_ayak")
        private val PurgingStaffs = arrayOf("obj.purging_staff", "obj.br_purging_staff")
        private val Dawnbringers = arrayOf("obj.verzik_special_weapon")
        private val StaffsOfLight =
            arrayOf(
                "obj.staff_of_light",
                "obj.staff_of_balance",
                "obj.sotd",
                "obj.br_sotd",
                "obj.toxic_sotd",
                "obj.toxic_sotd_charged",
                "obj.toxic_sotd_deadman",
                "obj.toxic_sotd_charged_deadman",
            )

        private const val LithicBaseMaxHit = 32
        private const val LithicShatterRadius = 3
        private const val LithicShatterMaxTargets = 12
        private const val LithicShatterClientDelay = 20
        private const val LithicPrimaryMultiplier = 1.50
        private const val LithicSplashMultiplier = 0.75
        private const val LithicShatterAnim = "seq.shatter"
        private const val LithicShatterCastSpotanim = "spotanim.sp_attack_shatter_spotanim"
        private const val LithicShatterImpactSpotanim = "spotanim.sp_attack_shatter_spotanim"

        private fun PathingEntity.drainMagicDefence() {
            when (this) {
                is Npc -> {
                    magicLvl = max(0, magicLvl - max(1, baseMagicLvl * 15 / 100))
                    defenceLvl = max(0, defenceLvl - max(1, baseDefenceLvl * 15 / 100))
                }
                is Player -> {
                    statSub("stat.magic", constant = 0, percent = 15)
                    statSub("stat.defence", constant = 0, percent = 15)
                }
            }
        }

        private fun PathingEntity.drainMagicDefenceFlat(amount: Int) {
            when (this) {
                is Npc -> {
                    magicLvl = max(0, magicLvl - amount)
                    defenceLvl = max(0, defenceLvl - amount)
                }
                is Player -> {
                    statSub("stat.magic", constant = amount, percent = 0)
                    statSub("stat.defence", constant = amount, percent = 0)
                }
            }
        }
    }
}
