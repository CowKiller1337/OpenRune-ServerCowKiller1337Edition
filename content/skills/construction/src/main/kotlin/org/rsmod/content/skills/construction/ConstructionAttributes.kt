package org.rsmod.content.skills.construction

import org.rsmod.api.attr.AttributeKey

internal val CONSTRUCTION_HOTSPOT_ORIGINAL_ATTR: AttributeKey<MutableMap<String, Int>> =
    AttributeKey(persistenceKey = "construction.hotspot.original")

internal val CONSTRUCTION_BUILD_MODE_ATTR: AttributeKey<Boolean> = AttributeKey(temp = true)
