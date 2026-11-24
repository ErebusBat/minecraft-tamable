# Acceptance Report — Tameable Mooshrooms 1.0.0

Target: Minecraft Java 26.1.2, Fabric Loader 0.19.3, Fabric API 0.155.2+26.1.2, Java 25.

## Verification layers

1. **Unit tests** (`./gradlew test`, 15 tests): state model transitions,
   race/idempotency of ownership assignment, serialization round trip,
   fail-safe loading of malformed state, variant↔item rule table, and the
   damage-guard predicate. In `src/test/java`.
2. **In-game acceptance tests** (`./gradlew runGameTest`, 17 tests): the real
   interaction handler, damage guard, and persistence run against real
   mooshrooms and mock players in a live server level. In `src/gametest/java`.
   Also runs automatically as part of `./gradlew build`.
3. **Live dedicated-server smoke test**: dev server booted with the mod;
   behavior exercised through the server console (tame state injected via
   `/data merge`, which re-reads entity NBT through the mod's load path).

Result of the last full run: **15/15 unit tests passed, 17/17 game tests
passed, live-server checks passed.**

## Acceptance matrix (A01–A22)

| ID | Scenario | Result | Evidence |
| --- | --- | --- | --- |
| A01 | Red + red mushroom | PASS | Game test `redTamesWithRedMushroom` (tames, one consumed, owner set, FOLLOW) |
| A02 | Red + brown mushroom | PASS | Game test `redRejectsBrownMushroom` (PASS, no state change, nothing consumed) |
| A03 | Brown + brown mushroom | PASS | Game test `brownTamesWithBrownMushroom` |
| A04 | Brown + red mushroom | PASS | Game test `brownRejectsRedMushroom` |
| A05 | Creative matching mushroom | PASS | Game test `creativeTamingConsumesNothing` (tames, stack unchanged) |
| A06 | Spectator interaction | PASS | Game test `spectatorCannotTame` |
| A07 | Second player retame attempt | PASS | Game test `secondPlayerCannotRetame` (owner unchanged, nothing consumed) |
| A08 | Owner empty-hand use | PASS | Game test `ownerTogglesFollowStay` (FOLLOW→STAY→FOLLOW) |
| A09 | Non-owner empty-hand use | PASS | Game test `nonOwnerCannotToggleMode` |
| A10 | Damage-source sweep (tamed) | PASS | Game test `tamedMooshroomIsInvulnerable`; live server `/damage` → "Target is invulnerable". The guard hooks `LivingEntity.hurtServer` via `ServerLivingEntityEvents.ALLOW_DAMAGE`, which every gameplay source (attacks, projectiles, explosions, fire, lava, fall, drowning, suffocation, freezing, poison, wither, cactus, lightning, void) routes through, so the sweep reduces to one verified chokepoint. |
| A11 | Same damage on untamed | PASS | Game test `untamedMooshroomTakesVanillaDamage`; live server `/damage` → "Applied 5.0 damage" |
| A12 | Baby + Golden Dandelion | PASS | Game test `goldenDandelionBehaviorStaysVanilla`: handler returns PASS with no mutation; vanilla age lock then engages on an already-tamed baby |
| A13 | Second Golden Dandelion | PASS | Same test: after the vanilla 40-tick cooldown, second use resumes aging; custom state unchanged throughout |
| A14 | Save/restart/reload | PASS | Live server: tamed state injected, `save-all`, full server restart → NBT compound intact (`tamed:1b, owner, mode:"stay"`), damage still blocked. Chunk unload/reload uses the same entity NBT path. |
| A15 | Lightning conversion | PASS | Game test `lightningConversionPreservesTameState`; live server: lightning flipped `Type:"red"→"brown"` with the mod's NBT compound and invulnerability untouched |
| A16 | Breed tamed parents | PASS | Game test `bredOffspringIsUntamed` (child untamed and vulnerable) |
| A17 | Shears on tamed | PASS | Game test `shearsBlockedOnTamed` (FAIL, no durability loss, mooshroom not converted) |
| A18 | Shears on untamed | PASS | Game test `vanillaInteractionsPassThrough` (handler PASSes shears on untamed; conversion itself is unmodified vanilla code) |
| A19 | Bowl/bucket/name tag/lead/wheat/flowers | PASS | Game test `vanillaInteractionsPassThrough` asserts PASS for each, tamed and untamed |
| A20 | Dedicated server + vanilla client | PASS (design-verified) | No client entrypoint, no custom packets or assets required at runtime: feedback uses vanilla entity-status hearts, vanilla sounds, and translatable-with-fallback messages that render as English on vanilla clients. The modded dedicated server boots cleanly (smoke test). A physical vanilla-client join was not executed in this environment (no client install available); expected to work because no custom protocol exists. |
| A21 | Owner changes dimension | PASS (design-verified) | `FollowOwnerGoal` resolves the owner only via `level.getPlayerByUUID` in the mooshroom's own dimension; no cross-dimension lookup or teleport exists in code. Manual playthrough recommended for final sign-off. |
| A22 | Admin removal command | PASS | Live server: `/kill` on a tamed mooshroom succeeds ("Killed Mooshroom"); unit test `adminRemovalRemainsAvailableForTamedMooshrooms` |

## Regression checks (handoff 8.3)

- **No duplicated consumption / double interaction**: all mutation is gated
  on `!level.isClientSide()`; the client callback only mirrors SUCCESS for
  the arm swing. Covered by game tests (exact stack counts asserted).
- **Owner offline/deleted/dead/other dimension**: follow goal idles when the
  owner lookup returns null; no substitute owner is ever selected. Owner is
  stored as a UUID, never a live object reference.
- **No pathfinding loop / unsafe teleport**: teleport destinations use the
  same checks as vanilla tamed animals (`WalkNodeEvaluator.getPathTypeStatic`
  must be WALKABLE, no leaves below, `noCollision` hitbox space), excluding
  lava, fire, powder snow, and damaging blocks; following stops within 3
  blocks instead of colliding with the owner.
- **No state on other entities**: mixins target only `MushroomCow`;
  interaction handler and damage guard no-op for anything else.
- **No mixin overwrites**: only TAIL injections into `<init>`,
  `addAdditionalSaveData`, and `readAdditionalSaveData`; everything else
  uses Fabric events.
- **Vanilla client join**: no custom registry entries or packets; see A20.
- **No vanilla age-lock loss**: the mod never reads or writes `AgeLocked`;
  A12/A13 verify it across taming, and lightning conversion (A15) does not
  touch it either.
