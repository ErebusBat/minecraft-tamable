IMPLEMENTATION HANDOFF

Tameable & Invulnerable Mooshrooms for Minecraft Java

Target: Minecraft Java Edition 26.1.x; default implementation target
26.1.2

Platform: Fabric Loader + Fabric API; Java 25 toolchain

Status: Requirements converged; implementation-ready

Prepared: July 26, 2026

|**Build target:** Modify vanilla mooshrooms in place. A red mooshroom is tamed by one red mushroom; a brown mooshroom is tamed by one brown mushroom. Taming is deterministic. A tamed mooshroom is owned, invulnerable, passive, persistent, and can follow or stay. The Minecraft 26.1 Golden Dandelion behavior must remain completely vanilla.|
|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|

1. Purpose and Success Definition

Deliver a focused Fabric mod that adds ownership and companion behavior
to the two vanilla mooshroom variants without replacing the entity,
adding new items, or changing unrelated animals. The finished mod must
feel like a small vanilla-compatible feature, not a new creature system.

The implementation is successful only when all of the following are
simultaneously true:

• The matching ordinary mushroom tames an untamed mooshroom on the first
valid use.
• The wrong-colored mushroom never tames and is never consumed.
• Only tamed mooshrooms receive invulnerability, ownership, follow/stay
behavior, and persistence.
• Tamed mooshrooms never acquire an attack target or attack behavior.
• Golden Dandelions still pause and resume baby aging exactly as
Minecraft 26.1 defines, before or after taming.
• Existing vanilla interactions that are not explicitly overridden
continue to work.
• State survives save/reload, chunk unload/reload, server restart, and
red/brown lightning conversion.
• The behavior is authoritative on the server and safe under multiplayer
races.

2. Locked Product Decisions

|**Do not reinterpret these requirements.** The decisions below are deliberate defaults intended to prevent an implementation agent from inventing mechanics, changing scope, or pausing for avoidable product questions.|
|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|

|**Decision**     |**Required behavior**                                                                                                    |
|-----------------|-------------------------------------------------------------------------------------------------------------------------|
|Target entity    |Only vanilla mooshrooms (red and brown variants).                                                                        |
|Red taming       |Red mooshroom + minecraft:red_mushroom.                                                                                  |
|Brown taming     |Brown mooshroom + minecraft:brown_mushroom.                                                                              |
|Taming chance    |100% on the first valid server-side interaction.                                                                         |
|Consumption      |Consume exactly one matching mushroom in Survival/Adventure; consume none in Creative.                                   |
|Who may tame     |A non-spectator player; first successful player becomes sole owner.                                                      |
|When invulnerable|Immediately after successful taming; never before taming.                                                                |
|Passive behavior |No attack AI, retaliation, owner-defense, or target acquisition.                                                         |
|Initial mode     |FOLLOW.                                                                                                                  |
|Mode control     |Owner right-clicks with an empty hand to toggle FOLLOW ↔ STAY.                                                           |
|Baby aging       |Golden Dandelion remains a vanilla interaction and is never a taming item.                                               |
|Offspring        |Born untamed and vulnerable; each child must be tamed separately.                                                        |
|Lightning        |Variant changes, but owner, tame state, invulnerability, and mode remain.                                                |
|Shearing         |Blocked for tamed mooshrooms to prevent conversion into an untamed cow and accidental pet loss.                          |
|Admin removal    |Administrative entity removal commands remain available; invulnerability is gameplay damage immunity, not undeletability.|

3. Interaction Contract

Interaction order is a compatibility requirement. The mod must intercept
only the cases it owns and return the equivalent of PASS for everything
else so vanilla logic and other mods retain an opportunity to handle the
interaction.

|**Held item**                               |**Entity state**  |**Result**        |**Notes**                                                         |
|--------------------------------------------|------------------|------------------|------------------------------------------------------------------|
|Golden Dandelion                            |Any baby mooshroom|PASS to vanilla   |Never consume or duplicate in custom code.                        |
|Matching mushroom                           |Untamed           |Tame              |Consume one unless Creative; hearts + tame sound/status.          |
|Wrong mushroom                              |Untamed           |PASS              |No state change; no consumption.                                  |
|Either mushroom                             |Already tamed     |PASS              |No retaming and no ownership transfer.                            |
|Empty hand                                  |Tamed + owner     |Toggle FOLLOW/STAY|Server message confirms new mode.                                 |
|Empty hand                                  |Tamed + non-owner |PASS              |No state change and no ownership disclosure required.             |
|Shears                                      |Tamed             |Block             |No conversion; no durability loss; brief owner-facing explanation.|
|Bowl, bucket, wheat, lead, name tag, flowers|Any               |PASS              |Preserve vanilla behavior.                                        |
|Any other item                              |Any               |PASS              |No custom action.                                                 |

