package org.rsmod.content.other.special.attacks.shield

import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.config.constants
import org.rsmod.api.player.lefthand
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.enumVarp
import org.rsmod.api.script.onOpWorn2
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.SpecialAttackType
import org.rsmod.api.specials.combat.ShieldSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.isType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ShieldSpecialAttacks : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        registerShield("obj.dragonfire_shield", DragonfireShield(manager, DragonfireSpec))
        registerShield("obj.dragonfire_ward", DragonfireShield(manager, DragonfireSpec))
        registerShield("obj.wyvern_shield", DragonfireShield(manager, WyvernIceSpec))
    }

    private class DragonfireShield(
        private val manager: SpecialAttackManager,
        private val spec: ShieldSpec,
    ) : ShieldSpecialAttack {
        override suspend fun ProtectedAccess.attack(target: Npc): Boolean {
            breathe(target)
            return true
        }

        override suspend fun ProtectedAccess.attack(target: Player): Boolean {
            breathe(target)
            return true
        }

        private fun ProtectedAccess.breathe(target: PathingEntity) {
            val availableAt = player.attr[DRAGONFIRE_SHIELD_COOLDOWN_ATTR] ?: 0
            if (mapClock < availableAt) {
                mes("The shield needs time to recharge.")
                manager.stopCombat(this)
                return
            }

            player.attr[DRAGONFIRE_SHIELD_COOLDOWN_ATTR] = mapClock + SHIELD_SPECIAL_COOLDOWN
            anim(spec.anim)
            spotanim(spec.launchSpotanim, height = 96, slot = constants.spotanim_slot_combat)
            manager.soundArea(player, spec.sound)

            val projectile = manager.spawnProjectile(this, target, spec.travelSpotanim, spec.projanim)
            val clientDelay = projectile.clientCycles
            val hitDelay = projectile.serverCycles
            val damage = random.of(0..spec.maxHit)
            manager.queueMagicHit(this, target, damage, clientDelay, hitDelay)
            manager.continueCombat(this, target)
        }
    }

    private data class ShieldSpec(
        val anim: String,
        val launchSpotanim: String,
        val travelSpotanim: String,
        val projanim: String,
        val sound: String,
        val maxHit: Int,
    )

    private companion object {
        private const val SHIELD_SPECIAL_COOLDOWN = 200
        private val DRAGONFIRE_SHIELD_COOLDOWN_ATTR = AttributeKey<Int>(temp = true)

        private val DragonfireSpec =
            ShieldSpec(
                anim = "seq.human_unarmedblock",
                launchSpotanim = "spotanim.sp_attackglow_red",
                travelSpotanim = "spotanim.dragon_ranged_fire_attack",
                projanim = "projanim.dragonfire",
                sound = "synth.firebreath",
                maxHit = 25,
            )

        private val WyvernIceSpec =
            ShieldSpec(
                anim = "seq.human_unarmedblock",
                launchSpotanim = "spotanim.wyvern_skeleton_launch_iceball",
                travelSpotanim = "spotanim.wyvern_skeleton_travel_iceball",
                projanim = "projanim.dragonfire",
                sound = "synth.firebreath",
                maxHit = 25,
            )
    }
}

class ShieldSpecialAttackScript : PluginScript() {
    private var Player.specialType by enumVarp<SpecialAttackType>("varp.sa_attack")

    override fun ScriptContext.startup() {
        for (shield in ShieldSpecials) {
            onOpWorn2(shield) { toggleShieldSpecial(shield) }
        }
    }

    private fun ProtectedAccess.toggleShieldSpecial(shield: String) {
        if (player.lefthand?.isType(shield) != true) {
            return
        }
        player.specialType =
            if (player.specialType == SpecialAttackType.Shield) {
                SpecialAttackType.None
            } else {
                SpecialAttackType.Shield
            }
    }

    private companion object {
        private val ShieldSpecials =
            listOf(
                "obj.dragonfire_shield",
                "obj.dragonfire_ward",
                "obj.wyvern_shield",
            )
    }
}
