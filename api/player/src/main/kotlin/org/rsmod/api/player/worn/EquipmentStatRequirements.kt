package org.rsmod.api.player.worn

import dev.openrune.types.ItemServerType
import org.rsmod.api.config.aliases.ParamInt
import org.rsmod.api.config.aliases.ParamStat
import org.rsmod.api.config.refs.BaseParams
import org.rsmod.game.stat.StatRequirement

public object EquipmentStatRequirements {
    private val reqParams: List<Pair<ParamStat, ParamInt>> =
        listOf(
            BaseParams.statreq1_skill to BaseParams.statreq1_level,
            BaseParams.statreq2_skill to BaseParams.statreq2_level,
            BaseParams.statreq3_skill to BaseParams.statreq3_level,
            BaseParams.statreq4_skill to BaseParams.statreq4_level,
            BaseParams.statreq5_skill to BaseParams.statreq5_level,
            BaseParams.statreq6_skill to BaseParams.statreq6_level,
            BaseParams.statreq7_skill to BaseParams.statreq7_level,
        )

    public fun list(type: ItemServerType): List<StatRequirement> =
        reqParams.mapNotNull { (skillParam, levelParam) ->
            val skill = type.paramOrNull(skillParam) ?: return@mapNotNull null
            val level = type.paramOrNull(levelParam) ?: return@mapNotNull null
            if (level <= 0) {
                return@mapNotNull null
            }
            StatRequirement(skill, level)
        }
}

public fun ItemServerType.equipmentStatRequirements(): List<StatRequirement> =
    EquipmentStatRequirements.list(this)