4. Core Behavior Requirements

4.1 Taming and ownership

• Perform all mutation on the logical server. Client prediction must not
consume items or set ownership.
• Atomically re-check that the entity is untamed before assigning
ownership. If two players interact in the same tick, exactly one wins
and at most one mushroom is consumed.
• Store the owner as a UUID, not a player name or live object reference.
• Do not permit retaming or ownership transfer through mushrooms.
• On success, emit vanilla-style heart particles/entity status and an
appropriate vanilla sound. Do not add custom assets.
• Taming an adult or baby mooshroom is allowed.

4.2 Invulnerability

Gameplay damage to a tamed mooshroom must be canceled before health
changes. This includes player and mob attacks, projectiles, explosions,
fire and lava, fall damage, drowning, suffocation, freezing,
poison/wither damage, cactus, lightning damage, and void damage.

• Untamed mooshrooms retain normal vanilla damage behavior.
• No damage animation, knockback caused solely by the canceled damage,
death loot, or death event should occur.
• Administrative removal such as /kill or direct entity discard is
intentionally not blocked.
• Do not apply a visible potion effect or change max health to simulate
invulnerability.

4.3 Passive behavior

Mooshrooms are passive in vanilla. Preserve that behavior. The mod must
not install melee, retaliation, owner-defense, anger, or
target-selection goals. Following the owner is movement, not combat. If
another mod adds hostile behavior, this mod is not required to rewrite
that mod’s AI, but it must never add hostile behavior itself.

4.4 Follow and stay

• Newly tamed mooshrooms begin in FOLLOW mode.
• FOLLOW: navigate toward the owner when the owner is in the same
dimension and meaningfully outside the stop radius; stop near the
owner rather than colliding continuously.
• Recommended constants: start following beyond 6 blocks; stop within 3
blocks; consider a safe teleport beyond 12 blocks when pathfinding
cannot reasonably catch up.
• Safe teleport must require a valid floor, enough collision-free space
for the mooshroom hitbox, and no lava, fire, powder snow, or damaging
block at the destination.
• Do not teleport across dimensions. Do not force-load chunks.
• Suspend following while the mooshroom is leashed, riding, being
ridden, inside a vehicle, or in STAY mode.
• STAY: cancel active navigation and prevent voluntary AI movement.
Environmental movement such as flowing water, pistons, or being pushed
is not required to be canceled.
• If the owner is offline or unavailable, the mooshroom idles; it must
never select a substitute owner.

4.5 Golden Dandelion compatibility

|**Critical compatibility invariant:** The mod must never consume, replace, reinterpret, or short-circuit minecraft:golden_dandelion. A baby mooshroom can be age-locked before or after it is tamed, and using a second Golden Dandelion resumes aging exactly as vanilla 26.1 specifies.|
|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|

• Return PASS immediately for Golden Dandelion interactions; let vanilla
determine eligibility, particles, sound, consumption, and age-lock
state.
• Do not store a second custom “forever baby” flag.
• Do not reset vanilla age-lock state when taming, saving, loading,
changing variant, toggling mode, or breeding.

5. Persistence and State Model

Use one authoritative state representation attached to each vanilla
mooshroom. The implementation may use a small mixin-backed access
interface plus namespaced NBT persistence, or an equivalent
Fabric-supported persistent attachment available in the exact target
API. Do not maintain parallel NBT and in-memory models that can diverge.

|**Field**     |**Type**       |**Default**|**Invariant**                                                    |
|--------------|---------------|-----------|-----------------------------------------------------------------|
|tamed         |boolean        |false      |Whether custom tame behavior is active.                          |
|owner_uuid    |UUID / optional|absent     |Required when tamed; clear/repair invalid partial state on load. |
|mode          |enum/string    |FOLLOW     |FOLLOW or STAY; unknown future values fall back safely to FOLLOW.|
|schema_version|integer        |1          |Allows future migration without guessing old layouts.            |

• Namespace every serialized key with the mod ID.
• Persist state through world save, server restart, chunk unload/reload,
and entity serialization.
• Do not store Golden Dandelion age-lock state; vanilla owns it.
• On load, a tamed=true record without a valid owner UUID must fail safe
to untamed rather than create an ownerless immortal pet.
• Lightning variant conversion must not replace or clear the entity’s
custom state.
• Breeding must not copy state to the child.

6. Recommended Fabric Architecture

Prefer public Fabric events over mixins. Use narrowly scoped mixins only
where the current 26.1.x API has no stable hook for mooshroom
persistence or goal registration. Confirm exact method and class names
against the selected mappings at build time; do not copy identifiers
from older Minecraft tutorials.

