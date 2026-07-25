# OpenRune Skill Progress

## Master Skill Coverage

Free-to-play:
- Attack: tracked in combat core, 70-75%.
- Strength: tracked in combat core, 70-75%.
- Defence: tracked in combat core, 70-75%.
- Ranged: tracked in combat core, 65-70%.
- Prayer: tracked as a skill module, 65-70%.
- Magic: tracked as combat/utility magic, 40-45%.
- Runecraft: tracked as Runecrafting, 65-70%.
- Hitpoints: tracked in combat core, 75-80%.
- Mining: tracked as a skill module, 85-88%.
- Smithing: tracked as a skill module, 70-75%.
- Fishing: tracked as a skill module, 82%.
- Cooking: tracked as a skill module, 75-80%.
- Firemaking: tracked as a skill module, 75-80%.
- Woodcutting: tracked as a skill module, 92-95%.
- Crafting: tracked as a skill module, 58-62%.

Members:
- Agility: first standalone course module, 18-22%.
- Herblore: tracked as a skill module, 70-75%.
- Thieving: missing standalone module, skipped for now, 0-5%.
- Fletching: tracked as a skill module, 68-72%.
- Slayer: tracked as a skill module, 70-75%.
- Farming: missing standalone module, 0-5%.
- Construction: missing standalone module, 0-5%.
- Hunter: missing standalone module, 0-5%.

## Attack

Estimated completion: 70-75% for our Leagues-focused server.

Working:
- Melee combat uses Attack-based accuracy formulas.
- Attack XP is awarded from accurate/shared melee combat styles.
- Weapon attack types, attack styles, speeds, stance selection, and combat tab updates are wired.
- Player-vs-npc, npc-vs-player, player-vs-player, and npc-vs-npc combat frameworks exist.

Still rough:
- Needs broad in-game testing across weapon types, attack styles, attack speeds, and NPC defence profiles.
- Weapon requirement checks, special-case weapon behavior, and exact OSRS animation/sound mappings need proofing.
- Special attacks exist only for some weapons so far.
- League relic effects are parked until base combat is trusted.

## Strength

Estimated completion: 70-75% for our Leagues-focused server.

Working:
- Melee max-hit formulas use Strength and equipment bonuses.
- Strength XP is awarded from aggressive/shared melee combat styles.
- Barbarian fishing can award Strength XP as a side reward from the fishing module.
- Core melee combat flow, weapon speeds, hit rolls, and damage application are wired.

Still rough:
- Needs max-hit proofing against OSRS for common weapons, prayers, potions, and gear.
- Special-case strength modifiers and weapon passives need more coverage.
- Special attacks exist only for some weapons so far.
- League relic effects are parked until base combat is trusted.

## Defence

Estimated completion: 70-75% for our Leagues-focused server.

Working:
- Defence is used in melee, ranged, and magic defence rolls.
- Defence XP is awarded from defensive/shared melee, ranged, and magic combat styles.
- Equipment stats and combat tab support exist.
- Damage reduction attributes and several combat formula hooks exist.

Still rough:
- Needs proofing across armour tiers, NPC attacks, attack styles, prayers, potions, and magic defence behavior.
- Gear requirement coverage and exact OSRS defensive bonuses need more testing.
- Damage reduction/passive item effects need a broader combat pass.
- League relic effects are parked until base combat is trusted.

## Ranged

Estimated completion: 65-70% for our Leagues-focused server.

Working:
- Ranged accuracy and max-hit formulas exist.
- Ranged XP is awarded from ranged combat styles.
- Ammo handling exists, including bow/crossbow ammunition checks and consumption.
- Dark bow special/ranged special weapon support exists.
- Fletching now supplies a bigger base of arrows, darts, bolts, and crossbow gear.

Still rough:
- Needs in-game testing for bows, crossbows, thrown weapons, darts, ammo compatibility, attack speeds, and XP modes.
- Chinchompas, cannons, blowpipe-style weapons, bolt effects, and many ranged special cases need implementation/proofing.
- Exact projectile graphics, animations, and sounds need OSRS polish.
- League relic effects are parked until base combat is trusted.

## Hitpoints

Estimated completion: 75-80% for our Leagues-focused server.

