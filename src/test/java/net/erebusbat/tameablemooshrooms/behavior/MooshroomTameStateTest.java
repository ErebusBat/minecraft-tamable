package net.erebusbat.tameablemooshrooms.behavior;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.erebusbat.tameablemooshrooms.api.TameMode;
import org.junit.jupiter.api.Test;

/**
 * Tame transaction, race/idempotency, serialization round trip, fail-safe
 * loading, and breeding defaults (handoff 8.1).
 */
class MooshroomTameStateTest {
	@Test
	void freshStateIsUntamedAndFollowing() {
		// A bred child is a fresh entity with fresh state: born untamed.
		MooshroomTameState state = new MooshroomTameState();
		assertFalse(state.isTamed());
		assertNull(state.getOwnerUuid());
		assertEquals(TameMode.FOLLOW, state.getMode());
	}

	@Test
	void tameTransactionSetsOwnerAndFollowMode() {
		MooshroomTameState state = new MooshroomTameState();
		UUID owner = UUID.randomUUID();

		assertTrue(state.tryTame(owner));
		assertTrue(state.isTamed());
		assertEquals(owner, state.getOwnerUuid());
		assertEquals(TameMode.FOLLOW, state.getMode());
	}

	@Test
	void repeatedTameAttemptsCannotTransferOwnership() {
		MooshroomTameState state = new MooshroomTameState();
		UUID first = UUID.randomUUID();
		UUID second = UUID.randomUUID();

		assertTrue(state.tryTame(first));
		// Race/idempotency: simultaneous or repeated attempts never win twice.
		assertFalse(state.tryTame(second));
		assertFalse(state.tryTame(first));

		assertEquals(first, state.getOwnerUuid());
	}

	@Test
	void modeTogglesBetweenFollowAndStay() {
		MooshroomTameState state = new MooshroomTameState();
		state.tryTame(UUID.randomUUID());

		state.setMode(state.getMode().toggle());
		assertEquals(TameMode.STAY, state.getMode());
		state.setMode(state.getMode().toggle());
		assertEquals(TameMode.FOLLOW, state.getMode());
	}

	@Test
	void serializationRoundTripPreservesAllFields() {
		MooshroomTameState original = new MooshroomTameState();
		UUID owner = UUID.randomUUID();
		original.tryTame(owner);
		original.setMode(TameMode.STAY);

		MapValueStorage.Output output = new MapValueStorage.Output();
		original.save(output);

		MooshroomTameState restored = new MooshroomTameState();
		restored.load(MapValueStorage.Input.of(output));

		assertTrue(restored.isTamed());
		assertEquals(owner, restored.getOwnerUuid());
		assertEquals(TameMode.STAY, restored.getMode());
	}

	@Test
	void untamedStateWritesNothingAndLoadsAsDefault() {
		MapValueStorage.Output output = new MapValueStorage.Output();
		new MooshroomTameState().save(output);
		assertTrue(output.isEmpty());

		MooshroomTameState restored = new MooshroomTameState();
		restored.load(MapValueStorage.Input.of(output));
		assertFalse(restored.isTamed());
		assertNull(restored.getOwnerUuid());
		assertEquals(TameMode.FOLLOW, restored.getMode());
	}

	@Test
	void tamedRecordWithoutOwnerFailsSafeToUntamed() {
		// Malformed partial state: tamed=true but no valid owner UUID must
		// never recreate an ownerless immortal pet.
		MapValueStorage.Output output = new MapValueStorage.Output();
		var child = output.child(MooshroomTameState.NBT_KEY);
		child.putInt("schema_version", MooshroomTameState.SCHEMA_VERSION);
		child.putBoolean("tamed", true);

		MooshroomTameState restored = new MooshroomTameState();
		restored.load(MapValueStorage.Input.of(output));

		assertFalse(restored.isTamed());
		assertNull(restored.getOwnerUuid());
		assertEquals(TameMode.FOLLOW, restored.getMode());
	}

	@Test
	void unknownModeFallsBackToFollow() {
		MapValueStorage.Output output = new MapValueStorage.Output();
		var child = output.child(MooshroomTameState.NBT_KEY);
		child.putInt("schema_version", MooshroomTameState.SCHEMA_VERSION);
		child.putBoolean("tamed", true);
		child.store("owner", net.minecraft.core.UUIDUtil.CODEC, UUID.randomUUID());
		child.putString("mode", "some_future_mode");

		MooshroomTameState restored = new MooshroomTameState();
		restored.load(MapValueStorage.Input.of(output));

		assertTrue(restored.isTamed());
		assertEquals(TameMode.FOLLOW, restored.getMode());
	}

	@Test
	void schemaVersionIsWritten() {
		MooshroomTameState state = new MooshroomTameState();
		state.tryTame(UUID.randomUUID());

		MapValueStorage.Output output = new MapValueStorage.Output();
		state.save(output);

		var child = (MapValueStorage.Output)output.map.get(MooshroomTameState.NBT_KEY);
		assertEquals(MooshroomTameState.SCHEMA_VERSION, child.map.get("schema_version"));
	}
}
