package net.erebusbat.tameablemooshrooms.mixin;

import net.erebusbat.tameablemooshrooms.api.MooshroomTameAccess;
import net.erebusbat.tameablemooshrooms.behavior.MooshroomTameState;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Attaches the authoritative {@link MooshroomTameState} to every vanilla
 * mooshroom and persists it through the entity's normal save/load cycle
 * (world save, server restart, chunk unload/reload). Lightning conversion
 * toggles the variant in place in 26.1, so this state survives it unchanged;
 * bred offspring are fresh entities and therefore start untamed.
 */
@Mixin(MushroomCow.class)
public abstract class MushroomCowStateMixin implements MooshroomTameAccess {
	@Unique
	private final MooshroomTameState tameableMooshrooms$state = new MooshroomTameState();

	@Override
	public MooshroomTameState tameableMooshrooms$getState() {
		return this.tameableMooshrooms$state;
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void tameableMooshrooms$saveTameState(final ValueOutput output, final CallbackInfo ci) {
		this.tameableMooshrooms$state.save(output);
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void tameableMooshrooms$loadTameState(final ValueInput input, final CallbackInfo ci) {
		this.tameableMooshrooms$state.load(input);
	}
}