Working:
- Hitpoints starts at the cache-defined base level.
- Damage processors subtract Hitpoints and combat awards Hitpoints XP.
- Stat regeneration and healing hooks exist.
- Death handling exists through the player death/combat systems.
- Poison/disease-style mechanics have support in the toxins API.

Still rough:
- Needs in-game testing for death flow, respawn flow, food healing, poison/venom/disease, overheal behavior, and regeneration timing.
- Exact OSRS death edge cases, skull/item protection interactions, and boss-instance deaths need proofing.
- League relic effects are parked until base combat is trusted.

## Mining

Estimated completion: 85-88% for our Leagues-focused server.

Working:
- Core mining loop starts quickly, plays pickaxe animations, gives ore and XP, stops on full inventory, and respawns rocks.
- Standard ore rocks through rune and amethyst have been tested in-game.
- Rocks deplete into object `11390` where the cache does not provide a better depleted stage.
- Bracelet of clay converts clay to soft clay and supports soft clay rocks.
- Varrock armour extra ore works.
- Mining gloves now support tier upgrades correctly: superior protects basic/superior ore groups, and expert protects all supported glove groups.
- Mining cape, Varrock armour, and celestial ring/signet bonus ores now print a chat message when they trigger.
- Celestial ring/signet checks accept both charged and uncharged cache names.
- Clue geodes, random gems, glory gem boost, prospector XP, infernal pickaxe smelting, sandstone, granite, essence upgrades, and several special rocks have first-pass support.

Still rough:
- Mining gloves, mining cape, celestial ring, clue geodes, prospector XP, and infernal pickaxe need more in-game RNG testing.
- Celestial ring charge usage is not implemented yet.
- Bracelet of clay charge behavior is only basic.
- Sounds are first-pass and not fully OSRS matched.
- Success rates are good enough for playtesting, but still need final OSRS proofing.
- Minigame-style mining content such as Motherlode Mine and Blast Mine is intentionally parked.

## Woodcutting

Estimated completion: 92-95% for our Leagues-focused server.

Working:
- Normal trees through redwoods.
- Teak, mahogany, hollow trees, blisterwood, and baseline sulliusceps.
- Axe selection, woodcutting animations, XP, depletion, and respawn behavior.
- Infernal axe burn effect.
- Bird nest drops and nest searching.
- Woodcutting Guild invisible +7 boost.
- Basic shared-tree group boost for eligible trees.
- OSRS-style 4-tick success rolls.
- Cache-backed success rates for standard trees, with chart-backed fixes for crystal axe rates and arctic pine.
- Axe-aware Blisterwood and Sulliuscep rates instead of one flat special-tree chance.

Still rough:
- Forestry events and rewards.
- Full sulliuscep path/rotation mechanic, if we ever decide we need Kourend parity.
- Axe charge degradation.
- Confirm niche trees such as achey and arctic pine.
- Fuller clue-nest/clue-scroll integration.
- Further rate proofing for any trees where future cache/wiki data disagree.

## Fishing

Estimated completion: 82%.

Working:
- Standard fishing loop starts immediately, plays animations, gives catch items and XP, removes bait, stops when walking away, and respects full inventories.
- Common spot groups are wired, including shrimp/anchovies, bait fish, fly fishing, cages, harpoons, big net fish, monkfish, karambwanji, karambwan, lava eels, infernal eels, minnows, Tempoross harpoonfish, slimy eels, and anglerfish.
- Fishing spots are pinned so they no longer wander.
- Fish barrel support exists with open/close, fill, check, empty, and automatic catch storage while open.
- Rada's blessing, spirit flakes, clue bottles, heron rolls, and infernal harpoon cooking have first-pass support.
- Infernal eels now require ice gloves to catch and can be cracked with a hammer for Tokkul, lava scales, or onyx bolt tips.

Still rough:
- Success rates need final OSRS tuning.
- Minnow flying fish movement/penalty is not implemented.
- Dark crab wilderness diary perks and note behavior are not implemented.
- Tempoross is only the raw harpoonfish catch method, not the full minigame loop.
- Infernal harpoon charges are not consumed yet.
- Fish barrel needs in-game testing for persistence after logout.
- League relic effects are intentionally parked until the base skills are stable.

