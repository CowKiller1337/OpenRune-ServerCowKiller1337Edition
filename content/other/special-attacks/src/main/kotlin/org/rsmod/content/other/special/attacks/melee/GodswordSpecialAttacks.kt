package org.rsmod.content.other.special.attacks.melee

import kotlin.math.min
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.CombatEffects
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.config.constants
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.baseHitpointsLvl
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

class GodswordSpecialAttacks : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        for (obj in ArmadylGodswords) {
            registerMelee(obj, Godsword(manager, GodswordEffect.Armadyl))
        }
        for (obj in BandosGodswords) {
            registerMelee(obj, Godsword(manager, GodswordEffect.Bandos))
        }
        for (obj in SaradominGodswords) {
            registerMelee(obj, Godsword(manager, GodswordEffect.Saradomin))
        }
        for (obj in ZamorakGodswords) {
            registerMelee(obj, Godsword(manager, GodswordEffect.Zamorak))
        }
        for (obj in AncientGodswords) {
            registerMelee(obj, Godsword(manager, GodswordEffect.Ancient))
        }
        for (obj in Dogswords) {
            registerMelee(obj, Godsword(manager, GodswordEffect.Dogsword))
        }
    }

    private class Godsword(
        private val manager: SpecialAttackManager,
        private val effect: GodswordEffect,
    ) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            swing(target, attack, pvp = false)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            swing(target, attack, pvp = true)
            return true
        }

        private fun ProtectedAccess.swing(
            target: PathingEntity,
            attack: CombatAttack.Melee,
            pvp: Boolean,
        ) {
            anim(effect.anim)
            spotanim(
                spot = effect.spotanim,
                slot = constants.spotanim_slot_combat,
                height = 96,
            )

            val damage =
                manager.rollMeleeDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = AccuracyMultiplier,
                    maxHitMultiplier = effect.maxHitMultiplier,
                    blockType = MeleeAttackType.Slash,
                )
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(this, target, damage)
            if (damage > 0) {
                applyEffect(target, damage, pvp)
            }
            manager.continueCombat(this, target)
        }

        private fun ProtectedAccess.applyEffect(
            target: PathingEntity,
            damage: Int,
            pvp: Boolean,
        ) {
            when (effect) {
                GodswordEffect.Armadyl -> Unit
                GodswordEffect.Bandos -> applyBandos(target, damage)
                GodswordEffect.Saradomin -> applySaradomin(damage)
                GodswordEffect.Zamorak -> applyZamorak(target)
                GodswordEffect.Ancient -> applyAncient(target, pvp, deadmanDogsword = false)
                GodswordEffect.Dogsword -> {
                    applyBandos(target, damage)
                    applySaradomin(damage)
                    applyZamorak(target)
                    applyAncient(target, pvp, deadmanDogsword = true)
                }
            }
        }

        private fun applyBandos(target: PathingEntity, damage: Int) {
            if (target !is Player) {
                return
            }
            var remaining = damage
            for (stat in BandosDrainStats) {
                val current = target.stat(stat)
                val drain = min(current, remaining)
                if (drain > 0) {
                    target.statSub(stat, constant = drain, percent = 0)
                    remaining -= drain
                }
                if (remaining <= 0) {
                    return
                }
            }
        }

        private fun ProtectedAccess.applySaradomin(damage: Int) {
            val hitpointsRestore = maxOf(SaradominMinimumHitpointsRestore, damage / 2)
            val prayerRestore = maxOf(SaradominMinimumPrayerRestore, damage / 4)
            if (hitpointsRestore > 0) {
                statHeal("stat.hitpoints", constant = hitpointsRestore, percent = 0)
            }
            if (prayerRestore > 0) {
                statHeal("stat.prayer", constant = prayerRestore, percent = 0)
            }
        }

        private fun applyZamorak(target: PathingEntity) {
            if (target is Player) {
                CombatEffects.freeze(target, ZamorakFreezeTicks)
            }
        }

        private fun ProtectedAccess.applyAncient(
            target: PathingEntity,
            pvp: Boolean,
            deadmanDogsword: Boolean,
        ) {
            val sacrificeDamage = if (pvp && deadmanDogsword) DeadmanDogswordPvpSacrificeDamage else AncientSacrificeDamage
            val sacrificeHeal =
                if (pvp && deadmanDogsword) {
                    DeadmanDogswordPvpSacrificeHeal
                } else {
                    min(AncientSacrificeDamage, (target.maxHitpointsLevel() * AncientHealPercent) / 100)
                }
            manager.queueMeleeHit(this, target, sacrificeDamage, delay = AncientSacrificeDelay)
            if (sacrificeHeal > 0) {
                statHeal("stat.hitpoints", constant = sacrificeHeal, percent = 0)
            }
        }
    }

    private enum class GodswordEffect(
        val maxHitMultiplier: Double,
        val anim: String,
        val spotanim: String,
    ) {
        Armadyl(
            maxHitMultiplier = 1.375,
            anim = "seq.ags_special_player",
            spotanim = "spotanim.dh_sword_update_armadyl_special_spotanim",
        ),
        Bandos(
            maxHitMultiplier = 1.21,
            anim = "seq.bgs_special_player",
            spotanim = "spotanim.dh_sword_update_bandos_special_spotanim",
        ),
        Saradomin(
            maxHitMultiplier = 1.10,
            anim = "seq.sgs_special_player",
            spotanim = "spotanim.dh_sword_update_saradomin_special_spotanim",
        ),
        Zamorak(
            maxHitMultiplier = 1.10,
            anim = "seq.zgs_special_player",
            spotanim = "spotanim.dh_sword_update_zamorak_special_spotanim",
        ),
        Ancient(
            maxHitMultiplier = 1.10,
            anim = "seq.ngs_special_player",
            spotanim = "spotanim.ngs_special_spotanim",
        ),
        Dogsword(
            maxHitMultiplier = 1.375,
            anim = "seq.the_godsword_special_attack",
            spotanim = "spotanim.league_5_godsword_spotanim",
        ),
    }

    private companion object {
        private const val AccuracyMultiplier = 2.0
        private const val AncientSacrificeDamage = 25
        private const val AncientSacrificeDelay = 8
        private const val AncientHealPercent = 15
        private const val DeadmanDogswordPvpSacrificeDamage = 15
        private const val DeadmanDogswordPvpSacrificeHeal = 10
        private const val SaradominMinimumHitpointsRestore = 10
        private const val SaradominMinimumPrayerRestore = 5
        private const val ZamorakFreezeTicks = 33

        private val BandosDrainStats =
            listOf(
                "stat.defence",
                "stat.strength",
                "stat.prayer",
                "stat.attack",
                "stat.magic",
                "stat.ranged",
            )

        private val ArmadylGodswords = intArrayOf(11802, 20368, 20593, 28537, 29605)
        private val BandosGodswords = intArrayOf(11804, 20370)
        private val SaradominGodswords = intArrayOf(11806, 20372)
        private val ZamorakGodswords = intArrayOf(11808, 20374)
        private val AncientGodswords = intArrayOf(26233, 27184)
        private val Dogswords = intArrayOf(30367, 33038)

        private fun PathingEntity.maxHitpointsLevel(): Int =
            when (this) {
                is Npc -> type.hitpoints
                is Player -> baseHitpointsLvl
            }
    }
}
