package org.rsmod.content.other.special.attacks

import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.content.other.special.attacks.boost.StatBoostSpecialAttacks
import org.rsmod.content.other.special.attacks.magic.CommonMagicSpecialAttacks
import org.rsmod.content.other.special.attacks.melee.CommonMeleeSpecialAttacks
import org.rsmod.content.other.special.attacks.melee.DragonLongswordSpecialAttack
import org.rsmod.content.other.special.attacks.melee.GodswordSpecialAttacks
import org.rsmod.content.other.special.attacks.ranged.CommonRangedSpecialAttacks
import org.rsmod.content.other.special.attacks.ranged.DarkBowSpecialAttack
import org.rsmod.content.other.special.attacks.shield.ShieldSpecialAttacks
import org.rsmod.plugin.module.PluginModule

class SpecialAttackModule : PluginModule() {
    override fun bind() {
        addSetBinding<SpecialAttackMap>(StatBoostSpecialAttacks::class.java)
        addSetBinding<SpecialAttackMap>(DarkBowSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(CommonRangedSpecialAttacks::class.java)
        addSetBinding<SpecialAttackMap>(CommonMeleeSpecialAttacks::class.java)
        addSetBinding<SpecialAttackMap>(CommonMagicSpecialAttacks::class.java)
        addSetBinding<SpecialAttackMap>(DragonLongswordSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(GodswordSpecialAttacks::class.java)
        addSetBinding<SpecialAttackMap>(ShieldSpecialAttacks::class.java)
    }
}
