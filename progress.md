# OpenRune Skill Progress

## Master Skill Coverage

Free-to-play:
- Attack: tracked in combat core, 70-75%.
- Strength: tracked in combat core, 70-75%.
- Defence: tracked in combat core, 70-75%.
- Ranged: tracked in combat core, 65-70%.
- Prayer: tracked as a skill module, 70-73%.
- Magic: tracked as combat/utility magic, 72-76%.
- Runecraft: tracked as Runecrafting, 68-72%.
- Hitpoints: tracked in combat core, 75-80%.
- Mining: tracked as a skill module, 85-88%.
- Smithing: tracked as a skill module, 70-75%.
- Fishing: tracked as a skill module, 82%.
- Cooking: tracked as a skill module, 75-80%.
- Firemaking: tracked as a skill module, 75-80%.
- Woodcutting: tracked as a skill module, 92-95%.
- Crafting: tracked as a skill module, 82-85%.

Members:
- Agility: first standalone course module, 60-65%.
- Herblore: tracked as a skill module, 70-75%.
- Thieving: first standalone pickpocket/stall/chest module, 70-73%.
- Fletching: tracked as a skill module, 82-85%.
- Slayer: tracked as a skill module, 70-75%.
- Farming: first standalone patch module, 68-72%.
- Construction: first standalone flatpack/direct hotspot module, 70-74%.
- Hunter: first standalone module with basic traps and direct catches, 68-72%.

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

Estimated completion: 70-73% for our Leagues-focused server.

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

Estimated completion: 82-85% for our Leagues-focused server.

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

Estimated completion: 82-85% for our Leagues-focused server.

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

Estimated completion: 68-72% for our Leagues-focused server.

Working:
- Barbarian fishing can award Agility XP as a side reward from the fishing module.
- First standalone Agility module compiles cleanly.
- Gnome Stronghold Agility Course has first-pass object hooks for log balance, obstacle nets, tree branches, balancing rope, and both pipes.
- Gnome obstacles check Agility level, move the player through the obstacle, award obstacle XP, and track ordered lap progress.
- Completing the Gnome course in order awards the OSRS-style lap bonus XP and stores a persistent lap count.
- Gnome log balance and tightrope now use a temporary balance walking set with real walking ticks, avoiding exact-move glide/stutter.
- Barbarian Outpost Agility Course has first-pass hooks for the rope swing, log balance, obstacle net, balancing ledge, and crumbling walls.
- Barbarian course obstacles check Agility level, move the player through the obstacle, award obstacle XP, and track ordered lap progress.
- Draynor Village Rooftop Course has first-pass hooks for the rough wall, tightropes, narrow wall, wall jump, gap jump, and final crate descent.
- Draynor rooftop awards the correct 120 XP total through obstacle XP and stores a persistent lap count.
- Al Kharid, Varrock, Canifis, Falador, Seers', Pollnivneach, Rellekka, Ardougne, Prifddinas, Wilderness, and Ape Atoll courses now have first-pass course data.
- Extra course data includes level checks, obstacle XP, lap tracking, and movement routes where the cache object ids and coordinates are known.

Still rough:
- Gnome net, branch, and pipe animations still need final in-game polish.
- Barbarian obstacle animations and coordinates need in-game proofing, especially the rope swing, ledge, and crumbling wall timings.
- Draynor rooftop needs in-game proofing for object start tiles, teleports, and jump/narrow-wall animation timings.
- Extra rooftop/normal courses need in-game proofing for exact start tiles, destination tiles, animation timing, and lap bonus behavior.
- Shortcuts, marks of grace, graceful outfit, stamina/restoration interactions, and obstacle failure logic are not implemented.
- Needs wider course data, OSRS sound effects, failure rolls, and route-specific polish.

## Farming

Estimated completion: 68-72% for our Leagues-focused server.