1. Create the project from the current Fabric template for Minecraft
26.1.2 and Java 25.
2. Define a small access contract for tame state, owner UUID, and mode
on vanilla mooshroom instances.
3. Persist that state with namespaced entity data; inject only into
save/load lifecycle points if no supported attachment API is
suitable.
4. Handle player interaction through UseEntityCallback or the
least-invasive current equivalent; explicitly PASS all unowned
interactions.
5. Cancel gameplay damage through ServerLivingEntityEvents.ALLOW_DAMAGE
or the current 26.1.x equivalent, with a narrow predicate for tamed
mooshrooms.
6. Register custom follow/stay movement goals for mooshrooms without
modifying combat goals or replacing the entity type.
7. Use vanilla packets/status events for hearts, sounds, and player
feedback so no custom client protocol is required.

6.1 Suggested internal structure

```text
src/main/java/<package>/
  TameableMooshroomsMod.java
  api/MooshroomTameAccess.java
  behavior/MooshroomInteractions.java
  behavior/MooshroomDamageGuard.java
  behavior/FollowOwnerGoal.java
  behavior/StayGoal.java
  mixin/MushroomCowStateMixin.java
  mixin/MushroomCowGoalsMixin.java
src/main/resources/
  fabric.mod.json
  <modid>.mixins.json
  assets/<modid>/lang/en_us.json
```

Names above are organizational suggestions, not permission to assume
mapping names. The implementation agent must use the actual deobfuscated
26.1.2 class names exposed by the selected Fabric toolchain.

6.2 Distribution model

• No custom items, blocks, entities, attributes, recipes, textures, or
network payloads are required.
• Design the mod to run server-authoritatively and permit unmodified
clients to join a dedicated server.
• Set fabric.mod.json environment to * (or the current
common-environment equivalent) so the same JAR loads on a dedicated
server and inside a single-player client; do not register a
client-only entrypoint.
• For single-player, install the mod and Fabric API in the local
instance because the integrated server runs in the client process.
• Keep common code free of dedicated-client-only classes. A client
initializer should not be necessary.

7. Vanilla Compatibility and Edge Cases

|**Case**             |**Required outcome**                                                                    |
|---------------------|----------------------------------------------------------------------------------------|
|Milking/stew         |Bowls and buckets retain vanilla behavior on tamed and untamed mooshrooms.              |
|Brown suspicious stew|Flower-related vanilla behavior remains available; custom code passes unrelated flowers.|
|Golden Dandelion     |Vanilla age lock toggles unchanged; custom state remains unchanged.                     |
|Leads/fences         |Vanilla leash behavior remains available; following pauses while leashed.               |
|Name tags            |Vanilla naming remains available.                                                       |
|Wheat/breeding       |Vanilla breeding remains available; child starts untamed.                               |
|Lightning            |Red ↔ brown conversion works; tame state and mode survive.                              |
|Shearing             |Blocked only after taming; untamed mooshrooms shear normally.                           |
|Portals              |Vanilla portal behavior is not replaced; no custom cross-dimensional owner teleport.    |
|Owner offline        |Entity idles and keeps owner UUID.                                                      |
|Chunk unload         |No force loading; state restores when loaded.                                           |
|Commands             |Administrative removal remains possible.                                                |

8. Automated and Manual Verification

Use automated tests where the target toolchain makes them reliable, then
complete the manual matrix on both an integrated server and a dedicated
server. A green build alone is not acceptance.

8.1 Automated tests

• Variant-to-item matching: red accepts only red mushroom; brown accepts
only brown mushroom.
• Tame transaction: exactly one item consumed, owner UUID set, mode
FOLLOW, tamed true.
• Creative transaction: no item consumed.
• Race/idempotency: repeated or simultaneous handling cannot transfer
ownership or consume extra items.
• Serialization round trip: all custom fields survive and malformed
partial state fails safe.
• Damage predicate: only tamed mooshrooms are protected.
• Breeding: child does not inherit custom tame state.
• Golden Dandelion path: custom interaction handler returns PASS without
mutation.

8.2 Mandatory gameplay acceptance matrix

