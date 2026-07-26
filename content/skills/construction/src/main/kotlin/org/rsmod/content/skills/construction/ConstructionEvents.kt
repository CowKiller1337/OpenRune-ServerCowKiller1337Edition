package org.rsmod.content.skills.construction

import dev.openrune.ServerCacheManager
import dev.openrune.types.ObjectServerType
import jakarta.inject.Inject
import org.rsmod.api.config.Constants
import org.rsmod.api.instances.InstanceAccess
import org.rsmod.api.instances.InstanceArea
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceSpec
import org.rsmod.api.instances.RegionLocal
import org.rsmod.api.instances.currentInstanceId
import org.rsmod.api.instances.events.InstanceEndedEvent
import org.rsmod.api.instances.events.InstancePlayerLeaveEvent
import org.rsmod.api.instances.events.instanceEventId
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.constructionLvl
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLoc4
import org.rsmod.api.script.onOpLoc5
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.skills.Material
import org.rsmod.content.skills.SkillMultiConfig
import org.rsmod.content.skills.SkillMultiEntry
import org.rsmod.content.skills.SkillingActionType
import org.rsmod.content.skills.openSkillMulti
import org.rsmod.game.MapClock
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.loc.LocLayerConstants

class ConstructionEvents
@Inject
constructor(
    private val locRepo: LocRepository,
    private val xpMods: XpModifiers,
    private val instanceManager: InstanceManager,
    private val worldClock: MapClock,
) : PluginScript() {
    private val sandboxLocs = HashMap<Long, MutableList<LocInfo>>()

    override fun ScriptContext.startup() {
        spawnYamaHousePortal()

        ConstructionDefinitions.saws.forEach { saw ->
            ConstructionDefinitions.recipesByPlank.keys.forEach { plank ->
                onOpHeldU(saw, plank) { promptFlatpacks(plank) }
            }
        }

        ConstructionDefinitions.hotspotLocIds.mapNotNull(::locTypeOrNull).distinctBy { it.id }.forEach { type ->
            onOpLoc5(type) { buildHotspot(it.loc) }
        }

        ConstructionDefinitions.flatpackRecipes.forEach { recipe ->
            val hotspotKind = recipe.hotspotKind ?: return@forEach
            val flatpack = recipe.flatpack ?: return@forEach
            hotspotKind.locIds.mapNotNull(::locTypeOrNull).distinctBy { it.id }.forEach { type ->
                onOpLocU(type, flatpack) { placeFlatpack(it.loc, recipe) }
            }
        }

        ConstructionDefinitions.builtLocToHotspotLocId.keys
            .mapNotNull(::locTypeOrNull)
            .distinctBy { it.id }
            .forEach { type ->
                onOpLoc5(type) { removeBuiltFurniture(it.loc, it.type.id) }
            }

        ConstructionDefinitions.portalLocIds.mapNotNull(::locTypeOrNull).distinctBy { it.id }.forEach { type ->
            onOpLoc1(type) { openHousePortal(buildMode = false) }
            onOpLoc2(type) { openHousePortal(buildMode = false) }
            onOpLoc3(type) { openHousePortal(buildMode = true) }
            onOpLoc4(type) { mes("Friend-owned houses are not available yet.") }
        }

        onPlayerQueueWithArgs<FlatpackTask>("queue.construction_flatpack") {
            processFlatpack(it.args)
        }

        onEvent<InstancePlayerLeaveEvent>(instanceEventId(CONSTRUCTION_INSTANCE_KEY)) {
            player.attr.remove(CONSTRUCTION_HOTSPOT_ORIGINAL_ATTR)
            player.attr.remove(CONSTRUCTION_BUILD_MODE_ATTR)
        }

        onEvent<InstanceEndedEvent>(instanceEventId(CONSTRUCTION_INSTANCE_KEY)) {
            sandboxLocs.remove(instanceId.value)?.forEach { loc -> locRepo.del(loc, Int.MAX_VALUE) }
        }
    }

    private suspend fun ProtectedAccess.promptFlatpacks(plank: String) {
        if (!hasSaw() || !hasHammer() || !inv.contains(plank)) {
            mes(Constants.dm_default)
            return
        }

        val recipes = ConstructionDefinitions.recipesByPlank[plank].orEmpty()
        val available = recipes.filter { constructionBuildLevel() >= it.level }
        if (available.isEmpty()) {
            val level = recipes.minOfOrNull { it.level } ?: return
            mesbox("You need a Construction level of $level to make anything with this.")
            return
        }

        openSkillMulti(
            SkillMultiConfig(
                actionType = SkillingActionType.MAKE,
                verb = "make",
                entries = available.map { recipe -> recipe.skillMultiEntry() },
                maxCountProvider = provider@{ inventory, entry ->
                    val recipe = ConstructionDefinitions.recipesByFlatpack[entry.internal] ?: return@provider 0
                    val plankCount = inventory.count(recipe.plank) / recipe.plankCount
                    val nailCount =
                        if (recipe.nailCount == 0) {
                            Int.MAX_VALUE
                        } else {
                            ConstructionDefinitions.nails.sumOf(inventory::count) / recipe.nailCount
                        }
                    val extraCounts =
                        recipe.extraMaterials.map { material -> inventory.count(material.obj) / material.count }
                    minOf(plankCount, nailCount, extraCounts.minOrNull() ?: Int.MAX_VALUE)
                },
            ),
        ) { selection ->
            val recipe = ConstructionDefinitions.recipesByFlatpack[selection.entry.internal] ?: return@openSkillMulti
            weakQueue("queue.construction_flatpack", 1, FlatpackTask(recipe, selection.amount, completed = 0))
        }
    }

    private suspend fun ProtectedAccess.processFlatpack(task: FlatpackTask) {
        val recipe = task.recipe
        if (!canBuild(recipe, showMessages = true)) {
            resetAnim()
            return
        }
        if (inv.isFull() && recipe.plankCount + recipe.nailCount <= 1) {
            mes("You don't have enough inventory space.")
            resetAnim()
            return
        }

        anim(ConstructionDefinitions.BUILD_ANIM)
        delay(ConstructionDefinitions.BUILD_DELAY)

        if (!consumeMaterials(recipe)) {
            resetAnim()
            return
        }
        val flatpack = recipe.flatpack ?: return
        if (invAdd(inv, flatpack, 1).failure) {
            refundMaterials(recipe)
            mes("You don't have enough inventory space.")
            resetAnim()
            return
        }

        advanceConstructionXp(recipe.xp, xpMods.get(player, "stat.construction"))
        mes("You make a ${recipe.displayName}.")

        val completed = task.completed + 1
        if (completed < task.amount && canBuild(recipe, showMessages = false)) {
            weakQueue("queue.construction_flatpack", 1, task.copy(completed = completed))
        } else {
            resetAnim()
        }
    }

    private suspend fun ProtectedAccess.buildHotspot(loc: BoundLocInfo) {
        val kind = ConstructionDefinitions.hotspotKindByLocId[loc.id]
        if (kind == null) {
            mes(Constants.dm_default)
            return
        }

        val recipes = ConstructionDefinitions.recipesByHotspot[kind].orEmpty()
        val recipe = recipes
            .filter { constructionBuildLevel() >= it.level }
            .filter { hasMaterials(it) }
            .maxByOrNull { it.level }

        if (recipe == null) {
            val next = recipes.firstOrNull { player.constructionLvl < it.level }
            if (next != null) {
                mes("You need a Construction level of ${next.level} to build a ${next.displayName}.")
            } else {
                mes("You don't have the materials to build anything here.")
            }
            return
        }

        if (!hasSaw()) {
            mes("You need a saw to build furniture.")
            return
        }
        if (!hasHammer()) {
            mes("You need a hammer to build furniture.")
            return
        }

        anim(ConstructionDefinitions.BUILD_ANIM)
        delay(ConstructionDefinitions.BUILD_DELAY)

        if (!consumeMaterials(recipe)) {
            return
        }

        val builtLocId = recipe.builtLocId ?: return
        player.attr.getOrPut(CONSTRUCTION_HOTSPOT_ORIGINAL_ATTR) { mutableMapOf() }[hotspotKey(loc.coords)] =
            loc.id
        locRepo.change(loc, locType(builtLocId), Int.MAX_VALUE)
        trackSandboxLoc(player.currentInstanceId()?.value, locInfo(loc.coords, builtLocId, loc.angle, loc.shape))
        advanceConstructionXp(recipe.xp, xpMods.get(player, "stat.construction"))
        mes("You build a ${recipe.displayName}.")
    }

    private suspend fun ProtectedAccess.placeFlatpack(loc: BoundLocInfo, recipe: ConstructionRecipe) {
        val hotspotKind = recipe.hotspotKind
        val builtLocId = recipe.builtLocId
        if (hotspotKind == null || builtLocId == null) {
            mes("This flatpack needs the right furniture space.")
            return
        }
        if (!meetsConstructionBuildLevel(recipe.level, "place this")) {
            return
        }
        if (ConstructionDefinitions.hotspotKindByLocId[loc.id] != hotspotKind) {
            mes(Constants.dm_default)
            return
        }
        val flatpack = recipe.flatpack ?: return
        if (invDel(inv, flatpack, 1).failure) {
            return
        }

        anim(ConstructionDefinitions.BUILD_ANIM)
        delay(2)

        player.attr.getOrPut(CONSTRUCTION_HOTSPOT_ORIGINAL_ATTR) { mutableMapOf() }[hotspotKey(loc.coords)] =
            loc.id
        locRepo.change(loc, locType(builtLocId), Int.MAX_VALUE)
        trackSandboxLoc(player.currentInstanceId()?.value, locInfo(loc.coords, builtLocId, loc.angle, loc.shape))
        mes("You place the ${recipe.displayName}.")
    }

    private suspend fun ProtectedAccess.removeBuiltFurniture(loc: BoundLocInfo, builtLocId: Int) {
        val fallback = ConstructionDefinitions.builtLocToHotspotLocId[builtLocId]
        if (fallback == null) {
            mes(Constants.dm_default)
            return
        }

        anim(ConstructionDefinitions.BUILD_ANIM)
        delay(2)

        val key = hotspotKey(loc.coords)
        val original = player.attr[CONSTRUCTION_HOTSPOT_ORIGINAL_ATTR]?.remove(key) ?: fallback
        locRepo.change(loc, locType(original), Int.MAX_VALUE)
        trackSandboxLoc(player.currentInstanceId()?.value, locInfo(loc.coords, original, loc.angle, loc.shape))
        mes("You remove the furniture.")
    }

    private suspend fun ProtectedAccess.openHousePortal(buildMode: Boolean) {
        val session = instanceManager.sessionForPlayer(player)
        if (session?.key == CONSTRUCTION_INSTANCE_KEY) {
            val exit = instanceManager.leave(player, session, worldClock.cycle)
            telejump(exit)
            mes("You leave your house.")
            return
        }
        if (session != null) {
            mes("You are already inside an instance.")
            return
        }

        player.attr[CONSTRUCTION_BUILD_MODE_ATTR] = buildMode
        player.attr.remove(CONSTRUCTION_HOTSPOT_ORIGINAL_ATTR)

        val result =
            instanceManager.create(
                owner = player,
                key = CONSTRUCTION_INSTANCE_KEY,
                spec = constructionInstanceSpec(),
                access = InstanceAccess.Private,
                currentTick = worldClock.cycle,
            )
        when (result) {
            is InstanceManager.Result.Failed -> mes(result.reason)
            is InstanceManager.Result.Created -> {
                populateConstructionSandbox(result.session.id.value, result.enter)
                telejump(result.enter)
                instanceManager.finalizeEntry(player, result.session, worldClock.cycle)
                if (buildMode) {
                    mes("Build mode is now enabled.")
                } else {
                    mes("You enter your house.")
                }
            }
            is InstanceManager.Result.Joined -> {
                populateConstructionSandbox(result.session.id.value, result.enter)
                telejump(result.enter)
                instanceManager.finalizeEntry(player, result.session, worldClock.cycle)
                mes("You enter your house.")
            }
        }
    }

    private fun spawnYamaHousePortal() {
        val portal = locTypeOrNull(YAMA_HOUSE_PORTAL_LOC) ?: return
        if (locRepo.findExact(YAMA_HOUSE_PORTAL_COORD, portal) != null) {
            return
        }
        locRepo.add(
            YAMA_HOUSE_PORTAL_COORD,
            portal,
            Int.MAX_VALUE,
            LocAngle.West,
            LocShape.CentrepieceStraight,
        )
    }

    private fun constructionInstanceSpec(): InstanceSpec =
        InstanceSpec(
            fee = 0,
            maxPlayers = 1,
            reclaimTicks = 0,
            graceTicks = 0,
            destroyWhenEmpty = true,
            area =
                InstanceArea.copyRegions(
                    regionIds = listOf(YAMA_LAIR_REGION_ID),
                    enterCoord = YAMA_LAIR_INSTANCE_ENTRY,
                    exitCoord = YAMA_HOUSE_PORTAL_EXIT,
                ),
            settingsRowId = 0,
            bossName = "House",
            description = "Player-owned house construction sandbox.",
        )

    private fun populateConstructionSandbox(instanceId: Long, enter: CoordGrid) {
        if (sandboxLocs.containsKey(instanceId)) {
            return
        }
        val locs =
            listOfNotNull(
                spawnSandboxLoc(enter.translate(0, -2), YAMA_HOUSE_PORTAL_LOC, LocAngle.South),
                spawnSandboxLoc(enter.translate(2, 0), 4515, LocAngle.West),
                spawnSandboxLoc(enter.translate(3, 2), 4521, LocAngle.West),
                spawnSandboxLoc(enter.translate(0, 3), 15298, LocAngle.West),
                spawnSandboxLoc(enter.translate(-2, 1), 15403, LocAngle.West),
                spawnSandboxLoc(enter.translate(2, -2), 15260, LocAngle.West),
                spawnSandboxLoc(enter.translate(4, -1), 15261, LocAngle.West),
                spawnSandboxLoc(enter.translate(4, 1), 15262, LocAngle.West),
                spawnSandboxLoc(enter.translate(-3, 0), 15270, LocAngle.West),
                spawnSandboxLoc(enter.translate(-3, 2), 15406, LocAngle.West),
            )
        sandboxLocs[instanceId] = locs.toMutableList()
    }

    private fun spawnSandboxLoc(coords: CoordGrid, locId: Int, angle: LocAngle): LocInfo? {
        val loc = locInfo(coords, locId, angle, LocShape.CentrepieceStraight)
        return if (locRepo.add(loc, Int.MAX_VALUE)) loc else null
    }

    private fun trackSandboxLoc(instanceId: Long?, loc: LocInfo) {
        if (instanceId == null) {
            return
        }
        sandboxLocs.getOrPut(instanceId) { mutableListOf() } += loc
    }

    private suspend fun ProtectedAccess.canBuild(
        recipe: ConstructionRecipe,
        showMessages: Boolean,
    ): Boolean {
        if (!meetsConstructionBuildLevel(recipe.level, "make this")) {
            return false
        }
        if (!hasSaw()) {
            if (showMessages) {
                mes("You need a saw to build furniture.")
            }
            return false
        }
        if (!hasHammer()) {
            if (showMessages) {
                mes("You need a hammer to build furniture.")
            }
            return false
        }
        if (!hasMaterials(recipe)) {
            if (showMessages) {
                mes("You don't have the materials to make this.")
            }
            return false
        }
        return true
    }

    private suspend fun ProtectedAccess.meetsConstructionBuildLevel(
        level: Int,
        action: String,
    ): Boolean {
        if (constructionBuildLevel() >= level) {
            return true
        }
        mesbox("You need a Construction level of $level to $action.")
        return false
    }

    private fun ProtectedAccess.constructionBuildLevel(): Int {
        val crystalBoost = if (hasCrystalSaw()) CRYSTAL_SAW_BOOST else 0
        return player.constructionLvl + crystalBoost
    }

    private fun ProtectedAccess.hasSaw(): Boolean =
        hasConstructionTool(ConstructionDefinitions.saws)

    private fun ProtectedAccess.hasHammer(): Boolean =
        hasConstructionTool(ConstructionDefinitions.hammers)

    private fun ProtectedAccess.hasCrystalSaw(): Boolean =
        hasConstructionTool(setOf(ConstructionDefinitions.CRYSTAL_SAW))

    private fun ProtectedAccess.hasConstructionTool(types: Set<String>): Boolean =
        types.any { tool -> inv.contains(tool) || tool in player.worn }

    private fun ProtectedAccess.hasMaterials(recipe: ConstructionRecipe): Boolean =
        inv.count(recipe.plank) >= recipe.plankCount &&
            availableNails() >= recipe.nailCount &&
            recipe.extraMaterials.all { inv.count(it.obj) >= it.count }

    private fun ProtectedAccess.availableNails(): Int =
        ConstructionDefinitions.nails.sumOf(inv::count)

    private fun ProtectedAccess.consumeMaterials(recipe: ConstructionRecipe): Boolean {
        if (invDel(inv, recipe.plank, recipe.plankCount).failure) {
            return false
        }
        if (recipe.nailCount > 0 && !deleteNails(recipe.nailCount)) {
            invAdd(inv, recipe.plank, recipe.plankCount)
            return false
        }
        val consumedExtras = mutableListOf<ConstructionMaterial>()
        for (material in recipe.extraMaterials) {
            if (invDel(inv, material.obj, material.count).failure) {
                invAdd(inv, recipe.plank, recipe.plankCount)
                if (recipe.nailCount > 0) {
                    invAdd(inv, ConstructionDefinitions.nails.first(), recipe.nailCount)
                }
                consumedExtras.forEach { invAdd(inv, it.obj, it.count) }
                return false
            }
            consumedExtras += material
        }
        return true
    }

    private fun ProtectedAccess.refundMaterials(recipe: ConstructionRecipe) {
        invAdd(inv, recipe.plank, recipe.plankCount)
        if (recipe.nailCount > 0) {
            invAdd(inv, ConstructionDefinitions.nails.first(), recipe.nailCount)
        }
        recipe.extraMaterials.forEach { invAdd(inv, it.obj, it.count) }
    }

    private fun ProtectedAccess.deleteNails(amount: Int): Boolean {
        var remaining = amount
        for (nail in ConstructionDefinitions.nails) {
            if (remaining <= 0) {
                return true
            }
            val take = minOf(remaining, inv.count(nail))
            if (take <= 0) {
                continue
            }
            if (invDel(inv, nail, take).failure) {
                return false
            }
            remaining -= take
        }
        return remaining <= 0
    }

    private fun ConstructionRecipe.skillMultiEntry(): SkillMultiEntry =
        SkillMultiEntry(
            internal = flatpack ?: error("Missing flatpack output."),
            materials =
                buildList {
                    add(Material(plank, plankCount))
                    if (nailCount > 0) {
                        add(Material(ConstructionDefinitions.nails.first(), nailCount))
                    }
                    extraMaterials.forEach { add(Material(it.obj, it.count)) }
                },
        )

    private fun hotspotKey(coords: CoordGrid): String = coords.packed.toString()

    private fun locInfo(coords: CoordGrid, id: Int, angle: LocAngle, shape: LocShape): LocInfo {
        val type = locType(id)
        val layer = LocLayerConstants.of(shape.id)
        return LocInfo(layer, coords, LocEntity(type.id, shape.id, angle.id))
    }

    private fun locType(id: Int): ObjectServerType =
        locTypeOrNull(id) ?: error("Missing construction loc: $id")

    private fun locTypeOrNull(id: Int): ObjectServerType? =
        ServerCacheManager.getObject(id)

    private data class FlatpackTask(
        val recipe: ConstructionRecipe,
        val amount: Int,
        val completed: Int,
    )

    private companion object {
        private const val CRYSTAL_SAW_BOOST = 3

        private const val CONSTRUCTION_INSTANCE_KEY = "construction_house"
        private const val YAMA_LAIR_REGION_ID = 5975
        private const val YAMA_HOUSE_PORTAL_LOC = 15477

        private val YAMA_HOUSE_PORTAL_COORD = CoordGrid(1505, 5602, 0)
        private val YAMA_HOUSE_PORTAL_EXIT = CoordGrid(1503, 5602, 0)
        private val YAMA_LAIR_INSTANCE_ENTRY =
            RegionLocal(level = 0, regionZoneX = 23, regionZoneZ = 87, localX = 31, localZ = 34)
    }
}
