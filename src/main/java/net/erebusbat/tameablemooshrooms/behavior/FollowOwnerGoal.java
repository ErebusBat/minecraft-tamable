package net.erebusbat.tameablemooshrooms.behavior;

import java.util.EnumSet;
import java.util.UUID;
import net.erebusbat.tameablemooshrooms.api.MooshroomTameAccess;
import net.erebusbat.tameablemooshrooms.api.TameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.jspecify.annotations.Nullable;

/**
 * FOLLOW mode: walks toward the owner when the owner is in the same dimension
 * and beyond the start radius, and stops near the owner instead of pushing
 * into them. Beyond the teleport radius the mooshroom attempts a safe
 * teleport near the owner, using the same destination checks as vanilla
 * tamed-animal teleports: walkable floor, collision-free hitbox space, and no
 * damaging blocks (lava, fire, powder snow, cactus) at the destination.
 *
 * <p>Following is movement only; this goal never acquires targets. It is
 * suspended while leashed, riding, being ridden, or in STAY mode, it never
 * crosses dimensions, and it idles when the owner is offline rather than
 * selecting a substitute.
 */
public class FollowOwnerGoal extends Goal {
	private static final double START_DISTANCE_SQR = 6.0 * 6.0;
	private static final double STOP_DISTANCE_SQR = 3.0 * 3.0;
	private static final double TELEPORT_DISTANCE_SQR = 12.0 * 12.0;
	private static final int TELEPORT_ATTEMPTS = 10;

	private final MushroomCow mob;
	private final MooshroomTameAccess access;
	private final PathNavigation navigation;
	private final double speedModifier;
	private @Nullable Player owner;
	private int timeToRecalcPath;
	private float oldWaterCost;

	public FollowOwnerGoal(final MushroomCow mob, final double speedModifier) {
		this.mob = mob;
		this.access = (MooshroomTameAccess)mob;
		this.navigation = mob.getNavigation();
		this.speedModifier = speedModifier;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	private @Nullable Player findOwner() {
		UUID ownerUuid = this.access.getOwnerUuid();
		// Lookup in the mooshroom's own level: same dimension only, no
		// cross-dimension teleport, no substitute owner when offline.
		return ownerUuid == null ? null : this.mob.level().getPlayerByUUID(ownerUuid);
	}

	private boolean unableToMoveToOwner() {
		return this.mob.isPassenger() || this.mob.isVehicle() || this.mob.mayBeLeashed();
	}

	@Override
	public boolean canUse() {
		if (!this.access.isTamed() || this.access.getMode() != TameMode.FOLLOW) {
			return false;
		}

		Player owner = this.findOwner();
		if (owner == null || owner.isSpectator()) {
			return false;
		}

		if (this.unableToMoveToOwner() || this.mob.distanceToSqr(owner) < START_DISTANCE_SQR) {
			return false;
		}

		this.owner = owner;
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		if (this.owner == null || !this.access.isTamed() || this.access.getMode() != TameMode.FOLLOW || this.unableToMoveToOwner()) {
			return false;
		}

		if (this.navigation.isDone()) {
			return false;
		}

		return this.mob.distanceToSqr(this.owner) > STOP_DISTANCE_SQR;
	}

	@Override
	public void start() {
		this.timeToRecalcPath = 0;
		this.oldWaterCost = this.mob.getPathfindingMalus(PathType.WATER);
		this.mob.setPathfindingMalus(PathType.WATER, 0.0F);
	}

	@Override
	public void stop() {
		this.owner = null;
		this.navigation.stop();
		this.mob.setPathfindingMalus(PathType.WATER, this.oldWaterCost);
	}

	@Override
	public void tick() {
		Player owner = this.owner;
		if (owner == null) {
			return;
		}

		this.mob.getLookControl().setLookAt(owner, 10.0F, this.mob.getMaxHeadXRot());
		if (--this.timeToRecalcPath > 0) {
			return;
		}

		this.timeToRecalcPath = this.adjustedTickDelay(10);
		if (this.mob.distanceToSqr(owner) >= TELEPORT_DISTANCE_SQR) {
			this.tryTeleportToOwner(owner);
		} else {
			this.navigation.moveTo(owner, this.speedModifier);
		}
	}

	private void tryTeleportToOwner(final Player owner) {
		BlockPos targetPos = owner.blockPosition();
		for (int attempt = 0; attempt < TELEPORT_ATTEMPTS; attempt++) {
			int xd = this.mob.getRandom().nextIntBetweenInclusive(-3, 3);
			int zd = this.mob.getRandom().nextIntBetweenInclusive(-3, 3);
			if (Math.abs(xd) < 2 && Math.abs(zd) < 2) {
				continue;
			}

			int yd = this.mob.getRandom().nextIntBetweenInclusive(-1, 1);
			if (this.maybeTeleportTo(targetPos.getX() + xd, targetPos.getY() + yd, targetPos.getZ() + zd)) {
				return;
			}
		}
	}

	private boolean maybeTeleportTo(final int x, final int y, final int z) {
		if (!this.canTeleportTo(new BlockPos(x, y, z))) {
			return false;
		}

		this.mob.snapTo(x + 0.5, y, z + 0.5, this.mob.getYRot(), this.mob.getXRot());
		this.navigation.stop();
		return true;
	}

	private boolean canTeleportTo(final BlockPos pos) {
		// WALKABLE already excludes destinations on or above lava, fire,
		// powder snow, and other damaging blocks, and requires a valid floor.
		if (WalkNodeEvaluator.getPathTypeStatic(this.mob, pos) != PathType.WALKABLE) {
			return false;
		}

		if (this.mob.level().getBlockState(pos.below()).getBlock() instanceof LeavesBlock) {
			return false;
		}

		BlockPos delta = pos.subtract(this.mob.blockPosition());
		return this.mob.level().noCollision(this.mob, this.mob.getBoundingBox().move(delta));
	}
}