|**ID**|**Scenario**                     |**Expected result**                                  |
|------|---------------------------------|-----------------------------------------------------|
|A01   |Red + red mushroom               |Tames; one consumed; hearts; owner set; FOLLOW.      |
|A02   |Red + brown mushroom             |No custom effect; nothing consumed.                  |
|A03   |Brown + brown mushroom           |Tames; one consumed; hearts; owner set; FOLLOW.      |
|A04   |Brown + red mushroom             |No custom effect; nothing consumed.                  |
|A05   |Creative matching mushroom       |Tames; stack count unchanged.                        |
|A06   |Spectator interaction            |No taming; no consumption.                           |
|A07   |Second player retame attempt     |Ownership unchanged; nothing consumed.               |
|A08   |Owner empty-hand use             |FOLLOW/STAY toggles once; feedback matches state.    |
|A09   |Non-owner empty-hand use         |No state change.                                     |
|A10   |Damage-source sweep              |Health unchanged for every gameplay source.          |
|A11   |Same damage on untamed           |Vanilla health loss occurs.                          |
|A12   |Baby + Golden Dandelion          |Vanilla age lock activates; custom state unchanged.  |
|A13   |Second Golden Dandelion          |Vanilla aging resumes; custom state unchanged.       |
|A14   |Save/restart/reload              |Owner, tame state, mode, invulnerability all persist.|
|A15   |Lightning conversion             |Variant toggles; owner/tame/mode persist.            |
|A16   |Breed tamed parents              |Child is untamed and vulnerable.                     |
|A17   |Shears on tamed                  |Conversion blocked; no durability loss.              |
|A18   |Shears on untamed                |Vanilla cow conversion works.                        |
|A19   |Bowl/bucket/name tag/lead/wheat  |Each vanilla interaction still works.                |
|A20   |Dedicated server + vanilla client|Join succeeds; all behavior is server authoritative. |
|A21   |Owner changes dimension          |No cross-dimension teleport; pet remains stable.     |
|A22   |Admin removal command            |Entity can be deliberately removed.                  |

8.3 Regression checks

• No duplicated item consumption or double interaction caused by
client/server callbacks.
• No crash when owner is offline, deleted, dead, or in another
dimension.
• No pathfinding loop, teleport into unsafe blocks, or continuous
collision with owner.
• No custom state copied to ordinary cows or other entities.
• No mixin overwrite of a whole vanilla method when an inject/event can
preserve compatibility.
• No warning or incompatibility preventing a vanilla client from joining
the dedicated server.
• No loss of vanilla age-lock state after taming or variant conversion.

9. Explicit Non-Goals

• No support for Forge, NeoForge, Quilt, Bedrock, or Minecraft 26.2 in
this delivery.
• No taming of cows or any mob other than vanilla mooshrooms.
• No collars, custom textures, models, animations, particles, sounds,
GUI, or configuration screen.
• No random taming chance, multi-step feeding, healing mechanic,
inventory, riding, combat, or owner defense.
• No automatic tame inheritance through breeding.
• No cross-dimensional teleport and no chunk force-loading.
• No ownership transfer or general player-facing untame feature.
• No attempt to neutralize hostile AI injected by unrelated third-party
mods.

10. Deliverables

• Complete Gradle project with wrapper, Fabric metadata, source, mixin
configuration if used, and reproducible build.
• Built JAR targeting Minecraft Java 26.1.2 and a clearly documented
compatible Fabric Loader/Fabric API range.
• README covering installation for single-player and dedicated server,
behavior summary, exact version support, and known non-goals.
• Automated tests plus a checked copy of the acceptance matrix or
equivalent test report.
• Source comments only where they explain version-sensitive hooks, state
invariants, or non-obvious compatibility choices.
• No generated caches, IDE files, secrets, or unrelated dependencies
committed.

11. Definition of Done

The agent must not declare completion until every statement below is
true:

• The project builds from a clean checkout using the included wrapper
and Java 25.
• The produced JAR launches successfully on Minecraft Java 26.1.2 with
the documented Fabric versions.
• All automated tests pass.
• Every A01–A22 acceptance scenario has been executed and passes.
• No unresolved ambiguity remains in the README or implementation.
• Golden Dandelion behavior has been tested both before and after
taming.
• Red/brown matching, persistence, lightning conversion, shearing
protection, and multiplayer ownership have all been explicitly
verified.
• A vanilla client can join the modded dedicated server and observe
correct behavior.

|**Stop condition:** If an exact 26.1.2 API name differs from this handoff, adapt the mechanism while preserving the behavior and acceptance tests. Do not silently change the target Minecraft version, weaken an acceptance criterion, add a dependency, or expand scope to other mobs.|
|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|

12. Agent Kickoff Prompt

Use the following instruction when handing this document to the
implementation agent:

Implement the Fabric mod described in this handoff for Minecraft Java
26.1.2. Treat Sections 2–5 as locked product requirements and A01–A22 as
the acceptance contract. Prefer Fabric events, use only narrow mixins
where required, preserve all vanilla interactions by default, and do not
retarget versions or add features outside the stated scope. Build, test,
and return the source project, JAR, README, and test results.

13. Authoritative References

> [Minecraft Java Edition 26.1 changelog — Golden Dandelion and
> baby-mob
> behavior](https://www.minecraft.net/en-us/article/minecraft-java-edition-26-1)
>
> [Fabric developer guides — environment setup, project creation,
> events, networking, and
> builds](https://docs.fabricmc.net/develop/)
