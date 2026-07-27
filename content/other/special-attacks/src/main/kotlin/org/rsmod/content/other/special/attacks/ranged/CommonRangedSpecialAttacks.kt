package org.rsmod.content.other.special.attacks.ranged

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import kotlin.math.max
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.CombatEffects
import org.rsmod.api.combat.manager.RangedAmmoManager
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.quiver
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.RangedSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.type.getInvObj
import org.rsmod.game.type.getOrNull

class CommonRangedSpecialAttacks @Inject constructor(private val ammunition: RangedAmmoManager) :
    SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        for (obj in MagicLongbows) {
            registerRanged(obj, RangedSpecAttack(manager, ammunition, MagicLongbowSpec))
        }
        for (obj in MagicShortbows) {
            registerRanged(obj, RangedSpecAttack(manager, ammunition, MagicShortbowSpec))
        }
        for (obj in ArmadylCrossbows) {
            registerRanged(obj, RangedSpecAttack(manager, ammunition, ArmadylCrossbowSpec))
        }
        for (obj in ZaryteCrossbows) {
            registerRanged(obj, RangedSpecAttack(manager, ammunition, ZaryteCrossbowSpec))
        }
        for (obj in DragonCrossbows) {
            registerRanged(obj, RangedSpecAttack(manager, ammunition, DragonCrossbowSpec))
        }
        for (obj in Ballistas) {
            registerRanged(obj, RangedSpecAttack(manager, ammunition, BallistaSpec))
        }
        for (obj in DragonKnives) {
            registerRanged(obj, RangedSpecAttack(manager, ammunition, DragonKnifeSpec))
        }
        for (obj in DragonThrownaxes) {
            registerRanged(obj, RangedSpecAttack(manager, ammunition, DragonThrownaxeSpec))
        }
        for (obj in RuneThrownaxes) {
            registerRanged(obj, RangedSpecAttack(manager, ammunition, RuneThrownaxeSpec))
        }
        for (obj in DorgeshuunCrossbows) {
            registerRanged(obj, RangedSpecAttack(manager, ammunition, DorgeshuunCrossbowSpec))
        }
        for (obj in MorrigansThrownaxes) {
            registerRanged(obj, RangedSpecAttack(manager, ammunition, MorrigansThrownaxeSpec))
        }
        for (obj in BountyMorrigansThrownaxes) {
            registerRanged(obj, RangedSpecAttack(manager, ammunition, BountyMorrigansThrownaxeSpec))
        }
        for (obj in MorrigansJavelins) {
            registerRanged(obj, RangedSpecAttack(manager, ammunition, MorrigansJavelinSpec))
        }
        for (obj in BountyMorrigansJavelins) {
            registerRanged(obj, RangedSpecAttack(manager, ammunition, BountyMorrigansJavelinSpec))
        }
        for (obj in ToxicBlowpipes) {
            registerRanged(obj, RangedSpecAttack(manager, ammunition, ToxicBlowpipeSpec))
        }
        for (obj in EclipseAtlatls) {
            registerRanged(obj, RangedSpecAttack(manager, ammunition, EclipseAtlatlSpec))
        }
        for (obj in TonalzticsOfRalos) {
            registerRanged(obj, RangedSpecAttack(manager, ammunition, TonalzticsOfRalosSpec))
        }
        for (obj in ScorchingBows) {
            registerRanged(obj, RangedSpecAttack(manager, ammunition, ScorchingBowSpec))
        }
        for (obj in WebweaverBows) {
            registerRanged(obj, RangedSpecAttack(manager, ammunition, WebweaverBowSpec))
        }
        for (obj in Seerculls) {
            registerRanged(obj, RangedSpecAttack(manager, ammunition, SeercullSpec))
        }
    }

    private class RangedSpecAttack(
        private val manager: SpecialAttackManager,
        private val ammunition: RangedAmmoManager,
        private val spec: RangedSpec,
    ) : RangedSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Ranged,
        ): Boolean {
            shoot(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Ranged,
        ): Boolean {
            shoot(target, attack)
            return true
        }

        private fun ProtectedAccess.shoot(target: PathingEntity, attack: CombatAttack.Ranged) {
            val weaponType = getInvObj(attack.weapon)
            val quiverType = getOrNull(player.quiver)
            val usingThrown = weaponType.isCategoryType("category.throwing_weapon")
            val ammoType = if (usingThrown) weaponType else quiverType

            if (!ammunition.attemptAmmoUsage(player, weaponType, quiverType)) {
                manager.stopCombat(this)
                return
            }

            val ammoCount = if (usingThrown) player.righthand?.count ?: 0 else player.quiver?.count ?: 0
            if (ammoCount < spec.ammoUse) {
                manager.stopCombat(this)
                mes(spec.notEnoughAmmoMessage)
                return
            }

            val projectileType =
                spec.projanim ?: weaponType.paramOrNull(params.proj_type)?.let {
                    projanimMappingOrNull(it.id)
                }
            if (projectileType == null) {
                manager.stopCombat(this)
                mes("You are unable to fire your ammunition.")
                return
            }

            val travelSpot =
                spec.travelSpotanim
                    ?: ammoType?.paramOrNull(params.proj_travel)?.let {
                        spotanimMappingOrNull(it.id)
                    }
            if (travelSpot == null) {
                manager.stopCombat(this)
                mes("You are unable to fire your ammunition.")
                return
            }

            anim(spec.anim)
            val launchSpot =
                spec.launchSpotanim
                    ?: ammoType?.paramOrNull(params.proj_launch)?.let {
                        spotanimMappingOrNull(it.id)
                    }
            if (launchSpot != null) {
                spotanim(launchSpot, height = 96, slot = constants.spotanim_slot_combat)
            }

            val projectile = manager.spawnProjectile(this, target, travelSpot, projectileType)
            val hitDelay = projectile.serverCycles
            val clientDelay = projectile.clientCycles

            if (usingThrown) {
                ammunition.useThrownWeapon(player, weaponType, target.coords, hitDelay, useCount = spec.ammoUse)
            } else if (ammoType != null) {
                ammunition.useQuiverAmmo(player, ammoType, target.coords, hitDelay, useCount = spec.ammoUse)
            }

            var total = 0
            repeat(spec.hits) {
                val damage = rollDamage(target, attack)
                total += damage
                manager.queueRangedHit(this, target, if (usingThrown) null else ammoType, damage, clientDelay, hitDelay)
            }
            manager.giveCombatXp(this, target, attack, total)
            spec.onHit(this, target, total, clientDelay)
            if (usingThrown && player.righthand == null) {
                mes("That was your last one!")
                return
            }
            manager.continueCombat(this, target)
        }

        private fun ProtectedAccess.rollDamage(target: PathingEntity, attack: CombatAttack.Ranged): Int {
            val accurate =
                spec.guaranteed ||
                    manager.rollRangedAccuracy(
                        source = this,
                        target = target,
                        attackType = attack.type,
                        attackStyle = attack.style,
                        blockType = attack.type,
                        multiplier = spec.accuracyMultiplier,
                    )
            if (!accurate) {
                return 0
            }
            val maxHit =
                manager.calculateRangedMaxHit(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    multiplier = spec.maxHitMultiplier,
                    boltSpecDamage = 0,
                )
            val minHit = spec.minimumHit.coerceAtMost(maxHit)
            return random.of(minHit..max(minHit, maxHit))
        }
    }

    private data class RangedSpec(
        val anim: String,
        val launchSpotanim: String? = null,
        val travelSpotanim: String? = null,
        val projanim: String? = null,
        val accuracyMultiplier: Double = 1.0,
        val maxHitMultiplier: Double = 1.0,
        val hits: Int = 1,
        val ammoUse: Int = hits,
        val minimumHit: Int = 0,
        val guaranteed: Boolean = false,
        val notEnoughAmmoMessage: String = "You do not have enough ammunition for this special attack.",
        val onHit: ProtectedAccess.(PathingEntity, Int, Int) -> Unit = { _, _, _ -> },
    )

    private companion object {
        private val MagicLongbowSpec =
            RangedSpec(anim = "seq.human_bow", accuracyMultiplier = 1.0, guaranteed = true)
        private val MagicShortbowSpec =
            RangedSpec(
                anim = "seq.human_bow",
                accuracyMultiplier = 0.75,
                hits = 2,
                ammoUse = 2,
                notEnoughAmmoMessage = "You need to have at least 2 arrows in your quiver for this special attack.",
            )
        private val ArmadylCrossbowSpec =
            RangedSpec(
                anim = "seq.human_crossbow",
                travelSpotanim = "spotanim.acb_crossbowbolt_travel",
                accuracyMultiplier = 2.0,
            )
        private val ZaryteCrossbowSpec =
            ArmadylCrossbowSpec.copy(accuracyMultiplier = 2.0)
        private val DragonCrossbowSpec =
            RangedSpec(
                anim = "seq.human_crossbow",
                travelSpotanim = "spotanim.dragon_crossbowbolt_travel",
                accuracyMultiplier = 1.0,
                maxHitMultiplier = 1.20,
            )
        private val BallistaSpec =
            RangedSpec(
                anim = "seq.ballista_special_attack",
                launchSpotanim = "spotanim.ballista_special",
                travelSpotanim = "spotanim.ballista_travel",
                accuracyMultiplier = 1.25,
                maxHitMultiplier = 1.25,
            )
        private val DragonKnifeSpec =
            RangedSpec(
                anim = "seq.human_dragon_knife",
                launchSpotanim = "spotanim.dragon_tknife_launch",
                travelSpotanim = "spotanim.dragon_tknife_travel_spec",
                projanim = "projanim.arrow",
                hits = 2,
                ammoUse = 2,
                notEnoughAmmoMessage = "You need to have at least 2 knives to use this special attack.",
            )
        private val DragonThrownaxeSpec =
            RangedSpec(
                anim = "seq.d_tknife_launch",
                launchSpotanim = "spotanim.dragon_taxe_launch_spec",
                travelSpotanim = "spotanim.dragon_taxe_travel_spec",
                projanim = "projanim.arrow",
                accuracyMultiplier = 1.25,
                ammoUse = 1,
            )
        private val RuneThrownaxeSpec =
            RangedSpec(
                anim = "seq.human_stake2_pvn",
                ammoUse = 1,
                notEnoughAmmoMessage = "You need to have at least 1 thrownaxe to use this special attack.",
            )
        private val DorgeshuunCrossbowSpec =
            RangedSpec(
                anim = "seq.xbows_human_fire_and_reload_pvn",
                accuracyMultiplier = 2.0,
                onHit = { target, damage, _ ->
                    if (damage > 0) {
                        target.drainDefence(damage)
                    }
                },
            )
        private val MorrigansThrownaxeSpec =
            RangedSpec(
                anim = "seq.human_stake2",
                launchSpotanim = "spotanim.morrigans_taxe_launch",
                travelSpotanim = "spotanim.morrigans_taxe_travel",
                projanim = "projanim.thrown",
                maxHitMultiplier = 1.20,
                onHit = { target, damage, _ ->
                    if (damage > 0 && target is Player) {
                        target.runEnergy = 0
                    }
                },
            )
        private val BountyMorrigansThrownaxeSpec =
            MorrigansThrownaxeSpec.copy(accuracyMultiplier = 1.50, maxHitMultiplier = 1.50)
        private val MorrigansJavelinSpec =
            RangedSpec(
                anim = "seq.human_stake2",
                launchSpotanim = "spotanim.morrigans_taxe_launch",
                travelSpotanim = "spotanim.morrigans_taxe_travel",
                projanim = "projanim.thrown",
                onHit = { target, damage, _ ->
                    if (damage > 0) {
                        target.drainDefence(max(1, damage / 4))
                    }
                },
            )
        private val BountyMorrigansJavelinSpec =
            MorrigansJavelinSpec.copy(accuracyMultiplier = 1.50, maxHitMultiplier = 1.50)
        private val ToxicBlowpipeSpec =
            RangedSpec(
                anim = "seq.toxic_blowpipe_special_updated",
                launchSpotanim = "spotanim.toxic_blowpipe_specialattack",
                accuracyMultiplier = 2.0,
                maxHitMultiplier = 1.50,
                onHit = { _, damage, _ ->
                    if (damage > 0) {
                        statAdd("stat.hitpoints", constant = damage / 2, percent = 0)
                    }
                },
            )
        private val EclipseAtlatlSpec =
            RangedSpec(
                anim = "seq.human_stake2",
                launchSpotanim = "spotanim.dragon_tknife_launch",
                travelSpotanim = "spotanim.dragon_tknife_travel_spec",
                projanim = "projanim.thrown",
                accuracyMultiplier = 1.50,
            )
        private val TonalzticsOfRalosSpec =
            RangedSpec(
                anim = "seq.human_stake2",
                launchSpotanim = "spotanim.dragon_taxe_launch_spec",
                travelSpotanim = "spotanim.dragon_taxe_travel_spec",
                projanim = "projanim.thrown",
                accuracyMultiplier = 1.25,
                onHit = { target, damage, _ ->
                    if (damage > 0) {
                        target.drainDefence(max(1, damage / 2))
                    }
                },
            )
        private val ScorchingBowSpec =
            RangedSpec(
                anim = "seq.vfx_human_scorching_bow_special_attack_01",
                launchSpotanim = "spotanim.vfx_scorching_bow_spotanim",
                travelSpotanim = "spotanim.vfx_scorching_bow_projectile",
                accuracyMultiplier = 1.30,
                onHit = { target, damage, clientDelay ->
                    if (damage > 0 && target is Player) {
                        target.spotanim("spotanim.vfx_scorching_bow_end_spotanim", height = 96, delay = clientDelay)
                        CombatEffects.freeze(target, 20)
                    }
                },
            )
        private val SeercullSpec =
            MagicLongbowSpec.copy(
                onHit = { target, damage, _ ->
                    if (damage > 0 && target is Player) {
                        target.statSub("stat.magic", constant = damage, percent = 0)
                    }
                }
            )
        private val WebweaverBowSpec =
            RangedSpec(
                anim = "seq.human_bow",
                hits = 4,
                ammoUse = 4,
                maxHitMultiplier = 0.40,
                notEnoughAmmoMessage = "You need to have at least 4 arrows to use this special attack.",
                onHit = { target, damage, _ ->
                    if (damage > 0 && target is Player) {
                        CombatEffects.poison(target, 4)
                    }
                },
            )

        private val MagicLongbows =
            arrayOf("obj.magic_longbow", "obj.trail_composite_bow_magic")
        private val MagicShortbows =
            arrayOf("obj.magic_shortbow", "obj.br_magic_bow", "obj.magic_shortbow_i")
        private val ArmadylCrossbows = arrayOf("obj.acb", "obj.br_acb")
        private val ZaryteCrossbows = arrayOf("obj.zaryte_xbow", "obj.br_zaryte_xbow")
        private val DragonCrossbows =
            arrayOf("obj.xbows_crossbow_dragon", "obj.br_xbows_crossbow_dragon", "obj.bh_xbows_crossbow_dragon_corrupted")
        private val Ballistas =
            arrayOf(
                "obj.heavy_ballista",
                "obj.br_heavy_ballista",
                "obj.heavy_ballista_ornament",
                "obj.light_ballista",
                "obj.br_light_ballista",
            )
        private val DragonKnives =
            arrayOf("obj.dragon_knife", "obj.br_dragon_knife", "obj.dragon_knife_p", "obj.dragon_knife_p+", "obj.dragon_knife_p++")
        private val DragonThrownaxes = arrayOf("obj.dragon_thrownaxe", "obj.br_dragon_thrownaxe")
        private val RuneThrownaxes = arrayOf("obj.rune_thrownaxe")
        private val DorgeshuunCrossbows = arrayOf("obj.dttd_bone_crossbow")
        private val MorrigansThrownaxes = arrayOf("obj.morrigans_thrownaxe")
        private val BountyMorrigansThrownaxes = arrayOf("obj.morrigans_thrownaxe_bh")
        private val MorrigansJavelins = arrayOf("obj.morrigans_javelin", "obj.br_morrigans_javelin")
        private val BountyMorrigansJavelins = arrayOf("obj.morrigans_javelin_bh")
        private val ToxicBlowpipes =
            arrayOf("obj.toxic_blowpipe_loaded", "obj.toxic_blowpipe_loaded_ornament", "obj.rosewood_blowpipe")
        private val EclipseAtlatls = arrayOf("obj.eclipse_atlatl", "obj.br_eclipse_atlatl")
        private val TonalzticsOfRalos =
            arrayOf("obj.tonalztics_of_ralos_charged", "obj.tonalztics_of_ralos_uncharged")
        private val ScorchingBows = arrayOf("obj.scorching_bow")
        private val WebweaverBows = arrayOf("obj.wild_cave_webweaver_charged")
        private val Seerculls = arrayOf("obj.daganoth_cave_magic_shortbow")

        private fun PathingEntity.drainDefence(amount: Int) {
            when (this) {
                is Npc -> defenceLvl = max(0, defenceLvl - amount)
                is Player -> statSub("stat.defence", constant = amount, percent = 0)
            }
        }

        private fun spotanimMappingOrNull(id: Int): String? {
            if (id == 0xFFFF || id < 0) {
                return null
            }
            return runCatching { RSCM.getReverseMapping(RSCMType.SPOTANIM, id) }.getOrNull()
        }

        private fun projanimMappingOrNull(id: Int): String? {
            if (id < 0) {
                return null
            }
            return runCatching { RSCM.getReverseMapping(RSCMType.PROJANIM, id) }.getOrNull()
        }
    }
}
