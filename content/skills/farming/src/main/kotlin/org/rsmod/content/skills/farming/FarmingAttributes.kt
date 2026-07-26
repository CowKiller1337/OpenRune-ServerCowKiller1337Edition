package org.rsmod.content.skills.farming

import org.rsmod.api.attr.AttributeKey

internal val FARMING_PATCH_RECIPE_ATTR: AttributeKey<MutableMap<String, Int>> =
    AttributeKey(persistenceKey = "farming.patch.recipe")

internal val FARMING_PATCH_PLANTED_AT_ATTR: AttributeKey<MutableMap<String, Int>> =
    AttributeKey(persistenceKey = "farming.patch.planted_at")

internal val FARMING_PATCH_HARVESTS_LEFT_ATTR: AttributeKey<MutableMap<String, Int>> =
    AttributeKey(persistenceKey = "farming.patch.harvests_left")

internal val FARMING_PATCH_COMPOST_ATTR: AttributeKey<MutableMap<String, Int>> =
    AttributeKey(persistenceKey = "farming.patch.compost")

internal val FARMING_PATCH_WATERED_ATTR: AttributeKey<MutableMap<String, Int>> =
    AttributeKey(persistenceKey = "farming.patch.watered")

internal val FARMING_PATCH_HEALTH_CHECKED_ATTR: AttributeKey<MutableMap<String, Int>> =
    AttributeKey(persistenceKey = "farming.patch.health_checked")
