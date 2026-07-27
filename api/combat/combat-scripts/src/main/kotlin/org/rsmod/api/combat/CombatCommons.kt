package org.rsmod.api.combat

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.config.constants

internal const val ACTIVE_COMBAT_DELAY = constants.combat_activecombat_delay

internal const val NULL_SPOTANIM_ID = 0xFFFF
internal const val MAX_ATTACK_RANGE = 10
internal const val MAGIC_ATTACK_RANGE = MAX_ATTACK_RANGE
internal const val MAGIC_STAFF_ATTACK_RATE = constants.combat_pstaff_attackrate
internal const val MAGIC_SPELL_ATTACK_RATE = constants.combat_spell_attackrate

internal fun spotanimMappingOrNull(id: Int): String? {
    if (id == NULL_SPOTANIM_ID || id < 0) {
        return null
    }
    return runCatching { RSCM.getReverseMapping(RSCMType.SPOTANIM, id) }.getOrNull()
}

internal fun projanimMappingOrNull(id: Int): String? {
    if (id < 0) {
        return null
    }
    return runCatching { RSCM.getReverseMapping(RSCMType.PROJANIM, id) }.getOrNull()
}
