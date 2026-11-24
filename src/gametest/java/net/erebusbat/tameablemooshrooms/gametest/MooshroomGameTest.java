package net.erebusbat.tameablemooshrooms.gametest;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.erebusbat.tameablemooshrooms.api.MooshroomTameAccess;
import net.erebusbat.tameablemooshrooms.api.TameMode;
import net.erebusbat.tameablemooshrooms.behavior.MooshroomInteractions;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/**
 * In-game acceptance tests: the real interaction handler, damage guard, and
 * state persistence run against real entities in a real server level.
 * Method javadocs map each test to the acceptance matrix in ACCEPTANCE.md.
 */
public class MooshroomGameTest {
	private static final InteractionHand HAND = InteractionHand.MAIN_HAND;

	private static Player mockPlayer(final ServerLevel level, final GameType gameType) {
		Player player = new Player(level, new GameProfile(UUID.randomUUID(), "test-mock-player")) {
			@Override
			public GameType gameMode() {
				return gameType;
			}

			@Override
			public boolean isClientAuthoritative() {
				return false;
			}
		};
		// Vanilla derives hasInfiniteMaterials() from this ability flag, not
		// from gameMode() alone, so mirror what a real creative player has.
		player.getAbilities().instabuild = gameType == GameType.CREATIVE;
		return player;
	}

	private static MooshroomTameAccess access(final MushroomCow cow) {
		return (MooshroomTameAccess)cow;
	}

	private static Player tame(final GameTestHelper helper, final MushroomCow cow, final GameType gameType, final net.minecraft.world.item.Item item) {
		Player player = mockPlayer((ServerLevel)helper.getLevel(), gameType);
		player.setItemInHand(HAND, new ItemStack(item, 2));
		InteractionResult result = MooshroomInteractions.interact(player, cow.level(), HAND, cow, null);
		if (result != InteractionResult.SUCCESS || !access(cow).isTamed()) {
			helper.fail("Expected successful taming but got " + result);
		}

		return player;
	}

	/** A01: red mooshroom + red mushroom tames, consumes exactly one, owner set, mode FOLLOW. */
	@GameTest
	public void redTamesWithRedMushroom(final GameTestHelper helper) {
		MushroomCow cow = helper.spawn(EntityType.MOOSHROOM, 1, 1, 1);
		Player player = tame(helper, cow, GameType.SURVIVAL, Items.RED_MUSHROOM);

		if (!access(cow).isOwnedBy(player)) {
			helper.fail("Owner UUID was not set to the taming player");
		}

		if (access(cow).getMode() != TameMode.FOLLOW) {
			helper.fail("Newly tamed mooshroom must start in FOLLOW mode");
		}

		if (player.getItemInHand(HAND).getCount() != 1) {
			helper.fail("Exactly one mushroom must be consumed in survival");
		}

		helper.succeed();
	}

	/** A03: brown mooshroom + brown mushroom tames (after lightning conversion). */
	@GameTest
	public void brownTamesWithBrownMushroom(final GameTestHelper helper) {
		MushroomCow cow = helper.spawn(EntityType.MOOSHROOM, 1, 1, 1);
		cow.thunderHit((ServerLevel)helper.getLevel(), new LightningBolt(EntityType.LIGHTNING_BOLT, helper.getLevel()));
		tame(helper, cow, GameType.SURVIVAL, Items.BROWN_MUSHROOM);
		helper.succeed();
	}

	/** A02: red mooshroom + brown mushroom passes, no state change, nothing consumed. */
	@GameTest
	public void redRejectsBrownMushroom(final GameTestHelper helper) {
		MushroomCow cow = helper.spawnWithNoFreeWill(EntityType.MOOSHROOM, 1, 1, 1);
		assertWrongMushroomRejected(helper, cow, Items.BROWN_MUSHROOM);
	}

