package org.rsmod.content.skills.mining

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.ObjectServerType
import dev.openrune.util.WeaponCategory
import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.area.checker.isInWilderness
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.miningLvl
import org.rsmod.api.player.stat.statRandom
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpContentLoc1
import org.rsmod.api.script.onOpContentLoc3
import org.rsmod.api.script.onOpContentLocU
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLoc4
import org.rsmod.api.script.onOpLoc5
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.stats.levelmod.InvisibleLevels
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.events.UnboundEvent
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.getInvObj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class Mining
@Inject
constructor(
    private val locRepo: LocRepository,
    private val objRepo: ObjRepository,
    private val areaChecker: AreaChecker,
    private val xpMods: XpModifiers,
    private val invisibleLvls: InvisibleLevels,
    private val random: GameRandom,
    private val mapClock: MapClock,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpContentLoc1("content.ore") { attempt(it.loc, it.type) }
        onOpContentLoc3("content.ore") { prospect(it.type) }
        onOpContentLocU("content.ore") {
            if (it.objType.weaponCategory == WeaponCategory.Pickaxe) {
                attempt(it.loc, it.type)
            } else {
                mes("Nothing interesting happens.")
            }
        }
        bindMineableLocOptions()
    }

    private fun ScriptContext.bindMineableLocOptions() {
        val types = ServerCacheManager.getObjects().values.filter { it.hasMineableOption }
        for (type in types) {
            bindMineOption(type)
            bindProspectOption(type)
            onOpLocU(type) {
                if (it.objType.weaponCategory == WeaponCategory.Pickaxe) {
                    attempt(it.loc, it.type)
                } else {
                    mes("Nothing interesting happens.")
                }
            }
        }
    }

    private fun ScriptContext.bindMineOption(type: ObjectServerType) {
        when (type.optionIndex("Mine")) {
            1 -> onOpLoc1(type) { attempt(it.loc, it.type) }
            2 -> onOpLoc2(type) { attempt(it.loc, it.type) }
            3 -> onOpLoc3(type) { attempt(it.loc, it.type) }
            4 -> onOpLoc4(type) { attempt(it.loc, it.type) }
            5 -> onOpLoc5(type) { attempt(it.loc, it.type) }
        }
    }

    private fun ScriptContext.bindProspectOption(type: ObjectServerType) {
        when (type.optionIndex("Prospect")) {
            1 -> onOpLoc1(type) { prospect(it.type) }
            2 -> onOpLoc2(type) { prospect(it.type) }
            3 -> onOpLoc3(type) { prospect(it.type) }
            4 -> onOpLoc4(type) { prospect(it.type) }
            5 -> onOpLoc5(type) { prospect(it.type) }
        }
    }

    private fun ProtectedAccess.attempt(rock: BoundLocInfo, type: ObjectServerType) {
        val blockMessage = type.miningBlockMessage
        if (blockMessage != null) {
            mes(blockMessage)
            return
        }

        val ore = type.resolveOre(random)
        if (ore == null) {
            mes("You cannot mine this rock.")
            return
        }

        if (player.miningLvl < ore.level) {
            mes("You need a Mining level of ${ore.level} to mine this rock.")
            return
        }

        if (inv.isFull()) {
            mes("Your inventory is too full to hold any more ${ore.inventoryName}.")
            soundSynth(INVENTORY_FULL_SOUND)
            return
        }

        val pickaxe = findPickaxe(player)
        if (pickaxe == null) {
            mes("You need a pickaxe to mine this rock.")
            mes("You do not have a pickaxe which you have the Mining level to use.")
            return
        }

        if (actionDelay < mapClock) {
            actionDelay = mapClock + pickaxe.delay
            skillAnimDelay = mapClock + 4
            anim(pickaxe.animation)
            soundSynth(MINE_SWING_SOUND)
            spam("You swing your pickaxe at the rock.")
            opLoc1(rock)
            return
        }

        if (skillAnimDelay <= mapClock) {
            skillAnimDelay = mapClock + 4
            anim(pickaxe.animation)
            soundSynth(MINE_SWING_SOUND)
        }

        var minedOre = false
        if (actionDelay == mapClock) {
            val (low, high) = ore.successRates(pickaxe.tier)
            minedOre = statRandom("stat.mining", low, high, invisibleLvls)
            actionDelay = mapClock + pickaxe.delay
        }

        if (minedOre) {
            val product = ore.rollProduct(player, random)
            val actualProduct = rollRandomGem(player, ore) ?: product
            val award = resolveAwardedOre(player, ore, product, actualProduct)
            val addResult = invAdd(inv, award.type.internalName, award.count)
            if (addResult.failure) {
                mes("Your inventory is too full to hold any more ${ore.inventoryName}.")
                soundSynth(INVENTORY_FULL_SOUND)
                resetAnim()
                return
            }

            val xp = ore.xp(product) * xpMods.get(player, "stat.mining")
            spam("You manage to mine some ${award.type.name.lowercase()}.")
            soundSynth(award.successSound)
            statAdvance("stat.mining", xp)
            publish(MineOre(player, rock, award.type))
            applyEnhancers(ore, award.bonusType, xp)
            rollClueGeode(ore)

            if (shouldDeplete(rock, ore)) {
                depleteRock(rock, type, ore)
                resetAnim()
                return
            }
        }

        opLoc1(rock)
    }

    private fun ProtectedAccess.prospect(type: ObjectServerType) {
        val blockMessage = type.miningBlockMessage
        if (blockMessage != null) {
            mes(blockMessage)
            return
        }

        val ore = type.resolveOre(random)
        if (ore == null) {
            mes("You examine the rock for ores but find nothing.")
            return
        }
        mes("This rock contains ${ore.prospectName}.")
    }

    private fun depleteRock(rock: BoundLocInfo, type: ObjectServerType, ore: OreDefinition) {
        val replacement = type.depletedRock
        if (replacement != null) {
            locRepo.change(rock, replacement, ore.respawnTicks)
        } else {
            locRepo.del(rock, ore.respawnTicks)
        }
    }

    private fun ProtectedAccess.resolveAwardedOre(
        player: Player,
        ore: OreDefinition,
        product: OreProduct,
        actualProduct: OreProduct,
    ): AwardedOre {
        val clayBraceletType = clayBraceletSoftClay(player, ore, product)
        if (clayBraceletType != null) {
            val count = if (ore.isSoftClayRock) 2 else 1
            return AwardedOre(
                type = clayBraceletType,
                bonusType = clayBraceletType,
                count = count,
                successSound = product.successSound,
            )
        }
        return AwardedOre(
            type = actualProduct.type,
            bonusType = product.type,
            count = 1,
            successSound = actualProduct.successSound,
        )
    }

    private fun clayBraceletSoftClay(
        player: Player,
        ore: OreDefinition,
        product: OreProduct,
    ): ItemServerType? {
        if (!ore.canBraceletClay || !player.wearingAny(clayBraceletIds)) {
            return null
        }
        if (!product.type.isType("obj.clay") && !product.type.isType("obj.softclay")) {
            return null
        }
        return softClayType
    }

    private fun ProtectedAccess.applyEnhancers(
        ore: OreDefinition,
        bonusType: ItemServerType,
        xp: Double,
    ) {
        val enhancers = ore.enhancers
        if (enhancers.miningCape && player.wearingAny(miningCapeIds) && random.randomBoolean(20)) {
            invAddOrDropType(objRepo, bonusType)
        }
        if (
            enhancers.varrockArmourTier != null &&
                player.varrockArmourTier() >= enhancers.varrockArmourTier &&
                random.randomBoolean(10)
        ) {
            invAddOrDropType(objRepo, bonusType)
            statAdvance("stat.mining", xp)
        }
        if (enhancers.ringOrSignet && player.wearingAny(celestialIds) && random.randomBoolean(10)) {
            invAddOrDropType(objRepo, bonusType)
        }
    }

    private fun ProtectedAccess.rollClueGeode(ore: OreDefinition) {
        val baseChance = ore.clueBaseChance ?: return
        if (baseChance <= 0) {
            return
        }
        val chance =
            if (player.wearingAny(ringOfWealthIds) && player.coords.isInWilderness(areaChecker)) {
                (baseChance / 2).coerceAtLeast(1)
            } else {
                baseChance
            }
        if (!random.randomBoolean(chance)) {
            return
        }

        val geode = random.pickOrNull(clueGeodeTypes) ?: return
        val added = invAddOrDropType(objRepo, geode)
        mes(
            if (added) {
                "You find a clue geode!"
            } else {
                "A clue geode falls to the ground as your inventory is too full."
            }
        )
    }

    private fun ProtectedAccess.shouldDeplete(rock: BoundLocInfo, ore: OreDefinition): Boolean {
        if (!ore.depletes) {
            return false
        }

        val rockKey = rock.depletionKey()
        val gloveProtection = ore.enhancers.miningGloves?.takeIf { player.wearingMiningGloves(it) }
        if (
            gloveProtection != null && !gloveProtectionDepleted(rockKey, gloveProtection.extraMines)
        ) {
            return false
        }

        val range = ore.depletionRange ?: return true
        val session = player.attr[DEPLETION_SESSION_ATTR]
        val current =
            if (session != null && session.rockKey == rockKey) {
                session.copy(mined = session.mined + 1)
            } else {
                DepletionSession(
                    rockKey = rockKey,
                    threshold = random.of(range.first, range.last),
                    mined = 1,
                )
            }

        return if (current.mined >= current.threshold) {
            player.attr.remove(DEPLETION_SESSION_ATTR)
            true
        } else {
            player.attr[DEPLETION_SESSION_ATTR] = current
            false
        }
    }

    private fun ProtectedAccess.gloveProtectionDepleted(rockKey: Long, extraMines: Int): Boolean {
        val session = player.attr[GLOVE_DEPLETION_SESSION_ATTR]
        val protectedMines = if (session != null && session.rockKey == rockKey) session.mined else 0
        if (protectedMines < extraMines) {
            player.attr[GLOVE_DEPLETION_SESSION_ATTR] =
                DepletionSession(
                    rockKey = rockKey,
                    threshold = extraMines,
                    mined = protectedMines + 1,
                )
            return false
        }
        player.attr.remove(GLOVE_DEPLETION_SESSION_ATTR)
        return true
    }

    private fun rollRandomGem(player: Player, ore: OreDefinition): OreProduct? {
        if (!ore.rollsRandomGems) {
            return null
        }

        val chance =
            if (player.wearingAny(gloryAmulets)) GLORY_RANDOM_GEM_CHANCE else RANDOM_GEM_CHANCE
        if (random.of(chance) != 0) {
            return null
        }

        val gems = randomGemProducts.resolveProducts(GEM_SUCCESS_SOUND)
        if (gems.isEmpty()) {
            return null
        }
        return rollProduct(gems, random)
    }

    public data class MineOre(
        public val player: Player,
        public val rock: BoundLocInfo,
        public val product: ItemServerType,
    ) : UnboundEvent

    private data class Pickaxe(
        val obj: InvObj,
        val type: ItemServerType,
        val tier: Int,
        val delay: Int,
        val animation: String,
    )

    private data class PickaxeDefinition(
        val keys: List<String>,
        val level: Int,
        val tier: Int,
        val delay: Int,
        val animationId: Int,
    ) {
        fun matches(type: ItemServerType): Boolean {
            val internal = type.internalName.substringAfter("obj.")
            return keys.any { internal.contains(it) }
        }
    }

    private data class OreDefinition(
        val keys: List<String>,
        val products: List<OreProduct>,
        val inventoryName: String,
        val prospectName: String,
        val level: Int,
        val xp: Double,
        val respawnTicks: Int,
        val low: Int,
        val high: Int,
        val depletes: Boolean,
        val depletionRange: IntRange?,
        val rollsRandomGems: Boolean,
        val enhancers: OreEnhancers,
        val clueBaseChance: Int?,
        val canBraceletClay: Boolean,
        val isSoftClayRock: Boolean,
    ) {
        fun matches(type: ObjectServerType): Boolean {
            val haystack = "${type.internalName} ${type.name} ${type.desc}".lowercase()
            return keys.any { haystack.contains(it) }
        }

        fun matches(product: ItemServerType): Boolean {
            val haystack = "${product.internalName} ${product.name}".lowercase()
            return keys.any { haystack.contains(it) }
        }

        fun successRates(@Suppress("UNUSED_PARAMETER") pickaxeTier: Int): Pair<Int, Int> {
            return low to high
        }

        fun rollProduct(player: Player, random: GameRandom): OreProduct {
            val eligible = products.filter { player.miningLvl >= it.minimumMiningLevel }
            if (eligible.isEmpty()) {
                return products.first()
            }
            if (eligible.any { it.minimumMiningLevel > 1 }) {
                return eligible.maxBy { it.minimumMiningLevel }
            }

            val totalWeight = eligible.sumOf { it.weight }
            var roll = random.of(totalWeight)
            for (product in eligible) {
                if (roll < product.weight) {
                    return product
                }
                roll -= product.weight
            }
            return eligible.last()
        }

        fun xp(product: OreProduct): Double = product.xp ?: xp
    }

    private data class OreProduct(
        val type: ItemServerType,
        val weight: Int,
        val xp: Double?,
        val minimumMiningLevel: Int,
        val successSound: String,
    )

    private data class AwardedOre(
        val type: ItemServerType,
        val bonusType: ItemServerType,
        val count: Int,
        val successSound: String,
    )

    private data class OreEnhancers(
        val miningGloves: MiningGloveTier? = null,
        val varrockArmourTier: Int? = null,
        val ringOrSignet: Boolean = false,
        val miningCape: Boolean = false,
    ) {
        companion object {
            val None = OreEnhancers()
        }
    }

    private enum class MiningGloveTier(val extraMines: Int, val itemNames: Set<String>) {
        Standard(extraMines = 1, itemNames = setOf("obj.mguild_gloves")),
        Expert(extraMines = 2, itemNames = setOf("obj.mguild_gloves_expert")),
        Superior(extraMines = 3, itemNames = setOf("obj.mguild_gloves_superior")),
    }

    private data class DepletionSession(val rockKey: Long, val threshold: Int, val mined: Int)

    private companion object {
        private const val XP_FINE_PRECISION: Double = 10.0
        private const val MINE_SWING_SOUND: String = "synth.clobber"
        private const val MINE_SUCCESS_SOUND: String = "synth.pick"
        private const val GEM_SUCCESS_SOUND: String = "synth.found_gem"
        private const val INVENTORY_FULL_SOUND: String = "synth.pillory_wrong"
        private const val RANDOM_GEM_CHANCE: Int = 256
        private const val GLORY_RANDOM_GEM_CHANCE: Int = 86

        private val DEPLETION_SESSION_ATTR = AttributeKey<DepletionSession>(temp = true)
        private val GLOVE_DEPLETION_SESSION_ATTR = AttributeKey<DepletionSession>(temp = true)

        private val gloryAmulets: Set<Int> by lazy {
            itemIds(
                "obj.amulet_of_glory_1",
                "obj.amulet_of_glory_2",
                "obj.amulet_of_glory_3",
                "obj.amulet_of_glory_4",
            )
        }

        private val miningCapeIds: Set<Int> by lazy {
            itemIds("obj.skillcape_mining", "obj.skillcape_mining_trimmed")
        }

        private val celestialIds: Set<Int> by lazy {
            itemIds("obj.celestial_ring", "obj.celestial_signet")
        }

        private val clayBraceletIds: Set<Int> by lazy {
            itemIds("obj.jewl_bracelet_of_clay", "obj.bracelet_of_clay")
        }

        private val ringOfWealthIds: Set<Int> by lazy {
            itemIds("obj.ring_of_wealth", "obj.ring_of_wealth_i")
        }

        private val varrockArmourIdsByTier: List<Set<Int>> by lazy {
            listOf(
                itemIds("obj.varrock_armour_easy"),
                itemIds("obj.varrock_armour_medium"),
                itemIds("obj.varrock_armour_hard"),
                itemIds("obj.varrock_armour_elite"),
            )
        }

        private val miningGloveIds: Map<MiningGloveTier, Set<Int>> by lazy {
            MiningGloveTier.entries.associateWith { tier -> itemIds(tier.itemNames) }
        }

        private val softClayType: ItemServerType? by lazy { itemTypeOrNull("obj.softclay") }

        private val clueGeodeTypes: List<ItemServerType> by lazy {
            itemTypes(
                "obj.mining_clue_geode_beginner",
                "obj.mining_clue_geode_easy",
                "obj.mining_clue_geode_medium",
                "obj.mining_clue_geode_hard",
                "obj.mining_clue_geode_elite",
            )
        }

        private val randomGemProducts: List<OreProductFallback> =
            products(
                product("obj.uncut_sapphire", weight = 70),
                product("obj.uncut_emerald", weight = 39),
                product("obj.uncut_ruby", weight = 39),
                product("obj.uncut_diamond", weight = 31),
            )

        private val ObjectServerType.product: ItemServerType?
            get() = paramOrNull(params.skill_productitem)

        private val ObjectServerType.emptyRock: ObjectServerType?
            get() = paramOrNull(params.next_loc_stage)

        private val ObjectServerType.depletedRock: ObjectServerType?
            get() = ServerCacheManager.getObject(DEPLETED_ORE_LOC) ?: emptyRock

        private val ObjectServerType.levelReq: Int?
            get() = paramOrNull(params.levelrequire)

        private val ObjectServerType.skillXp: Double?
            get() = paramOrNull<Int>(params.skill_xp)?.let { it / XP_FINE_PRECISION }

        private val ObjectServerType.respawnTicks: Int?
            get() = paramOrNull<Int>(params.respawn_time)?.takeIf { it > 0 }

        private val ObjectServerType.respawnTicksLow: Int?
            get() = paramOrNull<Int>(params.respawn_time_low)?.takeIf { it > 0 }

        private val ObjectServerType.respawnTicksHigh: Int?
            get() = paramOrNull<Int>(params.respawn_time_high)?.takeIf { it > 0 }

        private val ObjectServerType.hasMineableOption: Boolean
            get() = optionIndex("Mine") != null || optionIndex("Prospect") != null

        private val ObjectServerType.miningBlockMessage: String?
            get() {
                val key = internalName.substringAfter("loc.").lowercase()
                val text = "$key ${name.orEmpty()} ${desc.orEmpty()}".lowercase()
                if (
                    depletedMiningKeys.any { key == it } ||
                        depletedMiningMarkers.any { key.contains(it) }
                ) {
                    return "There is currently no ore available in this rock."
                }
                if (unsupportedMiningMarkers.any { text.contains(it) }) {
                    return "This Mining activity is not implemented yet."
                }
                return null
            }

        private fun ObjectServerType.optionIndex(option: String): Int? {
            for (op in 1..5) {
                val text = actions.getOpOrNull(op - 1) ?: continue
                if (text.equals(option, ignoreCase = true)) {
                    return op
                }
            }
            return null
        }

        private fun miningSeq(animationId: Int): String {
            return RSCM.getReverseMapping(RSCMType.SEQ, animationId)
        }

        private val fallbackMiningAnimation: String by lazy { miningSeq(RUNE_PICKAXE_ANIM) }

        private fun PickaxeDefinition.animation(): String {
            return runCatching { miningSeq(animationId) }.getOrDefault(fallbackMiningAnimation)
        }

        private const val BRONZE_PICKAXE_ANIM: Int = 625
        private const val IRON_PICKAXE_ANIM: Int = 626
        private const val STEEL_PICKAXE_ANIM: Int = 627
        private const val MITHRIL_PICKAXE_ANIM: Int = 629
        private const val ADAMANT_PICKAXE_ANIM: Int = 628
        private const val RUNE_PICKAXE_ANIM: Int = 624
        private const val DRAGON_PICKAXE_ANIM: Int = 7139
        private const val CRYSTAL_PICKAXE_ANIM: Int = 8347
        private const val DEPLETED_ORE_LOC: Int = 11390

        private val depletedMiningKeys: Set<String> =
            setOf(
                "rocks1",
                "rocks2",
                "rocks3",
                "amethystrock_empty",
                "limestone_rock_noore",
                "swamp_rock_noore",
                "punishrocks_no_ore",
                "tut2_empty_rock",
                "my2arm_saltrock_empty",
            )

        private val depletedMiningMarkers: List<String> =
            listOf("_empty", "_depleted", "depleted_", "_noore", "no_ore")

        private val unsupportedMiningMarkers: List<String> =
            listOf(
                "abyssal_barrier",
                "bim_boss",
                "blast mine",
                "blockage",
                "blocked",
                "castlewars",
                "corridor_rocks",
                "crashed star",
                "crimson_lovakite",
                "dt2_",
                "duke_sucellus",
                "easter24_",
                "fairytale2_",
                "fossil_ashpile",
                "fossil_mining_boulder",
                "fossil_volcano",
                "gauntlet_rock",
                "gotr_",
                "herorockslide",
                "infernal shale",
                "leadrock",
                "mah3_",
                "max_rockslide",
                "motherlode",
                "nickelrock",
                "raids_",
                "rock of dalgroth",
                "rock slide",
                "rockslide",
                "rockfall",
                "rubble",
                "rubium",
                "scar_amalgamation",
                "shooting star",
                "star_size",
                "tog_blue_stone",
                "vampyrium",
                "vmq3_",
                "zalcano",
            )

        private fun enhancers(
            gloves: MiningGloveTier? = null,
            varrock: Int? = null,
            ring: Boolean = false,
            cape: Boolean = false,
        ): OreEnhancers {
            return OreEnhancers(
                miningGloves = gloves,
                varrockArmourTier = varrock,
                ringOrSignet = ring,
                miningCape = cape,
            )
        }

        private val fallbackOres: List<OreFallback> =
            listOf(
                OreFallback(
                    listOf("daeyalt_stone_top_active"),
                    products("obj.blankrune_daeyalt"),
                    "daeyalt essence",
                    60,
                    5.0,
                    10,
                    18,
                    130,
                    rollsRandomGems = false,
                ),
                OreFallback(
                    listOf("area_sanguine_mine_minerocks", "daeyalt rocks"),
                    products("obj.daeyalt_shard"),
                    "daeyalt shards",
                    60,
                    5.0,
                    10,
                    18,
                    130,
                    rollsRandomGems = false,
                ),
                OreFallback(
                    listOf(
                        "big_essence_rock",
                        "blankrunestone",
                        "lunar_runestone_top",
                        "rune essence",
                    ),
                    products(
                        product("obj.blankrune"),
                        product("obj.blankrune_high", minimumMiningLevel = 30),
                    ),
                    "rune essence",
                    1,
                    5.0,
                    1,
                    255,
                    255,
                    depletes = false,
                    rollsRandomGems = false,
                    clueBaseChance = 317647,
                ),
                OreFallback(
                    listOf("softclayrock", "soft clay rocks"),
                    products("obj.softclay"),
                    "soft clay",
                    1,
                    5.0,
                    3,
                    90,
                    225,
                    enhancers = enhancers(varrock = 2, ring = true, cape = true),
                    canBraceletClay = true,
                    isSoftClayRock = true,
                ),
                OreFallback(
                    listOf("clayrock", "clay rocks", "clay ore vein"),
                    products("obj.clay"),
                    "clay",
                    1,
                    5.0,
                    2,
                    128,
                    400,
                    enhancers = enhancers(varrock = 1, ring = true, cape = true),
                    clueBaseChance = 741600,
                    canBraceletClay = true,
                ),
                OreFallback(
                    listOf("copperrock", "copper rocks", "copper ore vein"),
                    products("obj.copper_ore"),
                    "copper ore",
                    1,
                    17.5,
                    4,
                    100,
                    350,
                    enhancers =
                        enhancers(
                            gloves = MiningGloveTier.Standard,
                            varrock = 1,
                            ring = true,
                            cape = true,
                        ),
                    clueBaseChance = 741600,
                ),
                OreFallback(
                    listOf("tinrock", "tin rocks", "tin ore vein"),
                    products("obj.tin_ore"),
                    "tin ore",
                    1,
                    17.5,
                    4,
                    100,
                    350,
                    enhancers =
                        enhancers(
                            gloves = MiningGloveTier.Standard,
                            varrock = 1,
                            ring = true,
                            cape = true,
                        ),
                    clueBaseChance = 741600,
                ),
                OreFallback(
                    listOf("blurite_rock", "blurite rocks"),
                    products("obj.blurite_ore"),
                    "blurite ore",
                    10,
                    17.5,
                    42,
                    90,
                    350,
                    enhancers = enhancers(varrock = 1, ring = true, cape = true),
                    clueBaseChance = 741600,
                ),
                OreFallback(
                    listOf("ironrock", "iron rocks", "iron ore vein", "gim_ironrock"),
                    products("obj.iron_ore"),
                    "iron ore",
                    15,
                    35.0,
                    9,
                    96,
                    350,
                    enhancers =
                        enhancers(
                            gloves = MiningGloveTier.Standard,
                            varrock = 1,
                            ring = true,
                            cape = true,
                        ),
                    clueBaseChance = 741600,
                ),
                OreFallback(
                    listOf("silverrock", "silver rocks"),
                    products("obj.silver_ore"),
                    "silver ore",
                    20,
                    40.0,
                    100,
                    25,
                    200,
                    enhancers =
                        enhancers(
                            gloves = MiningGloveTier.Standard,
                            varrock = 1,
                            ring = true,
                            cape = true,
                        ),
                    clueBaseChance = 741600,
                ),
                OreFallback(
                    listOf("coalrock", "coal rocks", "coal ore vein"),
                    products("obj.coal"),
                    "coal",
                    30,
                    50.0,
                    50,
                    16,
                    100,
                    enhancers =
                        enhancers(
                            gloves = MiningGloveTier.Standard,
                            varrock = 1,
                            ring = true,
                            cape = true,
                        ),
                    clueBaseChance = 290640,
                ),
                OreFallback(
                    listOf(
                        "goldrock",
                        "gold rocks",
                        "gold vein",
                        "gold ore vein",
                        "dwarf_gold_mine",
                    ),
                    products("obj.gold_ore"),
                    "gold ore",
                    40,
                    65.0,
                    100,
                    7,
                    75,
                    enhancers =
                        enhancers(
                            gloves = MiningGloveTier.Standard,
                            varrock = 1,
                            ring = true,
                            cape = true,
                        ),
                    clueBaseChance = 296640,
                ),
                OreFallback(
                    listOf("gemrock", "gem rock", "gem rocks", "village_gem_rock"),
                    products(
                        product("obj.uncut_opal", weight = 469),
                        product("obj.uncut_jade", weight = 234),
                        product("obj.uncut_red_topaz", weight = 117),
                        product("obj.uncut_sapphire", weight = 70),
                        product("obj.uncut_emerald", weight = 39),
                        product("obj.uncut_ruby", weight = 39),
                        product("obj.uncut_diamond", weight = 31),
                    ),
                    "uncut gems",
                    40,
                    65.0,
                    99,
                    28,
                    70,
                    depletionRange = 3..3,
                    rollsRandomGems = false,
                    clueBaseChance = 211866,
                    successSound = GEM_SUCCESS_SOUND,
                ),
                OreFallback(
                    listOf("limestone_rock", "limestone rock"),
                    products("obj.limestone"),
                    "limestone",
                    10,
                    26.5,
                    4,
                    78,
                    205,
                    enhancers = enhancers(varrock = 1, ring = true, cape = true),
                ),
                OreFallback(
                    listOf("enakh_sandstone_rocks", "sandstone rocks"),
                    products(
                        product("obj.enakh_sandstone_tiny", weight = 5, xp = 30.0),
                        product("obj.enakh_sandstone_small", weight = 4, xp = 40.0),
                        product("obj.enakh_sandstone_medium", weight = 3, xp = 50.0),
                        product("obj.enakh_sandstone_large", weight = 2, xp = 60.0),
                    ),
                    "sandstone",
                    35,
                    30.0,
                    4,
                    45,
                    165,
                    enhancers =
                        enhancers(
                            gloves = MiningGloveTier.Expert,
                            varrock = 2,
                            ring = true,
                            cape = true,
                        ),
                    rollsRandomGems = false,
                ),
                OreFallback(
                    listOf("enakh_granite_rocks", "granite rocks"),
                    products(
                        product("obj.enakh_granite_tiny", weight = 5, xp = 50.0),
                        product("obj.enakh_granite_small", weight = 3, xp = 60.0),
                        product("obj.enakh_granite_medium", weight = 2, xp = 75.0),
                    ),
                    "granite",
                    45,
                    50.0,
                    4,
                    35,
                    150,
                    enhancers = enhancers(varrock = 2, ring = true, cape = true),
                    rollsRandomGems = false,
                ),
                OreFallback(
                    listOf("mithrilrock", "mithril rocks", "mithril ore vein"),
                    products("obj.mithril_ore"),
                    "mithril ore",
                    55,
                    80.0,
                    200,
                    4,
                    50,
                    enhancers =
                        enhancers(
                            gloves = MiningGloveTier.Superior,
                            varrock = 2,
                            ring = true,
                            cape = true,
                        ),
                    clueBaseChance = 148320,
                ),
                OreFallback(
                    listOf(
                        "adamantiterock",
                        "adamantite rocks",
                        "adamant ore vein",
                        "adamantite ore",
                    ),
                    products("obj.adamantite_ore"),
                    "adamantite ore",
                    70,
                    95.0,
                    400,
                    2,
                    25,
                    enhancers =
                        enhancers(
                            gloves = MiningGloveTier.Superior,
                            varrock = 3,
                            ring = true,
                            cape = true,
                        ),
                    clueBaseChance = 59328,
                ),
                OreFallback(
                    listOf("runiterock", "runite rocks", "runite ore"),
                    products("obj.runite_ore"),
                    "runite ore",
                    85,
                    125.0,
                    312,
                    1,
                    18,
                    enhancers = enhancers(gloves = MiningGloveTier.Expert, varrock = 4),
                    clueBaseChance = 42377,
                ),
                OreFallback(
                    listOf("amethystrock", "amethyst crystals"),
                    products("obj.amethyst"),
                    "amethyst",
                    92,
                    240.0,
                    125,
                    -18,
                    10,
                    depletionRange = 2..3,
                    enhancers = enhancers(gloves = MiningGloveTier.Expert, varrock = 4),
                    clueBaseChance = 46350,
                    rollsRandomGems = false,
                ),
                OreFallback(
                    listOf("sulphur_rock", "volcanic sulphur"),
                    products("obj.lovakengj_sulphur"),
                    "volcanic sulphur",
                    42,
                    25.0,
                    30,
                    28,
                    135,
                    enhancers = enhancers(ring = true, cape = true),
                ),
                OreFallback(
                    listOf("lovakite_rock", "lovakite rocks"),
                    products("obj.lovakite_ore"),
                    "lovakite ore",
                    65,
                    60.0,
                    59,
                    2,
                    50,
                    enhancers = enhancers(varrock = 2, ring = true, cape = true),
                    clueBaseChance = 245562,
                ),
                OreFallback(
                    listOf("my2arm_saltrock_special", "basalt rocks"),
                    products("obj.basalt"),
                    "basalt",
                    72,
                    15.0,
                    15,
                    18,
                    95,
                    rollsRandomGems = false,
                ),
                OreFallback(
                    listOf("camdozaalrock", "barronite rocks"),
                    products("obj.camdozaal_barronite_deposit"),
                    "barronite deposit",
                    14,
                    16.0,
                    4,
                    62,
                    180,
                    rollsRandomGems = false,
                ),
                OreFallback(
                    listOf("varlamore_mining_rock", "calcified rocks"),
                    products("obj.calcified_deposit"),
                    "calcified deposit",
                    41,
                    12.0,
                    8,
                    40,
                    150,
                    rollsRandomGems = false,
                ),
            )

        private val pickaxes: List<PickaxeDefinition> =
            listOf(
                PickaxeDefinition(
                    listOf("bronze_pickaxe"),
                    level = 1,
                    tier = 1,
                    delay = 8,
                    animationId = BRONZE_PICKAXE_ANIM,
                ),
                PickaxeDefinition(
                    listOf("iron_pickaxe"),
                    level = 1,
                    tier = 1,
                    delay = 7,
                    animationId = IRON_PICKAXE_ANIM,
                ),
                PickaxeDefinition(
                    listOf("steel_pickaxe"),
                    level = 6,
                    tier = 2,
                    delay = 6,
                    animationId = STEEL_PICKAXE_ANIM,
                ),
                PickaxeDefinition(
                    listOf("black_pickaxe"),
                    level = 11,
                    tier = 3,
                    delay = 5,
                    animationId = STEEL_PICKAXE_ANIM,
                ),
                PickaxeDefinition(
                    listOf("mithril_pickaxe"),
                    level = 21,
                    tier = 4,
                    delay = 5,
                    animationId = MITHRIL_PICKAXE_ANIM,
                ),
                PickaxeDefinition(
                    listOf("adamant_pickaxe"),
                    level = 31,
                    tier = 5,
                    delay = 4,
                    animationId = ADAMANT_PICKAXE_ANIM,
                ),
                PickaxeDefinition(
                    listOf("rune_pickaxe", "gilded_pickaxe"),
                    level = 41,
                    tier = 6,
                    delay = 3,
                    animationId = RUNE_PICKAXE_ANIM,
                ),
                PickaxeDefinition(
                    listOf(
                        "dragon_pickaxe",
                        "infernal_pickaxe",
                        "3a_pickaxe",
                        "zalcano_pickaxe",
                        "trailblazer_pickaxe",
                        "league_trailblazer_pickaxe",
                        "trailblazer_reloaded_pickaxe",
                    ),
                    level = 61,
                    tier = 7,
                    delay = 2,
                    animationId = DRAGON_PICKAXE_ANIM,
                ),
                PickaxeDefinition(
                    listOf("crystal_pickaxe"),
                    level = 71,
                    tier = 8,
                    delay = 2,
                    animationId = CRYSTAL_PICKAXE_ANIM,
                ),
            )

        private fun ObjectServerType.resolveOre(random: GameRandom): OreDefinition? {
            val product = product
            val fallback =
                if (product != null) {
                    fallbackOres.firstOrNull { it.matches(product) || it.matches(this) }
                } else {
                    fallbackOres.firstOrNull { it.matches(this) }
                }

            val products =
                fallback?.resolveProducts()
                    ?: product?.let { listOf(OreProduct(it, 1, null, 1, MINE_SUCCESS_SOUND)) }
            if (products.isNullOrEmpty()) {
                return null
            }

            return OreDefinition(
                keys =
                    fallback?.keys
                        ?: listOf(products.first().type.internalName.substringAfter("obj.")),
                products = products,
                inventoryName = fallback?.displayName ?: products.first().type.name.lowercase(),
                prospectName = fallback?.displayName ?: products.first().type.name.lowercase(),
                level = levelReq ?: fallback?.level ?: 1,
                xp = skillXp ?: fallback?.xp ?: 0.0,
                respawnTicks = resolveRespawnTicks(fallback, random),
                low = fallback?.low ?: 45,
                high = fallback?.high ?: 180,
                depletes = fallback?.depletes ?: true,
                depletionRange = fallback?.depletionRange,
                rollsRandomGems = fallback?.rollsRandomGems ?: true,
                enhancers = fallback?.enhancers ?: OreEnhancers.None,
                clueBaseChance = fallback?.clueBaseChance,
                canBraceletClay = fallback?.canBraceletClay ?: false,
                isSoftClayRock = fallback?.isSoftClayRock ?: false,
            )
        }

        private fun ObjectServerType.resolveRespawnTicks(
            fallback: OreFallback?,
            random: GameRandom,
        ): Int {
            respawnTicks?.let {
                return it
            }
            val low = respawnTicksLow
            val high = respawnTicksHigh
            if (low != null && high != null) {
                return random.of(low, high)
            }
            return fallback?.respawnTicks ?: 15
        }

        private fun findPickaxe(player: Player): Pickaxe? {
            val worn = player.wornPickaxe()
            val carried = player.carriedPickaxe()
            if (worn != null && carried != null) {
                return if (worn.tier >= carried.tier) worn else carried
            }
            return worn ?: carried
        }

        private fun Player.wornPickaxe(): Pickaxe? {
            val righthand = righthand ?: return null
            return righthand.toPickaxe(miningLvl)
        }

        private fun Player.carriedPickaxe(): Pickaxe? {
            return inv.filterNotNull { it.toPickaxe(miningLvl) != null }
                .mapNotNull { it.toPickaxe(miningLvl) }
                .maxByOrNull { it.tier }
        }

        private fun InvObj.toPickaxe(miningLevel: Int): Pickaxe? {
            val type = getInvObj(this)
            if (type.weaponCategory != WeaponCategory.Pickaxe) {
                return null
            }
            val definition = pickaxes.firstOrNull { it.matches(type) } ?: return null
            if (miningLevel < definition.level) {
                return null
            }
            return Pickaxe(this, type, definition.tier, definition.delay, definition.animation())
        }

        private fun OreFallback.matches(type: ObjectServerType): Boolean {
            val haystack = "${type.internalName} ${type.name} ${type.desc}".lowercase()
            return keys.any { haystack.contains(it) }
        }

        private fun OreFallback.matches(product: ItemServerType): Boolean {
            val haystack = "${product.internalName} ${product.name}".lowercase()
            return keys.any { haystack.contains(it) }
        }

        private fun OreFallback.resolveProducts(): List<OreProduct> {
            return products.resolveProducts(successSound)
        }

        private fun List<OreProductFallback>.resolveProducts(
            successSound: String
        ): List<OreProduct> {
            return mapNotNull { fallback ->
                val type =
                    ServerCacheManager.getItem(fallback.internal.asRSCM(RSCMType.OBJ))
                        ?: return@mapNotNull null
                OreProduct(
                    type = type,
                    weight = fallback.weight.coerceAtLeast(1),
                    xp = fallback.xp,
                    minimumMiningLevel = fallback.minimumMiningLevel,
                    successSound = successSound,
                )
            }
        }

        private fun rollProduct(products: List<OreProduct>, random: GameRandom): OreProduct {
            val totalWeight = products.sumOf { it.weight }
            var roll = random.of(totalWeight)
            for (product in products) {
                if (roll < product.weight) {
                    return product
                }
                roll -= product.weight
            }
            return products.last()
        }

        private fun product(
            internal: String,
            weight: Int = 1,
            xp: Double? = null,
            minimumMiningLevel: Int = 1,
        ): OreProductFallback {
            return OreProductFallback(internal, weight, xp, minimumMiningLevel)
        }

        private fun products(vararg internals: String): List<OreProductFallback> {
            return internals.map { product(it) }
        }

        private fun products(vararg products: OreProductFallback): List<OreProductFallback> {
            return products.toList()
        }

        private fun itemTypeOrNull(internal: String): ItemServerType? {
            return ServerCacheManager.getItem(internal.asRSCM(RSCMType.OBJ))
        }

        private fun itemTypes(vararg internals: String): List<ItemServerType> {
            return internals.mapNotNull(::itemTypeOrNull)
        }

        private fun itemIds(vararg internals: String): Set<Int> {
            return internals.mapNotNull { itemTypeOrNull(it)?.id }.toSet()
        }

        private fun itemIds(internals: Iterable<String>): Set<Int> {
            return internals.mapNotNull { itemTypeOrNull(it)?.id }.toSet()
        }

        private fun Player.wearingAny(items: Set<Int>): Boolean {
            return worn.any { obj -> obj != null && obj.id in items }
        }

        private fun Player.varrockArmourTier(): Int {
            for ((index, armourIds) in varrockArmourIdsByTier.withIndex()) {
                if (wearingAny(armourIds)) {
                    return index + 1
                }
            }
            return 0
        }

        private fun Player.wearingMiningGloves(tier: MiningGloveTier): Boolean {
            return wearingAny(miningGloveIds.getValue(tier))
        }

        private fun BoundLocInfo.depletionKey(): Long {
            return (coords.packed.toLong() shl 32) or
                (id.toLong() shl 16) or
                (shape.id.toLong() shl 8) or
                angle.id.toLong()
        }
    }

    private data class OreFallback(
        val keys: List<String>,
        val products: List<OreProductFallback>,
        val displayName: String,
        val level: Int,
        val xp: Double,
        val respawnTicks: Int,
        val low: Int,
        val high: Int,
        val depletes: Boolean = true,
        val depletionRange: IntRange? = null,
        val rollsRandomGems: Boolean = true,
        val enhancers: OreEnhancers = OreEnhancers.None,
        val clueBaseChance: Int? = null,
        val canBraceletClay: Boolean = false,
        val isSoftClayRock: Boolean = false,
        val successSound: String = MINE_SUCCESS_SOUND,
    )

    private data class OreProductFallback(
        val internal: String,
        val weight: Int = 1,
        val xp: Double? = null,
        val minimumMiningLevel: Int = 1,
    )
}