## Shooting Stars

Estimated completion: 70-75% for the standalone Shooting Stars system.

Working:
- Shooting Stars module compiles cleanly.
- Star locations, stage data, manager, settings, star mining, star teleport tablets, Dusuri dialogue/shop, and prospector dye scripts exist.
- The system is linked to the mining module and skilling utility module.

Still rough:
- Needs in-game testing for star spawning, stage depletion, mining XP, dust rewards, and respawn timing.
- Dusuri shop, telescope/tablet behavior, wilderness warning, and prospector dye behavior need practical testing.
- Exact OSRS timings and reward rates need proofing.

## Firemaking

Estimated completion: 75-80% for our Leagues-focused server.

Working:
- Firemaking module compiles cleanly.
- Normal tinderbox log burning is wired from the firemaking cache table.
- Bow/barbarian firemaking is wired for logs that expose a barbarian firemaking animation.
- Colored logs and colored fires are supported.
- Forester campfires can be created from basic fires and tended with logs.
- Light sources can be lit and extinguished.
- Pyromancer outfit XP bonus is wired.

Still rough:
- Needs in-game testing for normal logs through redwoods.
- Fire placement needs proofing around blocked tiles, doors, banks, and crowded areas.
- Fire duration/success rates are first-pass and need OSRS tuning.
- Bow firemaking requirements and animations need checking.
- Campfire behavior is mostly Forestry-adjacent; Forestry rewards/events are parked.
- Light sources need item-by-item testing.

## Cooking

Estimated completion: 75-80% for our Leagues-focused server.

Working:
- Cooking module compiles cleanly.
- Core food cooking is table-driven and covers common fish/meat/bread/pies/cakes/pizzas/stews.
- Wine fermenting is wired.
- Brewing is wired.
- Dough, pie assembly, pizza assembly, cake assembly, and stew assembly scripts exist.
- Cooking Guild door/head chef checks are wired.

Still rough:
- Needs in-game testing across raw fish tiers, burn rates, ranges, and fires.
- Burn/chance formulas need OSRS proofing.
- Cooking gauntlets, hosidius kitchen, range bonuses, and other niche modifiers need verification.
- Brewing and multi-step food assembly need practical testing.
- League relic effects are parked until the base cooking loop is trusted.

## Agility

Estimated completion: 22-25% for our Leagues-focused server.

Working:
- Barbarian fishing can award Agility XP as a side reward from the fishing module.
- First standalone Agility module compiles cleanly.
- Gnome Stronghold Agility Course has first-pass object hooks for log balance, obstacle nets, tree branches, balancing rope, and both pipes.
- Gnome obstacles check Agility level, move the player through the obstacle, award obstacle XP, and track ordered lap progress.
- Completing the Gnome course in order awards the OSRS-style lap bonus XP and stores a persistent lap count.
- Gnome log balance and tightrope now use a temporary balance walking set with real walking ticks, avoiding exact-move glide/stutter.

Still rough:
- Gnome net, branch, and pipe animations still need final in-game polish.
- Rooftop courses, other normal courses, shortcuts, marks of grace, graceful outfit, stamina/restoration interactions, and obstacle failure logic are not implemented.
- Needs wider course data, OSRS sound effects, failure rolls, and route-specific polish.

## Farming

Estimated completion: 0-5% for our Leagues-focused server.

Working:
- No standalone Farming module exists yet.

Still rough:
- Patches, seeds, growth cycles, compost, watering, disease, harvesting, tool leprechauns, payment protection, contracts, trees, herbs, allotments, flowers, hops, bushes, fruit trees, hardwoods, seaweed, mushrooms, Hespori, and farming guild systems are not implemented.
- Needs persistent patch state and timed growth handling before it can be considered playable.

## Construction

Estimated completion: 0-5% for our Leagues-focused server.

Working:
- No standalone Construction module exists yet.

Still rough:
- Player-owned houses, rooms, hotspots, furniture building/removal, servants, house portals, mounted teleports, pools, altars, costume room storage, portal nexus, and achievement gallery systems are not implemented.
- Needs a large design pass because Construction is instance-heavy and state-heavy.

## Hunter

