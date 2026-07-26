package org.rsmod.content.skills.hunter

import org.rsmod.api.attr.AttributeKey

internal val HUNTER_TRAPS_ATTR: AttributeKey<MutableMap<String, HunterTrapState>> =
    AttributeKey(temp = true)