Working:
- Farming module compiles cleanly.
- First-pass allotment, herb, and flower patch lifecycle is wired.
- Clean patches accept supported seeds with level checks, seed dibber checks, seed counts, plant XP, saved patch state, and visual planted patch changes.
- Supported crops can be inspected while growing and harvested when ready for produce, herbs, flowers, and harvest XP.
- Patch state is persisted per player by tile, so planted crops are not only temporary memory.
- Basic compost use, spade clearing, and limited weed/rake progression are wired.
- Compost, supercompost, and ultracompost are tracked per patch and now improve harvest yield.
- Watering cans can water supported growing patches, decrement charges, and save watered state.
- Bush patches now support redberries, cadavaberries, dwellberries, jangerberries, white berries, and poison ivy berries with cache-backed clean/planted/ready objects.
- Hops patches now support barley, hammerstone, asgarnian, jute, yanillian, krandorian, and wildblood crops.
- Mushroom patches now support bittercap mushrooms.
- Cactus patches now support cactus spines and potato cactus with cache-backed planted/ready objects.
- Normal tree patches now support oak, willow, maple, yew, and magic saplings.
- Fruit tree patches now support apple, banana, orange, curry, pineapple, papaya, palm, and dragonfruit saplings.
- Tree and fruit-tree patches use check-health behavior, awarding health-check XP once and leaving the tree standing until cleared with a spade.
- Fruit trees can be picked after their health is checked, with six fruit per tree in this first pass.
- Tree and fruit-tree seeds can be potted with an empty plant pot and gardening trowel, then watered into usable saplings.
- Hardwood tree patches now support teak and mahogany saplings with check-health XP.
- Seaweed patches now support giant seaweed from seaweed spores/seeds, with simple yield and XP handling.
- Anima patches now support iasor, attas, and kronos plants as a first-pass planted/ready/check-clear lifecycle.
- Calquat patches now support calquat saplings, check-health XP, and calquat fruit picking.
- Celastrus patches now support celastrus saplings, check-health XP, and celastrus wood harvesting.
- Redwood patches now support redwood saplings with check-health XP as a first-pass lifecycle.

Still rough:
- Growth timings are deliberately shortened for testing and need OSRS cycle tuning later.
- Sapling growth from watered pot to sapling is immediate for now instead of a timed background stage.
- Disease, cure, payment protection, scarecrows, tool leprechauns, contracts, farming guild, Hespori, and white tree are not implemented yet.
- Some herb visuals use generic revision-239 herb patch ids until we confirm exact per-herb loc ids.
- Bushes need in-game testing for real patch option slots, loc shape swaps, item names, yields, and persistence after relog.
- Hops, mushroom, and cactus patch ids/yields need in-game testing against real 239 patch objects.
- Tree and fruit-tree patch ids, option slots, visuals, fruit picking, and persistence still need in-game testing.
- Hardwood tree, seaweed, and anima patch object ids/options need in-game testing against the real patch locations.
- Calquat, celastrus, and redwood patch object ids/options need in-game testing; redwood especially needs a later multi-object visual pass.

## Construction

Estimated completion: 70-74% for our Leagues-focused server.

