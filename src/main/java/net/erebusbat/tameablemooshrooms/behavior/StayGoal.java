package net.erebusbat.tameablemooshrooms.behavior;

import java.util.EnumSet;
import net.erebusbat.tameablemooshrooms.api.MooshroomTameAccess;
import net.erebusbat.tameablemooshrooms.api.TameMode;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.cow.MushroomCow;

/**
 * STAY mode: cancels active navigation and prevents voluntary AI movement.
 * Registered above vanilla wandering/temptation goals and controls the MOVE
 * flag so no other movement goal can run while active. Environmental motion
 * (water flow, pistons, pushing) and the essential float goal are unaffected.
 */
public class StayGoal extends Goal {
	private final MushroomCow mob;
	private final MooshroomTameAccess access;

	public StayGoal(final MushroomCow mob) {
		this.mob = mob;
		this.access = (MooshroomTameAccess)mob;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		return this.access.isTamed() && this.access.getMode() == TameMode.STAY;
	}

	@Override
	public void start() {
		this.mob.getNavigation().stop();
	}

	@Override
	public void tick() {
		this.mob.getNavigation().stop();
	}
}
