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
- Official OpenRune multi-hit melee support, including Scythe of Vitur and the bopper family.

## Current Status

- `::weaponaudit critical` shows `0` critical issues.
- `::specaudit` shows `0` weapons missing a registered special attack.
- `::weaponaudit visual` shows `0` visual issues.
- Official OpenRune `main` has been merged through `3dd2440cf`.

The audit commands are clean. Remaining work is deeper behaviour polish for exact OSRS formulas, timings, and special-case effects.

## Remaining Weapon Fixes

- No remaining critical or visual audit issues.
- Manual in-game animation review can still polish individual weapons if their cache-perfect animation differs from the sane fallback assigned here.

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