Working:
- Construction module compiles cleanly and is bundled into the server content set.
- Saw-on-plank flatpack training is wired for chairs, bookcases, dining tables, kitchen tables, beds, dressers, wardrobes, clocks, and several costume-room storage flatpacks.
- Saw-on-plank flatpack training now accepts the normal saw, crystal saw, wearable saw, and wearable saw off-hand.
- Wearable saws, wearable hammers, and Imcando hammer variants now work from equipped gear as well as inventory.
- Crystal saw now gives its first-pass +3 Construction build-level boost for supported furniture building.
- Dining table coverage now includes carved oak and carved teak upgrades in addition to the earlier basic table set.
- Chair coverage now includes teak and mahogany armchairs.
- Opulent dining table flatpack training now requires marble blocks through the shared extra-material recipe support.
- Extra-material flatpacks now cover cloth, molten glass, gold leaf, marble blocks, and clockwork mechanisms where those recipes need them.
- Flatpack making checks Construction level, saw, hammer, planks, and nails where normal planks require them.
- Flatpack making opens the shared make-x skill menu, consumes materials, gives flatpack outputs, and awards Construction XP.
- Matching flatpacks can now be used on furniture hotspots to place the built furniture without paying the material cost twice.
- First-pass POH hotspot Build/Remove support exists for chair spaces, bookcase spaces, dining table spaces, kitchen table spaces, larder spaces, bed spaces, wardrobe spaces, and dresser spaces where those objects are present.
- Build-only hotspot recipes now work alongside flatpack recipes, so furniture that should be built directly in place no longer needs fake flatpack output data.
- Chapel altar hotspots now support oak, teak, cloth-covered teak, mahogany, limestone, marble, and gilded altar construction with matching removal back to the altar space.
- Portal hotspots now support teak, mahogany, and marble portal frame construction with matching removal back to the portal space.
- Larder training now covers wooden, oak, and teak larders, including the important oak larder route.
- Supported built furniture swaps back to the original hotspot when removed.
- POH portal clicks are caught with safe placeholder handling so they no longer fall through as a generic no-op.

Still rough:
- No real player-owned house instance, room creation, room rotation, house save model, or official build interface exists yet.
- Current training is a Leagues-friendly saw-on-plank flatpack loop, not the full OSRS workbench/POH flow.
- Many core furniture hotspot categories now have Build/Remove placement support, but clocks, costume-room storage, pools, gardens, workbenches, and room utility objects are still missing or flatpack-only.
- Portal frames can be built, but portal destination attunement and mounted teleport behavior are not implemented yet.
- Servants, house portals, mounted teleports, pools, costume room storage, portal nexus, menagerie, combat room, achievement gallery, and house options are not implemented.
- Needs in-game testing for new flatpack item names, skill menu display, material counts, hotspot object ids, build/remove swaps, and XP values.

## Hunter

Estimated completion: 68-72% for our Leagues-focused server.

Working:
- Hunter module compiles cleanly.
- Classic butterfly catching is wired for ruby harvest, sapphire glacialis, snowy knight, black warlock, sunlight moths, and moonlight moths.
- Butterflies require Hunter level, butterfly net or magic butterfly net, and an empty butterfly jar.
- Magic butterfly net now gives a first-pass catch-rate boost for butterflies and implings.
- Impling catching has a first playable slice for baby through dragon, crystal, and lucky implings.
- Puro-Puro maze impling variants are now mapped for baby through dragon and crystal implings.
- Implings require Hunter level, butterfly net or magic butterfly net, and an empty impling jar.
- Successful catches consume one empty jar, award the matching captured jar, give Hunter XP, and temporarily hide the caught NPC before it respawns.
- Catch success uses stat-based rolls, so higher Hunter improves catch reliability.
- Sunlight and moonlight moths use their revision-239 cache NPC/item names and official level/XP/catch-range values.
- Bird snares and box traps have a first playable lifecycle: lay trap, wait for a roll, inspect waiting traps, collect caught traps, collect failed traps, recover the trap item, and gain Hunter XP on catches.
- Trap limits scale by Hunter level: 1 trap at level 1, then 2/3/4/5 traps at levels 20/40/60/80.
- Basic bird snare rewards and chinchompa box trap rewards are wired with inventory-space checks.
- Box traps now choose standard, red, or black chinchompa rewards from nearby Hunter NPCs, including the correct Hunter level gate and XP for those chin types.
- Bird snares now check nearby bird Hunter NPC tiers before a trap can catch.
- Box traps now include hunting ferrets as a first-pass catch target.
- Direct catch support now covers green, orange, red, mountain, and black salamanders using rope plus the hunting net/snare item.
- First-pass pitfall-style direct catches now cover jaguar, leopard, snow tiger, fennec fox, Varlamore jaguar, sunlight antelope, and moonlight antelope with a teasing stick.
- Bird snare packs and box trap packs can be unpacked into usable traps.
- Deadfall boulders now support using logs on the boulder, resolving into caught/failed trap states, collecting rewards, and restoring the original boulder on collect, expiry, or logout.
- First-pass deadfall targets are wired for nearby clawed, barb-tailed, prickly, and sabre-toothed Hunter beasts using confirmed revision-239 gameval names.
- Sprung traps now expire after a short cleanup window, so abandoned caught/failed traps do not sit in the world forever.
- Active player-owned trap locs are removed when the player logs out, preventing ghost traps after disconnects or relogs.

