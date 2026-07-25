package org.rsmod.content.skills.agility

import org.rsmod.api.attr.AttributeKey

internal val GNOME_STRONGHOLD_COURSE_STEP_ATTR: AttributeKey<Int> =
    AttributeKey(temp = true)

internal val GNOME_STRONGHOLD_LAPS_ATTR: AttributeKey<Int> =
    AttributeKey(persistenceKey = "agility.gnome_stronghold_laps")
