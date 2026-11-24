package net.erebusbat.tameablemooshrooms.behavior;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Deterministic variant-to-item matching: a red mooshroom is tamed only by
 * {@code minecraft:red_mushroom}, a brown mooshroom only by
 * {@code minecraft:brown_mushroom}. Golden Dandelions are never taming items.
 *
 * <p>Matching is keyed by the variant's serialized name and resolved through
 * identifiers (rather than {@code Items} constants) so the rule table is
 * verifiable without bootstrapping the game registries.
 */
public final class TamingRules {
	public static final Identifier RED_TAMING_ITEM = Identifier.withDefaultNamespace("red_mushroom");
	public static final Identifier BROWN_TAMING_ITEM = Identifier.withDefaultNamespace("brown_mushroom");
	public static final Identifier GOLDEN_DANDELION_ITEM = Identifier.withDefaultNamespace("golden_dandelion");

	private TamingRules() {
	}

	/** @param variantName {@code MushroomCow.Variant#getSerializedName()} ("red" or "brown") */
	public static Identifier tamingItemIdFor(final String variantName) {
		return switch (variantName) {
			case "red" -> RED_TAMING_ITEM;
			case "brown" -> BROWN_TAMING_ITEM;
			default -> throw new IllegalArgumentException("Unknown mooshroom variant: " + variantName);
		};
	}

	public static boolean isMatchingMushroom(final String variantName, final ItemStack stack) {
		return stack.is(BuiltInRegistries.ITEM.getValue(tamingItemIdFor(variantName)));
	}
}