Still rough:
- Needs in-game testing for click option slots, respawn timing, exact XP, and catch-rate accuracy.
- Catch animation is a safe first-pass animation and still needs the exact OSRS net-catching sequence.
- Moonlight moth spawns are not present in the current local map data, so their handler still needs in-game testing once we add or find the spawns.
- Trap capture is currently a timed player-owned roll, not a true NPC pathing/lure interaction yet.
- Bird species still use a simple feather reward until exact cache reward names are filled in.
- Salamanders and pitfall-style beasts are direct catches for now, not full net-trap/pitfall object flows.
- Deadfall is playable-first-pass, but it still needs exact object orientation variants, final XP/reward proofing, and true NPC lure/path behavior.
- Real net-trap objects, herbiboar, rumours, hunter gear, and full death/world-restart-safe trap persistence are not implemented.
- Needs region restrictions, exact trap object variants/options, OSRS timings, and reward tables.

## Thieving

Estimated completion: 68-72% for our Leagues-focused server.

Working:
- Thieving module compiles cleanly.
- Core pickpocketing is wired for citizens, H.A.M. members, rogues, farmers, master farmers, warriors, guards, Fremennik citizens, bandits, cave goblins, pirates, Menaphite thugs, gnomes, Ardougne knights, watchmen, paladins, heroes, vyres, and elves.
- Pickpocketing checks Thieving level, plays the pickpocket animation, rolls a stat-based success chance, awards XP, and gives the matching coin pouch or direct reward.
- Coin pouches can be opened into coins, including stacked pouches in one click.
- Failed pickpockets now apply a short stun-style action delay and deal tiered typeless damage.
- Dodgy necklace has first-pass fail protection, preventing the stun/damage on some failed pickpockets.
- Gloves of silence now have first-pass pickpocket fail protection using the revision-239 cache name.
- Full rogue outfit has first-pass double loot support for successful pickpockets.
- Bakery, silk, fur, spice, seed, fruit, wine, and crafting stalls have first-pass stealing support.
- Stalls check level and inventory space, award XP/items, and temporarily swap to an empty market stall before respawning.
- Basic thievable chest support exists for early coin/lockpick/seed reward chests.
- Thievable chests now apply a temporary per-player lockout for the chest respawn window, preventing immediate repeat looting.

Still rough:
- Needs in-game testing for NPC option slots, stall object ids, empty stall visuals, XP values, pouch quantities, and respawn timing.
- Doors, traps, wall safes, diaries, and other perk modifiers are not implemented yet.
- Chest lockouts are per-player temporary state, not full world-object depletion shared across every player.
- Gloves of silence degradation/charge behavior is not implemented yet.

## Crafting

Estimated completion: 82-85% for our Leagues-focused server.