Estimated completion: 0-5% for our Leagues-focused server.

Working:
- No standalone Hunter module exists yet.

Still rough:
- Bird snares, box traps, deadfall, net traps, pitfall, implings, chinchompas, salamanders, herbiboar, rumours, hunter gear, and trap persistence are not implemented.
- Needs trap object lifecycle, NPC capture hooks, region restrictions, and reward tables.

## Thieving

Estimated completion: 0-5% for our Leagues-focused server.

Working:
- No standalone Thieving module exists in this branch.

Still rough:
- Pickpocketing, stalls, chests, doors, traps, coin pouches, fail/stun behavior, rogues outfit, and diary/perk modifiers are not implemented here.
- This skill was intentionally skipped for now because someone else is working on it.

## Crafting

Estimated completion: 58-62% for our Leagues-focused server.

Working:
- Crafting module compiles cleanly.
- Gem cutting is wired for opal, jade, red topaz, sapphire, emerald, ruby, diamond, dragonstone, and onyx.
- Leather crafting is wired for basic leather gear through coif, plus hard leather bodies.
- Green, blue, red, and black dragonhide vambraces/chaps/bodies are wired.
- Glassblowing is wired for beer glasses, candle lanterns, oil lamps, vials, fishbowls, unpowered orbs, and lantern lenses.
- Spinning wheels are wired for wool, flax, sinew, and magic roots.
- Battlestaff orb attachment is wired for water, earth, fire, and air battlestaves.
- Level checks, XP, make-x menus, weak queues, needle/thread checks, and chisel checks are in place.

Still rough:
- Needs in-game testing for gem cutting, leather/dragonhide, glassblowing, spinning, and battlestaff make-x behavior.
- Thread behavior is simplified to one thread per crafted item for now.
- Jewellery, pottery, silver crafting, amethyst, tanning, and other OSRS crafting methods still need implementation.
- Animations/messages are first-pass and need OSRS polish.
- League relic effects are parked until the base Crafting loop exists.

## Fletching

Estimated completion: 68-72% for our Leagues-focused server.

Working:
- Fletching module compiles cleanly.
- Knife-on-log carving is wired for arrow shafts, normal/oak/willow/maple/yew/magic shortbows and longbows, and crossbow stocks through magic.
- Bow stringing is wired for normal through magic shortbows/longbows.
- Crossbow assembly and stringing is wired for bronze, blurite, iron, steel, mithril, adamant, rune, and dragon crossbows.
- Arrow shaft feathering is wired into headless arrows.
- Headless arrow plus arrowhead fletching is wired for bronze, iron, steel, mithril, adamant, rune, amethyst, and dragon arrows.
- Broad arrows, darts through dragon/amethyst, and unfeathered bolts through runite/silver are wired.
- Level checks, XP, make-x menus for bows, batch ammo production, weak queues, and basic inventory rollback are in place.
- Ammunition XP now scales per item in the batch instead of once per batch.

Still rough:
- Needs in-game testing for every bow tier, crossbow tier, ammo tier, XP amount, and level requirement.
- Bolt tipping, javelins, brutal arrows, ogre arrows, broad bolts, shields, redwood products, and amethyst bolt edge cases need implementation or proofing.
- Messages and animations are first-pass and need OSRS polish.
- League relic effects are parked until the base Fletching loop is trusted.

## Magic

Estimated completion: 40-45% for our Leagues-focused server.

Working:
- Alchemy, standard elemental combat spells, and standard spell teleports compile cleanly.
- Standard spell teleports consume normal rune requirements and award Magic XP after teleporting.
- Standard spell home teleport now sends players to the Yama lair starter area.
- Quest-locked standard teleports have first-pass quest requirement checks.
- Ape Atoll teleport has first-pass banana handling.

Still rough:
- League home teleport animation and 30-minute cooldown still need proper implementation.
- Teleport tablets need their weird animation sequence checked.
- Ancient, Lunar, and Arceuus spellbooks are not wired yet.
- Utility spells, enchant spells, tele-other, charge spells, bind/snare/entangle polish, and niche spell behavior need implementation.
- Elemental weakness behavior needs a separate combat pass.
- Teleport destinations need OSRS proofing for every alternate/right-click option.