	/** A04: brown mooshroom + red mushroom passes, no state change, nothing consumed. */
	@GameTest
	public void brownRejectsRedMushroom(final GameTestHelper helper) {
		MushroomCow cow = helper.spawnWithNoFreeWill(EntityType.MOOSHROOM, 1, 1, 1);
		cow.thunderHit((ServerLevel)helper.getLevel(), new LightningBolt(EntityType.LIGHTNING_BOLT, helper.getLevel()));
		assertWrongMushroomRejected(helper, cow, Items.RED_MUSHROOM);
	}

	private static void assertWrongMushroomRejected(final GameTestHelper helper, final MushroomCow cow, final net.minecraft.world.item.Item wrongItem) {
		Player player = mockPlayer((ServerLevel)helper.getLevel(), GameType.SURVIVAL);
		player.setItemInHand(HAND, new ItemStack(wrongItem, 2));

		InteractionResult result = MooshroomInteractions.interact(player, cow.level(), HAND, cow, null);
		if (result != InteractionResult.PASS || access(cow).isTamed() || player.getItemInHand(HAND).getCount() != 2) {
			helper.fail("Wrong-colored mushroom must pass without state change or consumption");
		}

		helper.succeed();
	}

	/** A05: creative taming consumes nothing. */
	@GameTest
	public void creativeTamingConsumesNothing(final GameTestHelper helper) {
		MushroomCow cow = helper.spawn(EntityType.MOOSHROOM, 1, 1, 1);
		Player player = tame(helper, cow, GameType.CREATIVE, Items.RED_MUSHROOM);

		if (player.getItemInHand(HAND).getCount() != 2) {
			helper.fail("Creative taming must not consume the mushroom");
		}

		helper.succeed();
	}

	/** A06: spectators cannot tame and consume nothing. */
	@GameTest
	public void spectatorCannotTame(final GameTestHelper helper) {
		MushroomCow cow = helper.spawn(EntityType.MOOSHROOM, 1, 1, 1);
		Player spectator = mockPlayer((ServerLevel)helper.getLevel(), GameType.SPECTATOR);
		spectator.setItemInHand(HAND, new ItemStack(Items.RED_MUSHROOM, 2));

		InteractionResult result = MooshroomInteractions.interact(spectator, cow.level(), HAND, cow, null);
		if (result != InteractionResult.PASS || access(cow).isTamed() || spectator.getItemInHand(HAND).getCount() != 2) {
			helper.fail("Spectator interaction must not tame or consume");
		}

		helper.succeed();
	}

	/** A07: a second player cannot retame or take ownership. */
	@GameTest
	public void secondPlayerCannotRetame(final GameTestHelper helper) {
		MushroomCow cow = helper.spawn(EntityType.MOOSHROOM, 1, 1, 1);
		Player owner = tame(helper, cow, GameType.SURVIVAL, Items.RED_MUSHROOM);

		Player intruder = mockPlayer((ServerLevel)helper.getLevel(), GameType.SURVIVAL);
		intruder.setItemInHand(HAND, new ItemStack(Items.RED_MUSHROOM, 2));
		InteractionResult result = MooshroomInteractions.interact(intruder, cow.level(), HAND, cow, null);

		if (result != InteractionResult.PASS || !access(cow).isOwnedBy(owner) || intruder.getItemInHand(HAND).getCount() != 2) {
			helper.fail("Retaming must pass, keep the original owner, and consume nothing");
		}

		helper.succeed();
	}

	/** A08: the owner toggles FOLLOW to STAY and back with an empty hand. */
	@GameTest
	public void ownerTogglesFollowStay(final GameTestHelper helper) {
		MushroomCow cow = helper.spawn(EntityType.MOOSHROOM, 1, 1, 1);
		Player owner = tame(helper, cow, GameType.SURVIVAL, Items.RED_MUSHROOM);
		owner.setItemInHand(HAND, ItemStack.EMPTY);

		if (MooshroomInteractions.interact(owner, cow.level(), HAND, cow, null) != InteractionResult.SUCCESS
			|| access(cow).getMode() != TameMode.STAY) {
			helper.fail("First empty-hand use must switch to STAY");
		}

		if (MooshroomInteractions.interact(owner, cow.level(), HAND, cow, null) != InteractionResult.SUCCESS
			|| access(cow).getMode() != TameMode.FOLLOW) {
			helper.fail("Second empty-hand use must switch back to FOLLOW");
		}

		helper.succeed();
	}

