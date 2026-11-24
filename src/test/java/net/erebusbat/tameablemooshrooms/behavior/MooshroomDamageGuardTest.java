package net.erebusbat.tameablemooshrooms.behavior;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Damage predicate (handoff 8.1): only tamed mooshrooms are protected, and
 * administrative removal via the generic-kill damage type (/kill) stays
 * available.
 */
class MooshroomDamageGuardTest {
	@Test
	void gameplayDamageIsBlockedForTamedMooshrooms() {
		assertFalse(MooshroomDamageGuard.allowDamage(true, false));
	}

	@Test
	void untamedMooshroomsKeepVanillaDamageBehavior() {
		assertTrue(MooshroomDamageGuard.allowDamage(false, false));
		assertTrue(MooshroomDamageGuard.allowDamage(false, true));
	}

	@Test
	void adminRemovalRemainsAvailableForTamedMooshrooms() {
		assertTrue(MooshroomDamageGuard.allowDamage(true, true));
	}
}
