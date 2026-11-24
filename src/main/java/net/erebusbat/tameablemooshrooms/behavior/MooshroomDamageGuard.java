package net.erebusbat.tameablemooshrooms.behavior;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.erebusbat.tameablemooshrooms.api.MooshroomTameAccess;

/**
 * Gameplay invulnerability for tamed mooshrooms, enforced through
 * {@code ServerLivingEntityEvents.ALLOW_DAMAGE}, which fires inside
 * {@code LivingEntity.hurtServer} before armor, hurt animation, knockback,
 * loot, or death processing. Canceling there means no visible damage reaction
 * and no health change for any gameplay source (attacks, projectiles,
 * explosions, fire, lava, fall, drowning, suffocation, freezing, poison,
 * wither, cactus, lightning, and the void).
 *
 * <p>The generic-kill damage type used by {@code /kill} is deliberately
 * allowed: invulnerability is gameplay damage immunity, not undeletability.
 */
public final class MooshroomDamageGuard {
	private MooshroomDamageGuard() {
	}

	public static boolean allowDamage(final LivingEntity entity, final DamageSource source, final float amount) {
		if (entity instanceof MushroomCow cow) {
			return allowDamage(((MooshroomTameAccess)cow).isTamed(), source.is(DamageTypes.GENERIC_KILL));
		}

		return true;
	}

	static boolean allowDamage(final boolean tamed, final boolean genericKill) {
		return !tamed || genericKill;
	}
}
