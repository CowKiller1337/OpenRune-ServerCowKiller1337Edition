package org.rsmod.content.other.commands

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.NpcServerType
import dev.openrune.types.SequenceServerType
import dev.openrune.util.WeaponCategory
import jakarta.inject.Inject
import java.nio.file.Files
import java.nio.file.Path
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.worn.equipmentStatRequirements
import org.rsmod.api.specials.SpecialAttackRegistry
import org.rsmod.api.specials.weapon.SpecialAttackWeaponInfo
import org.rsmod.api.specials.weapon.SpecialAttackWeapons
import org.rsmod.api.weapons.WeaponRegistry
import org.rsmod.game.cheat.Cheat
import org.rsmod.game.inv.InvObj
import org.rsmod.game.stat.StatRequirement
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class AdminDefinitionDebugCommands
@Inject
constructor(
    private val specialWeapons: SpecialAttackWeapons,
    private val specialRegistry: SpecialAttackRegistry,
    private val weaponsRegistry: WeaponRegistry,
) : PluginScript() {
    private val logger = InlineLogger()

    override fun ScriptContext.startup() {
        onCommand("objdebug", "Show loaded obj definition details", ::objDebug) {
            invalidArgs = "Use as ::objdebug objNameOrId"
        }
        onCommand("itemdebug", "Show loaded obj definition details", ::objDebug) {
            invalidArgs = "Use as ::itemdebug objNameOrId"
        }
        onCommand("npcdebug", "Show loaded npc definition details", ::npcDebug) {
            invalidArgs = "Use as ::npcdebug npcNameOrId"
        }
        onCommand("seqdebug", "Show loaded seq definition details", ::seqDebug) {
            invalidArgs = "Use as ::seqdebug seqNameOrId"
        }
        onCommand("spotdebug", "Show loaded spotanim mapping details", ::spotDebug) {
            invalidArgs = "Use as ::spotdebug spotanimNameOrId"
        }
        onCommand("specaudit", "Audit special attack cache entries against handlers", ::specAudit) {
            invalidArgs = "Use as ::specaudit [missing|handled|all] [search]"
        }
        onCommand("weaponaudit", "Audit weapon cache entries for broken combat params", ::weaponAudit) {
            invalidArgs = "Use as ::weaponaudit [critical|visual|all] [search]"
        }
    }

    private fun objDebug(cheat: Cheat) =
        with(cheat) {
            val type = resolveObj(args) ?: run {
                player.mes("Obj not found: ${args.joinToString(" ")}")
                return@with
            }
            val reqs = type.equipmentStatRequirements()
            val specialEnergy = runCatching { specialWeapons.getSpecialEnergy(type.id) }.getOrNull()
            val specialDesc = runCatching { specialWeapons.getSpecialDescription(type.id) }.getOrNull()
            val special =
                when {
                    specialEnergy == null -> "none"
                    specialRegistry.contains(type.id) -> "${specialEnergy / 10}% handler=yes"
                    else -> "${specialEnergy / 10}% handler=no"
                }

            player.mes("Obj: ${type.name} [${type.id}] ${safeMapping(RSCMType.OBJ, type.id)}")
            player.mes(
                "wear=${type.wearpos1}/${type.wearpos2}/${type.wearpos3} stack=${type.stacks} " +
                    "cat=${label(RSCMType.CATEGORY, type.category)} weapon=${type.weaponCategory.text} special=$special"
            )
            player.mes("reqs=${reqs.formatReqs()} atkRate=${type.paramOrDash(params.attackrate)}")
            player.mes(
                "atk=${type.paramOrDash(params.attack_stab)}/${type.paramOrDash(params.attack_slash)}/" +
                    "${type.paramOrDash(params.attack_crush)}/${type.paramOrDash(params.attack_magic)}/" +
                    type.paramOrDash(params.attack_ranged)
            )
            player.mes(
                "def=${type.paramOrDash(params.defence_stab)}/${type.paramOrDash(params.defence_slash)}/" +
                    "${type.paramOrDash(params.defence_crush)}/${type.paramOrDash(params.defence_magic)}/" +
                    type.paramOrDash(params.defence_ranged)
            )
            player.mes(
                "str=${type.paramOrDash(params.melee_strength)} rstr=${type.paramOrDash(params.ranged_strength)} " +
                    "mdmg=${type.paramOrDash(params.magic_damage)} pray=${type.paramOrDash(params.item_prayer_bonus)}"
            )
            player.mes("ops=${type.options.formatOps()} iops=${type.interfaceOptions.formatInvOps()}")
            if (specialDesc != null) {
                player.mes("special: $specialDesc")
            }
            logger.debug { "Obj debug: $type" }
        }

    private fun npcDebug(cheat: Cheat) =
        with(cheat) {
            val type = resolveNpc(args) ?: run {
                player.mes("Npc not found: ${args.joinToString(" ")}")
                return@with
            }
            player.mes("Npc: ${type.name} [${type.id}] ${safeMapping(RSCMType.NPC, type.id)}")
            player.mes(
                "size=${type.size} cmb=${type.combatLevel} cat=${label(RSCMType.CATEGORY, type.category)} " +
                    "mode=${type.defaultMode} block=${type.blockWalk}"
            )
            player.mes(
                "stats atk=${type.attack} str=${type.strength} def=${type.defence} " +
                    "range=${type.ranged} mage=${type.magic} hp=${type.hitpoints}"
            )
            player.mes(
                "anims stand=${seqLabel(type.standAnim)} walk=${seqLabel(type.walkAnim)} " +
                    "run=${seqLabel(type.runSequence)}"
            )
            player.mes(
                "combat anims atk=${seqLabel(type.paramSeqId(params.attack_anim))} " +
                    "def=${seqLabel(type.paramSeqId(params.defend_anim))} death=${seqLabel(type.paramSeqId(params.death_anim))}"
            )
            player.mes(
                "ranges attack=${type.attackRange} hunt=${type.huntRange} wander=${type.wanderRange} " +
                    "max=${type.maxRange} giveChase=${type.giveChase}"
            )
            player.mes("ops=${type.actions.formatOps()}")
            logger.debug { "Npc debug: $type" }
        }

    private fun seqDebug(cheat: Cheat) =
        with(cheat) {
            val type = resolveSeq(args) ?: run {
                player.mes("Seq not found: ${args.joinToString(" ")}")
                return@with
            }
            player.mes("Seq: [${type.id}] ${safeMapping(RSCMType.SEQ, type.id)}")
            player.mes(
                "priority=${type.priority} tickDuration=${type.tickDuration} " +
                    "totalDelay=${type.totalDelay} maxLoops=${type.maxLoops}"
            )
            logger.debug { "Seq debug: $type" }
        }

    private fun spotDebug(cheat: Cheat) =
        with(cheat) {
            val id = resolveTypedId(RSCMType.SPOTANIM, args)
            if (id == null || id < 0) {
                player.mes("Spotanim not found: ${args.joinToString(" ")}")
                return@with
            }
            player.mes("Spotanim: [$id] ${safeMapping(RSCMType.SPOTANIM, id)}")
            player.mes("Use ::spot ${safeMapping(RSCMType.SPOTANIM, id).removePrefix("spotanim.")} [height] to preview it.")
        }

    private fun specAudit(cheat: Cheat) =
        with(cheat) {
            val (mode, query) = parseSpecAuditArgs(args)
            val rows = specialWeapons.getAllSpecialWeapons().toAuditRows()
            val matchingRows = rows.filter { row -> mode.accepts(row) && row.matches(query) }
            val missing = rows.count { !it.handled }
            val handled = rows.size - missing
            val report = buildSpecAuditReport(mode, query, rows, matchingRows)
            val path = writeSpecAuditReport(report)

            player.mes(
                "Special audit: ${rows.size} cache specs, $handled handled, $missing missing."
            )
            player.mes("Showing ${matchingRows.size} ${mode.label} result(s). Full report: $path")
            for (row in matchingRows.take(SPEC_AUDIT_CHAT_LIMIT)) {
                val state = if (row.handled) "yes" else "no"
                player.mes("${row.name} [${row.id}] energy=${row.energyPercent}% handler=$state")
            }
            if (matchingRows.size > SPEC_AUDIT_CHAT_LIMIT) {
                player.mes("${matchingRows.size - SPEC_AUDIT_CHAT_LIMIT} more result(s) are in the report.")
            }
        }

    private fun weaponAudit(cheat: Cheat) =
        with(cheat) {
            val (mode, query) = parseWeaponAuditArgs(args)
            val rows = ServerCacheManager.getItems().values.toWeaponAuditRows()
            val matchingRows = rows.filter { row -> mode.accepts(row) && row.matches(query) }
            val critical = rows.count { it.severity == WeaponAuditSeverity.Critical }
            val visual = rows.count { it.severity == WeaponAuditSeverity.Visual }
            val report = buildWeaponAuditReport(mode, query, rows, matchingRows)
            val path = writeWeaponAuditReports(report, query, rows)

            player.mes(
                "Weapon audit: ${rows.size} issue(s), $critical critical, $visual visual."
            )
            player.mes("Showing ${matchingRows.size} ${mode.label} result(s). Full report: $path")
            for (row in matchingRows.take(WEAPON_AUDIT_CHAT_LIMIT)) {
                player.mes("${row.name} [${row.id}] ${row.category}: ${row.issueSummary}")
            }
            if (matchingRows.size > WEAPON_AUDIT_CHAT_LIMIT) {
                player.mes("${matchingRows.size - WEAPON_AUDIT_CHAT_LIMIT} more result(s) are in the report.")
            }
        }

    private fun resolveObj(args: List<String>): ItemServerType? {
        val id = resolveTypedId(RSCMType.OBJ, args)
        if (id != null) {
            return ServerCacheManager.getItem(id)
        }
        val query = args.normalizedQuery()
        return ServerCacheManager.getItems().values.findByNameOrInternal(query, RSCMType.OBJ)
    }

    private fun resolveNpc(args: List<String>): NpcServerType? {
        val id = resolveTypedId(RSCMType.NPC, args)
        if (id != null) {
            return ServerCacheManager.getNpc(id)
        }
        val query = args.normalizedQuery()
        return ServerCacheManager.getNpcs().values.findByNameOrInternal(query, RSCMType.NPC)
    }

    private fun resolveSeq(args: List<String>): SequenceServerType? {
        val id = resolveTypedId(RSCMType.SEQ, args) ?: return null
        return ServerCacheManager.getAnim(id)
    }

    private fun resolveTypedId(type: RSCMType, args: List<String>): Int? {
        val query = args.normalizedQuery()
        if (query.isBlank()) {
            return null
        }
        query.toIntOrNull()?.let { return it }
        val withPrefix = if (query.startsWith("${type.prefix}.")) query else "${type.prefix}.$query"
        return runCatching { RSCM.getRSCM(withPrefix) }.getOrNull()?.takeIf { it >= 0 }
    }

    private fun <T> Collection<T>.findByNameOrInternal(query: String, type: RSCMType): T?
        where T : Any =
        firstOrNull { it.debugName(type).equals(query, ignoreCase = true) }
            ?: firstOrNull { it.displayName().normalized() == query }
            ?: firstOrNull { it.debugName(type).contains(query, ignoreCase = true) }
            ?: firstOrNull { it.displayName().normalized().contains(query) }

    private fun Any.debugName(type: RSCMType): String {
        val id =
            when (this) {
                is ItemServerType -> id
                is NpcServerType -> id
                else -> return ""
            }
        return safeMapping(type, id).substringAfter('.')
    }

    private fun Any.displayName(): String =
        when (this) {
            is ItemServerType -> name
            is NpcServerType -> name
            else -> ""
        }

    private fun List<String>.normalizedQuery(): String =
        joinToString("_").removePrefix("obj.").removePrefix("npc.").removePrefix("seq.")
            .removePrefix("spotanim.").replace("-", "_").lowercase()

    private fun String.normalized(): String = replace(" ", "_").replace("-", "_").lowercase()

    private fun safeMapping(type: RSCMType, id: Int): String =
        if (id < 0) {
            id.toString()
        } else {
            runCatching { RSCM.getReverseMapping(type, id) }.getOrDefault(id.toString())
        }

    private fun label(type: RSCMType, id: Int): String =
        if (id < 0) "-" else "$id/${safeMapping(type, id)}"

    private fun seqLabel(id: Int): String =
        if (id < 0) "-" else "$id/${safeMapping(RSCMType.SEQ, id)}"

    private fun NpcServerType.paramSeqId(param: dev.openrune.TypedParamType<SequenceServerType>): Int =
        paramOrNull(param)?.id ?: -1

    private fun ItemServerType.paramOrDash(param: dev.openrune.TypedParamType<Int>): String =
        paramOrNull(param)?.toString() ?: "-"

    private fun List<StatRequirement>.formatReqs(): String =
        if (isEmpty()) {
            "none"
        } else {
            joinToString(",") { "${it.stat.displayName}:${it.level}" }
        }

    private fun dev.openrune.definition.EntityOpsDefinition.formatOps(): String =
        (1..5)
            .mapNotNull { slot ->
                getOpOrNull(slot - 1)?.takeUnless { it.isBlank() }?.let { "$slot:$it" }
            }
            .ifEmpty { listOf("none") }
            .joinToString(", ")

    private fun List<String?>.formatInvOps(): String =
        mapIndexedNotNull { index, op ->
                op?.takeUnless { it.isBlank() }?.let { "${index + 1}:$it" }
            }
            .ifEmpty { listOf("none") }
            .joinToString(", ")

    private fun parseSpecAuditArgs(args: List<String>): Pair<SpecAuditMode, String> {
        val mode = SpecAuditMode.fromArg(args.firstOrNull())
        val queryArgs = if (mode.consumesArg) args.drop(1) else args
        return mode.mode to queryArgs.normalizedQuery()
    }

    private fun List<SpecialAttackWeaponInfo>.toAuditRows(): List<SpecAuditRow> {
        val handledIds = specialRegistry.registeredObjTypes()
        return mapNotNull { info ->
                val obj = ServerCacheManager.getItem(info.objType) ?: return@mapNotNull null
                SpecAuditRow(
                    id = info.objType,
                    internal = safeMapping(RSCMType.OBJ, info.objType),
                    name = obj.name.ifBlank { safeMapping(RSCMType.OBJ, info.objType) },
                    energyPercent = info.energy / 10,
                    handled = info.objType in handledIds,
                    description = info.description,
                )
            }
            .sortedWith(compareBy<SpecAuditRow> { it.handled }.thenBy { it.name }.thenBy { it.id })
    }

    private fun buildSpecAuditReport(
        mode: SpecAuditMode,
        query: String,
        allRows: List<SpecAuditRow>,
        rows: List<SpecAuditRow>,
    ): String {
        val missing = allRows.count { !it.handled }
        val handled = allRows.size - missing
        return buildString {
            appendLine("OpenRune Special Attack Audit")
            appendLine("Total cache special weapons: ${allRows.size}")
            appendLine("Handled: $handled")
            appendLine("Missing: $missing")
            appendLine("Filter: ${mode.label}")
            appendLine("Search: ${query.ifBlank { "none" }}")
            appendLine()
            appendLine("handler | energy | id | internal | name | description")
            appendLine("--- | --- | --- | --- | --- | ---")
            for (row in rows) {
                appendLine(
                    "${if (row.handled) "yes" else "no"} | " +
                        "${row.energyPercent}% | " +
                        "${row.id} | " +
                        "${row.internal} | " +
                        "${row.name} | " +
                        row.description.orEmpty().replace("|", "/")
                )
            }
        }
    }

    private fun writeSpecAuditReport(report: String): Path {
        val path = Path.of("logs", "special-attack-audit.md")
        Files.createDirectories(path.parent)
        Files.writeString(path, report)
        return path
    }

    private fun Collection<ItemServerType>.toWeaponAuditRows(): List<WeaponAuditRow> =
        asSequence()
            .filter { it.name.isNotBlank() && !it.name.equals("null", ignoreCase = true) }
            .filter { WeaponCategory.getOrUnarmed(it.weaponCategory?.id) != WeaponCategory.Unarmed }
            .flatMap { it.auditWeapon().asSequence() }
            .sortedWith(
                compareBy<WeaponAuditRow> { it.severity.rank }
                    .thenBy { it.category }
                    .thenBy { it.name }
                    .thenBy { it.id }
            )
            .toList()

    private fun ItemServerType.auditWeapon(): List<WeaponAuditRow> {
        val category = WeaponCategory.getOrUnarmed(weaponCategory?.id)
        val issues = mutableListOf<WeaponAuditIssue>()
        val specializedRanged = weaponsRegistry.getRanged(InvObj(this)) != null

        fun checkSeq(paramName: String, id: Int?) {
            if (id == null || id < 0) {
                return
            }
            if (!isMapped(RSCMType.SEQ, id)) {
                issues += WeaponAuditIssue.Visual("$paramName references missing seq $id")
            }
        }

        checkSeq("attack_anim_stance1", paramOrNull(params.attack_anim_stance1)?.id)
        checkSeq("attack_anim_stance2", paramOrNull(params.attack_anim_stance2)?.id)
        checkSeq("attack_anim_stance3", paramOrNull(params.attack_anim_stance3)?.id)
        checkSeq("attack_anim_stance4", paramOrNull(params.attack_anim_stance4)?.id)
        checkSeq("defend_anim", paramOrNull(params.defend_anim)?.id)
        checkSeq("bas_readyanim", paramOrNull(params.bas_readyanim)?.id)

        if (category.isRangedWeapon()) {
            if (
                paramOrNull(params.attack_anim_stance1) == null &&
                    !specializedRanged &&
                    !hasDefaultRangedAttackAnim(category)
            ) {
                issues += WeaponAuditIssue.Critical(
                    "missing attack_anim_stance1; generic ranged combat refuses to fire"
                )
            } else if (paramOrNull(params.attack_anim_stance1) == null && !specializedRanged) {
                issues += WeaponAuditIssue.Visual(
                    "missing attack_anim_stance1; ranged combat uses category fallback"
                )
            }
            val projType = paramOrNull(params.proj_type)?.id
            if (projType == null && !specializedRanged) {
                if (category == WeaponCategory.Gun) {
                    issues += WeaponAuditIssue.Visual(
                        "missing proj_type; unsupported gun cannot fire as a normal ranged weapon"
                    )
                } else {
                    issues += WeaponAuditIssue.Critical(
                        "missing proj_type; generic ranged combat cannot build a projectile"
                    )
                }
            } else if (projType != null && !isMapped(RSCMType.PROJANIM, projType)) {
                issues += WeaponAuditIssue.Critical("proj_type references missing projanim $projType")
            }
            if (category == WeaponCategory.Thrown || category == WeaponCategory.Chinchompas) {
                val travel = paramOrNull(params.proj_travel)?.id
                if (travel == null && !specializedRanged) {
                    issues += WeaponAuditIssue.Critical(
                        "missing proj_travel; thrown weapon has no travel graphic"
                    )
                } else if (travel != null && !isNullSpotanim(travel) && !isMapped(RSCMType.SPOTANIM, travel)) {
                    issues += WeaponAuditIssue.Critical("proj_travel references missing spotanim $travel")
                }
            }
            val launch = paramOrNull(params.proj_launch)?.id
            if (launch != null && !isNullSpotanim(launch) && !isMapped(RSCMType.SPOTANIM, launch)) {
                issues += WeaponAuditIssue.Visual("proj_launch references missing spotanim $launch")
            }
        } else if (paramOrNull(params.attack_anim_stance1) == null) {
            issues += WeaponAuditIssue.Visual("missing attack_anim_stance1; melee falls back to default")
        }

        if (issues.isEmpty()) {
            return emptyList()
        }
        val severity =
            if (issues.any { it.severity == WeaponAuditSeverity.Critical }) {
                WeaponAuditSeverity.Critical
            } else {
                WeaponAuditSeverity.Visual
            }
        return listOf(
            WeaponAuditRow(
                id = id,
                internal = safeMapping(RSCMType.OBJ, id),
                name = name,
                category = category.text,
                severity = severity,
                issueSummary = issues.joinToString("; ") { it.message },
            )
        )
    }

    private fun buildWeaponAuditReport(
        mode: WeaponAuditMode,
        query: String,
        allRows: List<WeaponAuditRow>,
        rows: List<WeaponAuditRow>,
    ): String {
        val critical = allRows.count { it.severity == WeaponAuditSeverity.Critical }
        val visual = allRows.count { it.severity == WeaponAuditSeverity.Visual }
        return buildString {
            appendLine("OpenRune Weapon Audit")
            appendLine("Total issues: ${allRows.size}")
            appendLine("Critical: $critical")
            appendLine("Visual: $visual")
            appendLine("Filter: ${mode.label}")
            appendLine("Search: ${query.ifBlank { "none" }}")
            appendLine()
            appendLine("severity | category | id | internal | name | issues")
            appendLine("--- | --- | --- | --- | --- | ---")
            for (row in rows) {
                appendLine(
                    "${row.severity.label} | " +
                        "${row.category} | " +
                        "${row.id} | " +
                        "${row.internal} | " +
                        "${row.name} | " +
                        row.issueSummary.replace("|", "/")
                )
            }
        }
    }

    private fun writeWeaponAuditReports(report: String, query: String, rows: List<WeaponAuditRow>): Path {
        val path = Path.of("logs", "weapon-audit.md")
        Files.createDirectories(path.parent)
        Files.writeString(path, report)
        for (mode in WeaponAuditMode.entries) {
            val modeRows = rows.filter { row -> mode.accepts(row) && row.matches(query) }
            val modeReport = buildWeaponAuditReport(mode, query, rows, modeRows)
            Files.writeString(Path.of("logs", "weapon-audit-${mode.label}.md"), modeReport)
        }
        return path
    }

    private fun SpecAuditRow.matches(query: String): Boolean =
        query.isBlank() ||
            name.normalized().contains(query) ||
            internal.normalized().contains(query) ||
            id.toString() == query ||
            description.orEmpty().normalized().contains(query)

    private fun WeaponAuditRow.matches(query: String): Boolean =
        query.isBlank() ||
            name.normalized().contains(query) ||
            internal.normalized().contains(query) ||
            category.normalized().contains(query) ||
            issueSummary.normalized().contains(query) ||
            id.toString() == query

    private fun parseWeaponAuditArgs(args: List<String>): Pair<WeaponAuditMode, String> {
        val mode = WeaponAuditMode.fromArg(args.firstOrNull())
        val queryArgs = if (mode.consumesArg) args.drop(1) else args
        return mode.mode to queryArgs.normalizedQuery()
    }

    private fun WeaponCategory.isRangedWeapon(): Boolean =
        this == WeaponCategory.Bow ||
            this == WeaponCategory.Crossbow ||
            this == WeaponCategory.Thrown ||
            this == WeaponCategory.Chinchompas ||
            this == WeaponCategory.Salamander ||
            this == WeaponCategory.Gun

    private fun hasDefaultRangedAttackAnim(category: WeaponCategory): Boolean =
        category == WeaponCategory.Bow ||
            category == WeaponCategory.Crossbow ||
            category == WeaponCategory.Thrown ||
            category == WeaponCategory.Chinchompas ||
            category == WeaponCategory.Salamander

    private fun isNullSpotanim(id: Int): Boolean = id == 0xFFFF || id < 0

    private fun isMapped(type: RSCMType, id: Int): Boolean =
        id >= 0 && runCatching { RSCM.getReverseMapping(type, id) }.isSuccess

    private data class SpecAuditRow(
        val id: Int,
        val internal: String,
        val name: String,
        val energyPercent: Int,
        val handled: Boolean,
        val description: String?,
    )

    private data class WeaponAuditRow(
        val id: Int,
        val internal: String,
        val name: String,
        val category: String,
        val severity: WeaponAuditSeverity,
        val issueSummary: String,
    )

    private sealed class WeaponAuditIssue(
        val severity: WeaponAuditSeverity,
        val message: String,
    ) {
        class Critical(message: String) : WeaponAuditIssue(WeaponAuditSeverity.Critical, message)

        class Visual(message: String) : WeaponAuditIssue(WeaponAuditSeverity.Visual, message)
    }

    private enum class WeaponAuditSeverity(val label: String, val rank: Int) {
        Critical("critical", 0),
        Visual("visual", 1),
    }

    private enum class SpecAuditMode(val label: String) {
        Missing("missing"),
        Handled("handled"),
        All("all");

        fun accepts(row: SpecAuditRow): Boolean =
            when (this) {
                Missing -> !row.handled
                Handled -> row.handled
                All -> true
            }

        companion object {
            fun fromArg(arg: String?): ParsedSpecAuditMode {
                val mode =
                    when (arg?.lowercase()) {
                        "missing",
                        "miss",
                        "no" -> Missing
                        "handled",
                        "done",
                        "yes" -> Handled
                        "all" -> All
                        else -> Missing
                    }
                return ParsedSpecAuditMode(mode, consumesArg = modeArgWasConsumed(arg))
            }

            private fun modeArgWasConsumed(arg: String?): Boolean =
                arg?.lowercase() in setOf("missing", "miss", "no", "handled", "done", "yes", "all")
        }
    }

    private data class ParsedSpecAuditMode(val mode: SpecAuditMode, val consumesArg: Boolean)

    private enum class WeaponAuditMode(val label: String) {
        Critical("critical"),
        Visual("visual"),
        All("all");

        fun accepts(row: WeaponAuditRow): Boolean =
            when (this) {
                Critical -> row.severity == WeaponAuditSeverity.Critical
                Visual -> row.severity == WeaponAuditSeverity.Visual
                All -> true
            }

        companion object {
            fun fromArg(arg: String?): ParsedWeaponAuditMode {
                val mode =
                    when (arg?.lowercase()) {
                        "critical",
                        "fatal",
                        "broken" -> Critical
                        "visual",
                        "cosmetic" -> Visual
                        "all" -> All
                        else -> Critical
                    }
                return ParsedWeaponAuditMode(mode, consumesArg = modeArgWasConsumed(arg))
            }

            private fun modeArgWasConsumed(arg: String?): Boolean =
                arg?.lowercase() in setOf("critical", "fatal", "broken", "visual", "cosmetic", "all")
        }
    }

    private data class ParsedWeaponAuditMode(val mode: WeaponAuditMode, val consumesArg: Boolean)

    private companion object {
        private const val SPEC_AUDIT_CHAT_LIMIT = 8
        private const val WEAPON_AUDIT_CHAT_LIMIT = 8
    }
}
