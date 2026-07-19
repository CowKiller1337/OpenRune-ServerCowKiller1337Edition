# OpenRune Skill Progress

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
