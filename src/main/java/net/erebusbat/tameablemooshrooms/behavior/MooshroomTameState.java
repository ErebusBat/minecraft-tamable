package net.erebusbat.tameablemooshrooms.behavior;

import java.util.UUID;
import net.erebusbat.tameablemooshrooms.TameableMooshroomsMod;
import net.erebusbat.tameablemooshrooms.api.TameMode;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Single authoritative tame state for one mooshroom. Lives on the entity
 * instance and is serialized into the entity's NBT under a child compound
 * named with the mod ID, so every persisted key is namespaced.
 *
 * <p>Invariants:
 * <ul>
 *     <li>{@code tamed == true} requires a non-null owner UUID; partial or
 *     malformed records fail safe to untamed on load.</li>
 *     <li>Ownership is assigned exactly once by {@link #tryTame}; there is no
 *     retaming or transfer path.</li>
 *     <li>Newly tamed mooshrooms always start in {@link TameMode#FOLLOW}.</li>
 * </ul>
 */
public final class MooshroomTameState {
	/** Namespaced NBT compound key; all serialized fields live beneath it. */
	public static final String NBT_KEY = TameableMooshroomsMod.MOD_ID;
	public static final int SCHEMA_VERSION = 1;

	private static final String KEY_SCHEMA_VERSION = "schema_version";
	private static final String KEY_TAMED = "tamed";
	private static final String KEY_OWNER = "owner";
	private static final String KEY_MODE = "mode";

	private boolean tamed;
	private @Nullable UUID ownerUuid;
	private TameMode mode = TameMode.FOLLOW;

	public boolean isTamed() {
		return this.tamed;
	}

	public @Nullable UUID getOwnerUuid() {
		return this.ownerUuid;
	}

	public TameMode getMode() {
		return this.mode;
	}

	public void setMode(final TameMode mode) {
		this.mode = mode;
	}

	/**
	 * Atomically claims ownership. The server processes entity interactions on
	 * a single thread, so the check-and-set here is sufficient to guarantee
	 * that simultaneous tame attempts produce exactly one winner.
	 *
	 * @return true if this call tamed the mooshroom, false if it was already tamed
	 */
	public boolean tryTame(final UUID owner) {
		if (this.tamed) {
			return false;
		}

		this.tamed = true;
		this.ownerUuid = owner;
		this.mode = TameMode.FOLLOW;
		return true;
	}

	public void save(final ValueOutput output) {
		if (!this.tamed || this.ownerUuid == null) {
			return;
		}

		ValueOutput child = output.child(NBT_KEY);
		child.putInt(KEY_SCHEMA_VERSION, SCHEMA_VERSION);
		child.putBoolean(KEY_TAMED, true);
		child.store(KEY_OWNER, UUIDUtil.CODEC, this.ownerUuid);
		child.putString(KEY_MODE, this.mode.getSerializedName());
	}

	public void load(final ValueInput input) {
		this.tamed = false;
		this.ownerUuid = null;
		this.mode = TameMode.FOLLOW;

		input.child(NBT_KEY).ifPresent(child -> {
			boolean savedTamed = child.getBooleanOr(KEY_TAMED, false);
			// LENIENT_CODEC accepts both vanilla int-array and string UUID encodings.
			UUID savedOwner = child.read(KEY_OWNER, UUIDUtil.LENIENT_CODEC).orElse(null);
			if (savedTamed && savedOwner != null) {
				this.tamed = true;
				this.ownerUuid = savedOwner;
				this.mode = TameMode.byName(child.getStringOr(KEY_MODE, TameMode.FOLLOW.getSerializedName()));
			} else if (savedTamed) {
				// Fail safe: never recreate an ownerless immortal pet.
				TameableMooshroomsMod.LOGGER.warn("Discarding malformed tame state: tamed without a valid owner UUID");
			}
		});
	}
}
