package net.erebusbat.tameablemooshrooms.mixin;

import net.erebusbat.tameablemooshrooms.behavior.FollowOwnerGoal;
import net.erebusbat.tameablemooshrooms.behavior.StayGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Registers the custom movement goals on mooshrooms. Only movement goals are
 * added; vanilla combat-free behavior is untouched and no targeting goals are
 * installed. Priorities are chosen so STAY suppresses voluntary wandering and
 * following preempts random strolling but yields to vanilla wheat temptation,
 * mirroring the vanilla goal ordering.
 */
@Mixin(MushroomCow.class)
public abstract class MushroomCowGoalsMixin extends Mob {
	protected MushroomCowGoalsMixin(final EntityType<? extends Mob> type, final Level level) {
		super(type, level);
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void tameableMooshrooms$registerTameGoals(final EntityType<? extends MushroomCow> type, final Level level, final CallbackInfo ci) {
		MushroomCow self = (MushroomCow)(Object)this;
		this.goalSelector.addGoal(1, new StayGoal(self));
		this.goalSelector.addGoal(4, new FollowOwnerGoal(self, 1.25));
	}
}