## Prayer

Estimated completion: 65-70% for our Leagues-focused server.

Working:
- Prayer module compiles cleanly.
- Bone burying is wired.
- Gilded altar support exists.
- Ectofuntus support exists, including slime, bonemeal, grinder, worship, and movement helpers.
- Bonecrusher, bonecrusher necklace, ash sanctifier, ectoplasmator, necklace of faith, zealot robes, and Catacombs prayer restore scripts exist.
- Blessed bone shard/libation bowl/sunfire wine support exists.
- Demonic Ruins prayer regeneration is wired.

Still rough:
- Needs in-game testing for burying, altar XP, ectofuntus flow, and all item charge behavior.
- Bonecrusher/ash sanctifier NPC kill hooks need practical testing.
- Prayer drain, restoration, overhead behavior, and full prayer book polish are not covered by this module scan.
- League relic effects are parked until the base prayer behavior is trusted.

## Herblore

Estimated completion: 70-75% for our Leagues-focused server.

Working:
- Herblore module compiles cleanly.
- Herb cleaning, unfinished potions, finished potions, crushing, swamp tar, barbarian mixes, and common stat checks are wired.
- Amulet of chemistry and herblore goggles support exists.

Still rough:
- Needs in-game testing for every potion tier, level requirement, XP amount, and make-x flow.
- Amulet of chemistry charges and proc rates need proofing.
- Herblore goggles and any minigame/reward-only modifiers need testing.
- Niche potion edge cases and OSRS exact messages/animations need polish.
- League relic effects are parked until the base potion loop is trusted.

## Runecrafting

Estimated completion: 65-70% for our Leagues-focused server.

Working:
- Runecrafting module compiles cleanly.
- Standard altar crafting, combo runes, aether/core altar scripts, and tiara crafting are wired.
- Essence pouches, colossal pouch creation, pouch repair, Dark Mage/Cordelia dialogue, and storage hooks exist.
- Binding necklace, Magic Imbue, blood essence, and Raiments of the Eye support exists.

Still rough:
- Needs in-game testing for all altars, rune amounts, level requirements, XP, and multiple-rune output.
- Combo rune failure/chance behavior needs OSRS proofing.
- Abyss, quest gates, Guardians of the Rift, and other larger systems are not confirmed.
- Pouch decay/repair and modifier item charges need practical testing.
- League relic effects are parked until the base altar loop is trusted.

## Slayer

Estimated completion: 70-75% for our Leagues-focused server.

Working:
- Slayer module compiles cleanly.
- Assignment rolling, task manager, master profiles, standard master dialogue, boss task dialogue, Konar helpers, and task tips exist.
- Slayer reward shop, unlocks, tasks, points, blocks, and rewards handler are wired.
- Enchanted gem, bracelet of slaughter, expeditious bracelet, Slayer cape perk, and superior spawn framework exist.
- NPC kill hook support exists for task progress.

Still rough:
- Needs in-game testing for every master, task list, weighting, combat-level gate, task completion, and point streak.
- Konar area restrictions, superior spawn rates, bracelets, block/cancel/extend behavior, and reward unlocks need practical testing.
- Slayer helm/black mask and damage/accuracy modifiers need combat-side verification.
- Boss tasks and newer monster categories need OSRS proofing.
- League relic effects are parked until the base Slayer loop is trusted.

## Smithing

Estimated completion: 70-75% for our Leagues-focused server.

Working:
- Smithing module compiles cleanly.
- Anvil smithing is wired and has been lightly tested with a rune platebody.
- Bar smelting scripts exist.
- Cannonball smelting exists.
- Coal bag storage exists.
- Godsword assembly exists.
- Dragon forge and crystal singing scripts exist.
- Smithing outfit XP modifier is wired.

Still rough:
- Needs in-game testing for all bar tiers, anvil products, and interface quantities.
- Smelting formulas, coal requirements, and furnace behavior need OSRS proofing.
- Cannonball, coal bag, crystal singing, godsword assembly, and dragon forge need practical testing.
- Goldsmith gauntlets, ice gloves, smithing cape, Varrock armour interactions, and other modifiers need verification.
- League relic effects are parked until the base smithing loop is trusted.
