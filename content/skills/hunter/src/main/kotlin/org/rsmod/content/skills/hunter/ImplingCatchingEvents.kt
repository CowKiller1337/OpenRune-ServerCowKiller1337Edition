package org.rsmod.content.skills.hunter

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcServerType
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hunterLvl
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.stats.levelmod.InvisibleLevels
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ImplingCatchingEvents
@Inject
constructor(
    private val invisibleLevels: InvisibleLevels,
    private val xpMods: XpModifiers,
) : PluginScript() {
    override fun ScriptContext.startup() {
        HunterDefinitions.butterflyCatchRecipes.forEach { recipe ->
            onOpNpc1(recipe.npc) { catchButterfly(it.npc, recipe) }
        }
        HunterDefinitions.implingCatchRecipes.forEach { recipe ->
            onOpNpc1(recipe.npc) { catchImpling(it.npc, recipe) }
        }
        HunterDefinitions.directCatchRecipes.forEach { recipe ->
            onOpNpc1(recipe.npc) { catchDirect(it.npc, recipe) }
        }
    }

    private suspend fun ProtectedAccess.catchButterfly(npc: Npc, recipe: ButterflyCatchRecipe) {
        if (!canCatchButterfly(recipe)) {
            resetAnim()
            return
        }

        faceEntitySquare(npc)
        npcPlayerFaceClose(npc)
        anim(CATCH_ANIM)
        delay(2)

        if (!rollNetCatch(recipe.lowChance, recipe.highChance)) {
            mes("You fail to catch the butterfly.")
            resetAnim()
            return
        }

        if (invDel(inv, HunterDefinitions.BUTTERFLY_JAR, 1).failure) {
            resetAnim()
            return
        }
        if (invAdd(inv, recipe.capturedJar, 1).failure) {
            invAdd(inv, HunterDefinitions.BUTTERFLY_JAR, 1)
            mes("You don't have enough inventory space.")
            resetAnim()
            return
        }

        statAdvance("stat.hunter", recipe.xp * xpMods.get(player, "stat.hunter"))
        mes("You manage to catch the butterfly.")
        npcChangeType(npc, inactiveNpc(), recipe.respawnTicks)
        resetAnim()
    }

    private suspend fun ProtectedAccess.catchDirect(npc: Npc, recipe: DirectHunterCatchRecipe) {
        if (!canCatchDirect(recipe)) {
            resetAnim()
            return
        }

        faceEntitySquare(npc)
        npcPlayerFaceClose(npc)
        anim(CATCH_ANIM)
        delay(2)

        if (!statRandom("stat.hunter", recipe.lowChance, recipe.highChance, invisibleLevels)) {
            mes("You fail to catch the ${recipe.displayName}.")
            resetAnim()
            return
        }

        if (invAdd(inv, recipe.reward, recipe.rewardCount).failure) {
            mes("You don't have enough inventory space.")
            resetAnim()
            return
        }

        statAdvance("stat.hunter", recipe.xp * xpMods.get(player, "stat.hunter"))
        mes("You manage to catch the ${recipe.displayName}.")
        npcChangeType(npc, inactiveNpc(), recipe.respawnTicks)
        resetAnim()
    }

    private suspend fun ProtectedAccess.catchImpling(npc: Npc, recipe: ImplingCatchRecipe) {
        if (!canCatchImpling(recipe)) {
            resetAnim()
            return
        }

        faceEntitySquare(npc)
        npcPlayerFaceClose(npc)
        anim(CATCH_ANIM)
        delay(2)

        if (!rollNetCatch(recipe.lowChance, recipe.highChance)) {
            mes("You fail to catch the impling.")
            resetAnim()
            return
        }

        if (invDel(inv, HunterDefinitions.IMPLING_JAR, 1).failure) {
            resetAnim()
            return
        }
        if (invAdd(inv, recipe.capturedJar, 1).failure) {
            invAdd(inv, HunterDefinitions.IMPLING_JAR, 1)
            mes("You don't have enough inventory space.")
            resetAnim()
            return
        }

        statAdvance("stat.hunter", recipe.xp * xpMods.get(player, "stat.hunter"))
        mes("You manage to catch the impling.")
        npcChangeType(npc, inactiveNpc(), recipe.respawnTicks)
        resetAnim()
    }

    private suspend fun ProtectedAccess.canCatchButterfly(recipe: ButterflyCatchRecipe): Boolean {
        if (player.hunterLvl < recipe.level) {
            mes("You need a Hunter level of ${recipe.level} to catch this butterfly.")
            return false
        }
        if (!hasButterflyNet()) {
            mes("You need a butterfly net to catch butterflies.")
            return false
        }
        if (HunterDefinitions.BUTTERFLY_JAR !in inv) {
            mes("You need an empty butterfly jar to catch this.")
            return false
        }
        return true
    }

    private suspend fun ProtectedAccess.canCatchImpling(recipe: ImplingCatchRecipe): Boolean {
        if (player.hunterLvl < recipe.level) {
            mes("You need a Hunter level of ${recipe.level} to catch this impling.")
            return false
        }
        if (!hasButterflyNet()) {
            mes("You need a butterfly net to catch implings.")
            return false
        }
        if (HunterDefinitions.IMPLING_JAR !in inv) {
            mes("You need an empty impling jar to catch this.")
            return false
        }
        return true
    }

    private suspend fun ProtectedAccess.canCatchDirect(recipe: DirectHunterCatchRecipe): Boolean {
        if (player.hunterLvl < recipe.level) {
            mes("You need a Hunter level of ${recipe.level} to catch this.")
            return false
        }
        val missingTool = recipe.requiredTools.firstOrNull { !hasTool(it) }
        if (missingTool != null) {
            mes("You need ${toolName(missingTool)} to catch this.")
            return false
        }
        return true
    }

    private fun ProtectedAccess.hasButterflyNet(): Boolean {
        return HunterDefinitions.BUTTERFLY_NET in inv ||
            HunterDefinitions.BUTTERFLY_NET in player.worn ||
            HunterDefinitions.MAGIC_BUTTERFLY_NET in inv ||
            HunterDefinitions.MAGIC_BUTTERFLY_NET in player.worn
    }

    private fun ProtectedAccess.rollNetCatch(lowChance: Int, highChance: Int): Boolean {
        val boost = if (hasMagicButterflyNet()) MAGIC_NET_CHANCE_BOOST else 0
        return statRandom("stat.hunter", lowChance + boost, highChance + boost, invisibleLevels)
    }

    private fun ProtectedAccess.hasMagicButterflyNet(): Boolean {
        return HunterDefinitions.MAGIC_BUTTERFLY_NET in inv ||
            HunterDefinitions.MAGIC_BUTTERFLY_NET in player.worn
    }

    private fun ProtectedAccess.hasTool(tool: String): Boolean =
        tool in inv || tool in player.worn

    private fun toolName(tool: String): String =
        when (tool) {
            HunterDefinitions.HUNTING_NET -> "a hunting net"
            HunterDefinitions.ROPE -> "a rope"
            HunterDefinitions.TEASING_STICK -> "a teasing stick"
            else -> "the correct tool"
        }

    private fun inactiveNpc(): NpcServerType =
        ServerCacheManager.getNpc(HunterDefinitions.INACTIVE_NPC.asRSCM(RSCMType.NPC))
            ?: error("Missing npc type: ${HunterDefinitions.INACTIVE_NPC}")

    private companion object {
        private const val CATCH_ANIM = "seq.human_shearing"
        private const val MAGIC_NET_CHANCE_BOOST = 20
    }
}
