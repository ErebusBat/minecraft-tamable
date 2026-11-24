package net.erebusbat.tameablemooshrooms.api;

import java.util.UUID;
import net.erebusbat.tameablemooshrooms.behavior.MooshroomTameState;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/**
 * Access contract for custom tame state attached to every vanilla
 * {@link net.minecraft.world.entity.animal.cow.MushroomCow} instance by mixin.
 * The state object is the single authoritative representation; it is written
 * to and read from entity NBT under the mod's namespaced compound.
 */
public interface MooshroomTameAccess {
	MooshroomTameState tameableMooshrooms$getState();

	default boolean isTamed() {
		return this.tameableMooshrooms$getState().isTamed();
	}

	default @Nullable UUID getOwnerUuid() {
		return this.tameableMooshrooms$getState().getOwnerUuid();
	}

	default boolean isOwnedBy(final Player player) {
		UUID owner = this.getOwnerUuid();
		return owner != null && owner.equals(player.getUUID());
	}

	default TameMode getMode() {
		return this.tameableMooshrooms$getState().getMode();
	}
}