Working:
- Crafting module compiles cleanly.
- Gem cutting is wired for opal, jade, red topaz, sapphire, emerald, ruby, diamond, dragonstone, onyx, and zenyte.
- Leather crafting is wired for basic leather gear through coif, plus hard leather bodies.
- Green, blue, red, and black dragonhide vambraces/chaps/bodies are wired.
- Glassblowing is wired for beer glasses, candle lanterns, oil lamps, vials, fishbowls, unpowered orbs, and lantern lenses.
- Spinning wheels are wired for wool, flax, sinew, and magic roots.
- Battlestaff orb attachment is wired for water, earth, fire, and air battlestaves.
- Gold jewellery is wired for rings, necklaces, bracelets, and unstrung amulets through zenyte.
- Silver jewellery is wired for opal/jade/topaz pieces, tiaras, holy symbols, unholy symbols, and silver sickles.
- Amulet and symbol stringing is wired with make-x support.
- Pottery is wired for soft clay shaping on pottery wheels and firing unfired pottery in pottery ovens.
- Amethyst can be cut into bolt tips, arrowheads, javelin heads, and dart tips.
- Tanner NPCs can tan cowhide, hard leather, dragonhide, snake hide, suqah hide, and yak hide using coins.
- Level checks, XP, make-x menus, weak queues, needle/thread checks, and chisel checks are in place.

Still rough:
- Needs in-game testing for gem cutting, leather/dragonhide, pottery, glassblowing, spinning, battlestaves, jewellery, silver crafting, and stringing make-x behavior.
- Thread behavior is simplified to one thread per crafted item for now.
- Tanning costs, tanner option slots, and niche hides need practical OSRS proofing.
- Animations/messages are first-pass and need OSRS polish.
- League relic effects are parked until the base Crafting loop exists.

## Fletching

Estimated completion: 82-85% for our Leagues-focused server.

Working:
- Fletching module compiles cleanly.
- Knife-on-log carving is wired for arrow shafts, normal/oak/willow/maple/yew/magic shortbows and longbows, and crossbow stocks through magic.
- Bow stringing is wired for normal through magic shortbows/longbows.
- Crossbow assembly and stringing is wired for bronze, blurite, iron, steel, mithril, adamant, rune, and dragon crossbows.
- Arrow shaft feathering is wired into headless arrows.
- Headless arrow plus arrowhead fletching is wired for bronze, iron, steel, mithril, adamant, rune, amethyst, and dragon arrows.
- Broad arrows, darts through dragon/amethyst, and unfeathered bolts through runite/silver are wired.
- Broad bolts, amethyst broad bolts, magic shields, redwood shields, and the main tipped bolt chain are wired.
- Javelin shafts, bronze through dragon javelins, amethyst javelins, and feathered dragon bolts are wired.
- Achey logs, ogre arrow shafts, wolfbone arrowheads, ogre arrows, ogre bows, and bronze through rune brutal arrows are wired.
- Ammunition can now be made from partial stacks instead of requiring a full default batch.
- Level checks, XP, make-x menus for bows, batch ammo production, weak queues, and basic inventory rollback are in place.
- Ammunition XP now scales per item in the batch instead of once per batch.

Still rough:
- Needs in-game testing for every bow tier, crossbow tier, ammo tier, XP amount, and level requirement.
- Ogre/brutal arrows, redwood stock-style edge cases, dragon bolt tipping, poison/barbed bolts, and amethyst bolt edge cases need proofing or implementation.
- Messages and animations are first-pass and need OSRS polish.
- League relic effects are parked until the base Fletching loop is trusted.

## Magic

Estimated completion: 72-76% for our Leagues-focused server.

