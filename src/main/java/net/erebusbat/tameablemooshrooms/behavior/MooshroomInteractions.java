package net.erebusbat.tameablemooshrooms.behavior;

import net.erebusbat.tameablemooshrooms.api.MooshroomTameAccess;
import net.erebusbat.tameablemooshrooms.api.TameMode;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.jspecify.annotations.Nullable;

/**
 * Player-interaction contract for mooshrooms. Every case this mod does not
 * own returns {@link InteractionResult#PASS} so vanilla logic (milking, stew,
 * breeding, leads, name tags, Golden Dandelion age-lock) and other mods keep
 * their opportunity to handle the interaction. All mutation happens on the
 * logical server; the client only mirrors success for the arm swing.
 */
public final class MooshroomInteractions {
	/** Vanilla entity status handled by {@code Animal.handleEntityEvent}: heart particles. */
	private static final byte HEARTS_STATUS = 18;

	private MooshroomInteractions() {
	}

	public static InteractionResult interact(
		final Player player, final Level level, final InteractionHand hand, final Entity entity, final @Nullable EntityHitResult hitResult
	) {
		if (!(entity instanceof MushroomCow cow)) {
			return InteractionResult.PASS;
		}

		if (player.isSpectator()) {
			return InteractionResult.PASS;
		}

		MooshroomTameAccess access = (MooshroomTameAccess)cow;
		ItemStack stack = player.getItemInHand(hand);

		if (!access.isTamed() && TamingRules.isMatchingMushroom(cow.getVariant().getSerializedName(), stack)) {
			return tame(player, level, cow, access, stack);
		}

		if (access.isTamed() && stack.is(Items.SHEARS)) {
			return blockShearing(player, level);
		}

		if (access.isTamed() && stack.isEmpty() && access.isOwnedBy(player)) {
			return toggleMode(player, level, cow, access);
		}

		// Everything else is not ours: wrong-colored mushroom, mushrooms on an
		// already tamed mooshroom, non-owner empty hand, bowls, buckets, wheat,
		// leads, name tags, flowers, and Golden Dandelions all pass through.
		return InteractionResult.PASS;
	}

	private static InteractionResult tame(
		final Player player, final Level level, final MushroomCow cow, final MooshroomTameAccess access, final ItemStack stack
	) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		// Interactions are processed on the server thread, so this re-check
		// makes simultaneous attempts produce exactly one winner and consume
		// at most one mushroom.
		if (!access.tameableMooshrooms$getState().tryTame(player.getUUID())) {
			return InteractionResult.PASS;
		}

		// consume respects creative mode (hasInfiniteMaterials) like vanilla.
		stack.consume(1, player);
		cow.setPersistenceRequired();
		level.broadcastEntityEvent(cow, HEARTS_STATUS);
		cow.playSound(SoundEvents.MOOSHROOM_EAT, 1.0F, 1.0F);
		return InteractionResult.SUCCESS;
	}

	private static InteractionResult blockShearing(final Player player, final Level level) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		// Shearing a tamed mooshroom would convert it into an untamed cow and
		// silently discard the pet; block it before vanilla runs, with no
		// durability loss, and tell the owner why.
		player.sendOverlayMessage(
			Component.translatableWithFallback("message.tameable_mooshrooms.shear_blocked", "This mooshroom is tamed and cannot be sheared.")
		);
		return InteractionResult.FAIL;
	}

	private static InteractionResult toggleMode(final Player player, final Level level, final MushroomCow cow, final MooshroomTameAccess access) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		TameMode mode = access.getMode().toggle();
		access.tameableMooshrooms$getState().setMode(mode);
		cow.getNavigation().stop();
		player.sendOverlayMessage(
			Component.translatableWithFallback(
				mode == TameMode.FOLLOW ? "message.tameable_mooshrooms.mode.following" : "message.tameable_mooshrooms.mode.staying",
				mode == TameMode.FOLLOW ? "Your mooshroom will follow you." : "Your mooshroom will stay here."
			)
		);
		return InteractionResult.SUCCESS;
	}
}