	/** A09: a non-owner's empty hand changes nothing. */
	@GameTest
	public void nonOwnerCannotToggleMode(final GameTestHelper helper) {
		MushroomCow cow = helper.spawn(EntityType.MOOSHROOM, 1, 1, 1);
		tame(helper, cow, GameType.SURVIVAL, Items.RED_MUSHROOM);

		Player stranger = mockPlayer((ServerLevel)helper.getLevel(), GameType.SURVIVAL);
		stranger.setItemInHand(HAND, ItemStack.EMPTY);
		InteractionResult result = MooshroomInteractions.interact(stranger, cow.level(), HAND, cow, null);

		if (result != InteractionResult.PASS || access(cow).getMode() != TameMode.FOLLOW) {
			helper.fail("Non-owner empty-hand use must pass without changing the mode");
		}

		helper.succeed();
	}

	/** A10: gameplay damage is canceled for tamed mooshrooms. */
	@GameTest
	public void tamedMooshroomIsInvulnerable(final GameTestHelper helper) {
		MushroomCow cow = helper.spawn(EntityType.MOOSHROOM, 1, 1, 1);
		tame(helper, cow, GameType.SURVIVAL, Items.RED_MUSHROOM);

		boolean damaged = cow.hurtServer((ServerLevel)helper.getLevel(), cow.damageSources().generic(), 5.0F);
		if (damaged || cow.getHealth() != cow.getMaxHealth()) {
			helper.fail("Tamed mooshroom must not take gameplay damage");
		}

		helper.succeed();
	}

	/** A11: untamed mooshrooms keep vanilla damage behavior. */
	@GameTest
	public void untamedMooshroomTakesVanillaDamage(final GameTestHelper helper) {
		MushroomCow cow = helper.spawn(EntityType.MOOSHROOM, 1, 1, 1);

		boolean damaged = cow.hurtServer((ServerLevel)helper.getLevel(), cow.damageSources().generic(), 5.0F);
		if (!damaged || cow.getHealth() != cow.getMaxHealth() - 5.0F) {
			helper.fail("Untamed mooshroom must take vanilla damage");
		}

		helper.succeed();
	}

	/** A12/A13 + handoff 4.5: Golden Dandelion passes through our handler and vanilla age lock still toggles. */
	@GameTest(maxTicks = 100)
	public void goldenDandelionBehaviorStaysVanilla(final GameTestHelper helper) {
		MushroomCow cow = helper.spawn(EntityType.MOOSHROOM, 1, 1, 1);
		cow.setBaby(true);
		tame(helper, cow, GameType.SURVIVAL, Items.RED_MUSHROOM);

		Player player = mockPlayer((ServerLevel)helper.getLevel(), GameType.SURVIVAL);
		player.setItemInHand(HAND, new ItemStack(Items.GOLDEN_DANDELION, 2));

		// Our handler must pass without mutation.
		if (MooshroomInteractions.interact(player, cow.level(), HAND, cow, null) != InteractionResult.PASS
			|| !access(cow).isTamed() || player.getItemInHand(HAND).getCount() != 2) {
			helper.fail("Golden Dandelion must pass through the custom handler untouched");
		}

		// Vanilla then handles it exactly as unmodified vanilla would.
		cow.mobInteract(player, HAND);
		if (!cow.isAgeLocked() || player.getItemInHand(HAND).getCount() != 1) {
			helper.fail("Vanilla age lock must engage and consume as usual");
		}

		// Vanilla enforces a 40-tick cooldown before the dandelion can be reused.
		helper.runAfterDelay(41, () -> {
			cow.mobInteract(player, HAND);
			if (cow.isAgeLocked()) {
				helper.fail("Second Golden Dandelion must resume vanilla aging");
			}

			if (!access(cow).isTamed()) {
				helper.fail("Custom tame state must be untouched by Golden Dandelion use");
			}

			helper.succeed();
		});
	}

