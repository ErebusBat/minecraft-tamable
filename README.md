# Tameable Mooshrooms

A small, vanilla-compatible Fabric mod that lets you tame the two vanilla
mooshroom variants. A tamed mooshroom is owned, invulnerable to gameplay
damage, passive, persistent, and can follow you or stay in place.

No new items, blocks, entities, textures, GUIs, or network payloads — tamed
mooshrooms are still completely vanilla mooshrooms.

## How it works

| Action | Result |
| --- | --- |
| Right-click a **red** mooshroom with a **red mushroom** | Tames it (100%, first valid use) |
| Right-click a **brown** mooshroom with a **brown mushroom** | Tames it |
| Wrong-colored mushroom | Nothing happens; the item is not consumed |
| Owner right-clicks with an **empty hand** | Toggles FOLLOW ↔ STAY (confirmed on the action bar) |
| Shears on a tamed mooshroom | Blocked, so the pet cannot be turned back into a cow |
| Lightning strikes a tamed mooshroom | Variant flips red ↔ brown; ownership, mode, and invulnerability are kept |

Tamed mooshrooms:

- are immune to gameplay damage (attacks, projectiles, explosions, fire,
  lava, fall, drowning, suffocation, freezing, poison, wither, cactus,
  lightning, and the void) — but **not** to `/kill` or other deliberate
  administrative removal;
- never acquire attack targets or any combat behavior;
- follow their owner in the same dimension (start beyond 6 blocks, stop
  within 3, safe-teleport beyond 12 when they cannot keep up), and idle
  when the owner is offline or in another dimension;
- survive world save/reload, chunk unload/reload, server restarts, and
  lightning conversion; bred offspring are born untamed.

Everything else stays vanilla: milking, mushroom/suspicious stew, wheat
breeding, leads, name tags, shearing untamed mooshrooms, and the Minecraft
26.1 **Golden Dandelion** baby age-lock all behave exactly as in unmodified
vanilla, before and after taming.

## Requirements

- Minecraft Java Edition **26.1.2**
- Fabric Loader **0.19.3** or newer
- Fabric API **0.155.2+26.1.2** or newer for Minecraft 26.1.2
- Java **25** or newer

## Installation

The mod is server-authoritative: an unmodified vanilla client can join a
server running it. The same JAR works in both environments.

### Single-player

The integrated server runs inside your client, so install the mod in your
local instance:

1. Install Fabric Loader 0.19.3+ for Minecraft 26.1.2.
2. Put Fabric API and `tameable-mooshrooms-1.0.0.jar` into the instance's
   `mods` folder.
3. Launch the game.

### Dedicated server

1. Install Fabric Loader 0.19.3+ on the server for Minecraft 26.1.2.
2. Put Fabric API and `tameable-mooshrooms-1.0.0.jar` into the server's
   `mods` folder.
3. Start the server. Players can join with completely vanilla clients.

## Building

Requires Java 25. From a clean checkout:

```sh
./gradlew build
```

This compiles the mod, runs the unit tests, runs the in-game acceptance
test suite (`runGameTest`, spins up a temporary server), and produces
`build/libs/tameable-mooshrooms-1.0.0.jar`.

See `ACCEPTANCE.md` for the full acceptance matrix and how each scenario
was verified.

## Non-goals

- No Forge/NeoForge/Quilt/Bedrock support, and no other Minecraft versions.
- No taming of cows or any mob other than the two vanilla mooshrooms.
- No collars, custom models/textures/sounds/particles, GUI, or config screen.
- No random taming chance, healing, inventory, riding, combat, or owner
  defense.
- No tame inheritance through breeding, no ownership transfer, no untame
  feature.
- No cross-dimension teleport and no chunk force-loading.

## License

CC0-1.0
