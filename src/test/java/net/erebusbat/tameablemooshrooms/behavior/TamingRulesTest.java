package net.erebusbat.tameablemooshrooms.behavior;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

/**
 * Variant-to-item matching (handoff 8.1): red accepts only red mushroom,
 * brown accepts only brown mushroom, and a Golden Dandelion is never a
 * taming item.
 */
class TamingRulesTest {
	@Test
	void redMooshroomTamesOnlyWithRedMushroom() {
		assertEquals(Identifier.withDefaultNamespace("red_mushroom"), TamingRules.tamingItemIdFor("red"));
		assertNotEquals(Identifier.withDefaultNamespace("brown_mushroom"), TamingRules.tamingItemIdFor("red"));
	}

	@Test
	void brownMooshroomTamesOnlyWithBrownMushroom() {
		assertEquals(Identifier.withDefaultNamespace("brown_mushroom"), TamingRules.tamingItemIdFor("brown"));
		assertNotEquals(Identifier.withDefaultNamespace("red_mushroom"), TamingRules.tamingItemIdFor("brown"));
	}

	@Test
	void goldenDandelionIsNeverATamingItem() {
		assertNotEquals(TamingRules.GOLDEN_DANDELION_ITEM, TamingRules.tamingItemIdFor("red"));
		assertNotEquals(TamingRules.GOLDEN_DANDELION_ITEM, TamingRules.tamingItemIdFor("brown"));
	}
}
