# Combat Flex

Combat Flex is a Minecraft Forge 1.20.1 mod built around high-mobility combat, survival scaling, and compact progression. The mod mixes damage-based growth, a skill tree, throw-and-grab combat, a separate flight branch, and supersonic effects into one integrated gameplay package.

## Highlights

- Damage-based progression: taking damage builds progress and grants skill points.
- Compact skill tree: small icon-based UI with hover details and branch-specific prerequisites.
- Independent flight branch: Slow Flight and Fast Flight are unlocked in their own mini tree and do not conflict with the combat tree.
- Grab-and-throw combat: heavy attacks, grabbing, throwing blocks, and special thrown items.
- Supersonic flight effects: high-speed visuals, sound filtering, impact effects, and stronger buffs for advanced flight.

## Gameplay Overview

### Combat Growth

- Missing health increases your resistance and regeneration.
- Skill points are earned by surviving damage during a single life.
- Additional points can also be earned by surviving at 1 HP for long enough.

### Skill Tree

- The main combat branch unlocks defensive and survival-oriented abilities.
- The flight branch is separate.
- Slow Flight is a tier-2 flight skill.
- Fast Flight is a tier-3 flight skill and requires Slow Flight first.

### Special Combat Systems

- Grab and throw enemies, blocks, and special items.
- Iron nugget throws deal very high direct piercing damage.
- Repeated-damage adaptation can reduce or nullify certain recurring damage patterns.

## Controls

- `I`: Open skill tree
- `Mouse Button 4`: Charged heavy attack
- `Mouse Button 5`: Grab / throw
- `R`: Flight mode menu / toggle

## Admin Commands

These commands are intended for testing and admin use in Creative mode:

- `/cbtflex points add <amount>`: Add skill points instantly.
- `/cbtflex reset skills`: Reset all unlocked skills.
- `/cbtflex reset progression`: Reset damage progress, total received damage, and current skill points.

Notes:

- These commands are restricted to Creative-mode players.
- Vanilla Creative flight still works as normal.
- Mod-specific Slow Flight, Hover, and Fast Flight still require the related skills to be unlocked.

## Project Info

- Minecraft: 1.20.1
- Forge: 47.4.18
- Java target: 17
- Mod ID: `cbtflex`
- Current packaged release: `1.0stb3`

## Localization

The mod currently includes these language files:

- English
- Simplified Chinese
- Japanese
- German
- French
- Russian

## Building

On Windows:

```powershell
.\gradlew.bat build
```

The packaged jar will be generated in `build/libs/`.

## Repository Layout

- `src/main/java`: main mod source code
- `src/main/resources`: mod resources, language files, metadata, and icon
- `build/libs`: generated jars

## Notes

- The mod icon is loaded from the root of `src/main/resources` and referenced in `mods.toml`.
- The current release jar name is `cbtflex-1.0stb3.jar`.