Working:
- Alchemy, standard elemental combat spells, and standard spell teleports compile cleanly.
- Standard spell teleports consume normal rune requirements and award Magic XP after teleporting.
- Standard spell home teleport now sends players to the Yama lair starter area.
- Standard spell home teleport now stores a persistent 30-minute cooldown after a successful cast.
- Quest-locked standard teleports have first-pass quest requirement checks.
- Ape Atoll teleport has first-pass banana handling.
- Standard teleport tablets now have first-pass item-click support for Varrock, Lumbridge, Falador, POH fallback, Camelot, Ardougne, Watchtower, Kourend Castle, and Civitas illa Fortis.
- Fossil Island volcano and Lunar teleport tablets now have first-pass item-click support for Moonclan, Ourania, Waterbirth, Barbarian, Khazard, Fishing Guild, Catherby, and Ice Plateau.
- Ancient spellbook teleports are wired from cache destinations for Paddewwa, Senntisten, Kharyrll, Lassar, Dareeyak, Carrallangar, Annakarl, and Ghorrock.
- Lunar spellbook teleports are wired from cache destinations for Moonclan, Ourania, Waterbirth, Barbarian Outpost, Khazard, Fishing Guild, Catherby, and Ice Plateau/Ghorrock-style entries where present.
- Arceuus spellbook teleports are wired for the cache-backed teleport entries we could identify, including Battlefront, Mind Altar, Salve Graveyard, Fenkenstrain's Castle, West Ardougne, Harmony Island, Cemetery, Barrows, and Ape Atoll Dungeon.
- Ancient tablets and Arceuus teletabs now have first-pass item-click support.
- Ancient Magicks combat spells are wired for smoke, shadow, blood, and ice rush/burst/blitz/barrage.
- Ancient spell max hits follow the OSRS-style tier values for rush, burst, blitz, and barrage.
- Ancient spell effects have a first-pass implementation: smoke poisons players, shadow drains Attack, blood heals the caster, and ice freezes players.
- Ancient burst/barrage spells now have first-pass PvM area hits against nearby NPCs around the primary target.
- Elemental weakness support already exists in the combat formulas and standard elemental spell tags; it still needs practical combat proofing.

Still rough:
- League home teleport animation still needs proper implementation.
- Teleport tablet destinations and animation timing need in-game proofing.
- Ancient combat spell visuals currently use stable elemental-style graphics until exact ancient spotanim names are verified.
- Ancient burst/barrage area hits are PvM-first for now; PvP/multiway safety rules need a dedicated pass before enabling player splash targets.
- NPC-side ancient effects, especially freezing/draining NPC targets, need engine support or a dedicated NPC effect pass.
- Lunar and Arceuus combat/utility spells are not wired yet outside teleports.
- Utility spells, enchant spells, tele-other, charge spells, bind/snare/entangle polish, and niche spell behavior need implementation.
- Teleport destinations need OSRS proofing for every alternate/right-click option.

## Prayer

Estimated completion: 70-73% for our Leagues-focused server.

Working:
- Prayer module compiles cleanly.
- Bone burying is wired.
- Gilded altar support exists.
- Ectofuntus support exists, including slime, bonemeal, grinder, worship, and movement helpers.
- Bonecrusher, bonecrusher necklace, ash sanctifier, ectoplasmator, necklace of faith, zealot robes, and Catacombs prayer restore scripts exist.
- Blessed bone shard/libation bowl/sunfire wine support exists.
- Demonic Ruins prayer regeneration is wired.
- Prayer XP now respects the shared XP modifier path for burying/scattering, gilded altars, ectofuntus worship, libation bowl sacrifices, bonecrusher, ash sanctifier, and ectoplasmator.

Still rough:
- Needs in-game testing for burying/scattering, altar XP, ectofuntus flow, modifier XP, and all item charge behavior.
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

Estimated completion: 68-72% for our Leagues-focused server.

Working:
- Runecrafting module compiles cleanly.
- Standard altar crafting, combo runes, aether/core altar scripts, and tiara crafting are wired.
- Essence pouches, colossal pouch creation, pouch repair, Dark Mage/Cordelia dialogue, and storage hooks exist.
- Binding necklace, Magic Imbue, blood essence, and Raiments of the Eye support exists.
- Standard altar and Ourania XP now uses essence consumed instead of produced rune count, so multi-rune output no longer over-awards XP.
- Scar essence extracts are consumed when their bonus rune output is applied, without adding extra XP for the extract bonus.

Still rough:
- Needs in-game testing for all altars, rune amounts, level requirements, XP, extract consumption, and multiple-rune output.
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