	/** A15: lightning flips the variant but keeps owner, tame state, and mode. */
	@GameTest
	public void lightningConversionPreservesTameState(final GameTestHelper helper) {
		MushroomCow cow = helper.spawn(EntityType.MOOSHROOM, 1, 1, 1);
		Player owner = tame(helper, cow, GameType.SURVIVAL, Items.RED_MUSHROOM);
		access(cow).tameableMooshrooms$getState().setMode(TameMode.STAY);

		cow.thunderHit((ServerLevel)helper.getLevel(), new LightningBolt(EntityType.LIGHTNING_BOLT, helper.getLevel()));

		if (cow.getVariant() != MushroomCow.Variant.BROWN || !access(cow).isOwnedBy(owner) || access(cow).getMode() != TameMode.STAY) {
			helper.fail("Lightning conversion must keep variant change but preserve tame state");
		}

		helper.succeed();
	}

	/** A16: bred offspring are born untamed and vulnerable. */
	@GameTest
	public void bredOffspringIsUntamed(final GameTestHelper helper) {
		MushroomCow parent = helper.spawn(EntityType.MOOSHROOM, 1, 1, 1);
		tame(helper, parent, GameType.SURVIVAL, Items.RED_MUSHROOM);

		MushroomCow child = parent.getBreedOffspring((ServerLevel)helper.getLevel(), parent);
		if (child == null || access(child).isTamed()) {
			helper.fail("Bred offspring must start untamed");
		}

		if (!child.hurtServer((ServerLevel)helper.getLevel(), child.damageSources().generic(), 1.0F)) {
			helper.fail("Bred offspring must be vulnerable");
		}

		helper.succeed();
	}

	/** A17: shears on a tamed mooshroom are blocked with no durability loss. */
	@GameTest
	public void shearsBlockedOnTamed(final GameTestHelper helper) {
		MushroomCow cow = helper.spawn(EntityType.MOOSHROOM, 1, 1, 1);
		Player owner = tame(helper, cow, GameType.SURVIVAL, Items.RED_MUSHROOM);

		ItemStack shears = new ItemStack(Items.SHEARS);
		owner.setItemInHand(HAND, shears);
		InteractionResult result = MooshroomInteractions.interact(owner, cow.level(), HAND, cow, null);

		if (result != InteractionResult.FAIL || shears.getDamageValue() != 0) {
			helper.fail("Shearing a tamed mooshroom must fail without durability loss");
		}

		helper.assertEntityInstancePresent(cow, 1, 1, 1);
		helper.succeed();
	}

	/** A18/A19: vanilla interactions pass through untouched on tamed and untamed mooshrooms. */
	@GameTest
	public void vanillaInteractionsPassThrough(final GameTestHelper helper) {
		MushroomCow untamed = helper.spawnWithNoFreeWill(EntityType.MOOSHROOM, 0, 1, 0);
		MushroomCow tamed = helper.spawnWithNoFreeWill(EntityType.MOOSHROOM, 2, 1, 0);
		Player owner = tame(helper, tamed, GameType.SURVIVAL, Items.RED_MUSHROOM);

		net.minecraft.world.item.Item[] vanillaItems = {
			Items.SHEARS, Items.BOWL, Items.BUCKET, Items.WHEAT, Items.LEAD, Items.NAME_TAG, Items.POPPY
		};

		for (net.minecraft.world.item.Item item : vanillaItems) {
			owner.setItemInHand(HAND, new ItemStack(item));
			if (MooshroomInteractions.interact(owner, untamed.level(), HAND, untamed, null) != InteractionResult.PASS) {
				helper.fail("Untamed mooshroom must pass " + item + " to vanilla");
			}

			if (item != Items.SHEARS
				&& MooshroomInteractions.interact(owner, tamed.level(), HAND, tamed, null) != InteractionResult.PASS) {
				helper.fail("Tamed mooshroom must pass " + item + " to vanilla");
			}
		}

		if (access(untamed).isTamed() || !access(tamed).isTamed()) {
			helper.fail("Vanilla pass-through items must not change tame state");
		}

		helper.succeed();
	}
}
