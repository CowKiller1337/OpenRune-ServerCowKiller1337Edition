# Weapons and Special Attacks

This branch contains both weapon fixes and special attack work.

## Included

- Weapon audit commands for finding broken weapon data.
- Safer ranged combat so missing projectile or spotanim data does not disconnect players.
- Fallback attack animations for bows, crossbows, thrown weapons, blowpipes, chinchompas, and salamanders.
- Extra server item params for newer revision weapons, ranged ammo, and common visual animation gaps.
- Salamander tar checks.
- Equipment stat requirement cleanup for wielding weapons.
- Special attack support for melee, ranged, magic, instant boost specs, and shields.
- Dark bow variant support, including Deadman/blighted variants.

## Current Status

- `::weaponaudit critical` shows `0` critical issues.
- `::specaudit` shows `0` weapons missing a registered special attack.
- `::weaponaudit visual` previously showed `185` visual issues. The latest definition pass should reduce this to roughly `37`, pending an in-game rerun of the audit command.

The remaining weapon work is mostly visual polish. These weapons should no longer crash combat, but a few still use fallback attack animations or placeholder projectile/spotanim data.

## Remaining Weapon Fixes

Biggest visual buckets expected after the latest cache-definition pass:

- Blunt weapons and event props: 26
- Small leftover weapon oddities: 11

Most real weapons now have stance, attack, block, projectile, or spotanim data assigned. The leftovers are mainly joke/event handhelds, claws/boxing gloves, and a few fringe weapons that need cache-perfect references before tuning.

## Remaining Special Attack Polish

- Some special attacks are implemented as close server-side approximations and still need OSRS-perfect formulas.
- Some newer/custom weapons, like Deadman variants, still need visual polish.
- A few effects need deeper combat-system support later, such as exact drains, stuns, binds, multi-hit timing, PvP restrictions, charge systems, and NPC-specific bonus rules.
- Dogsword special works, but the animation/visual result still needs tuning.

## Useful Test Commands

- `::specaudit`
- `::weaponaudit critical`
- `::weaponaudit visual`
- `::weaponaudit all`
- `::objdebug <item>`
- `::seqdebug <id>`
- `::spotdebug <id>`
