package net.erebusbat.tameablemooshrooms.api;

import org.jspecify.annotations.Nullable;

/**
 * Movement mode of a tamed mooshroom. Serialized by name inside the mod's
 * namespaced NBT compound; unknown future values fall back to FOLLOW.
 */
public enum TameMode {
	FOLLOW("follow"),
	STAY("stay");

	private final String serializedName;

	TameMode(final String serializedName) {
		this.serializedName = serializedName;
	}

	public String getSerializedName() {
		return this.serializedName;
	}

	public TameMode toggle() {
		return this == FOLLOW ? STAY : FOLLOW;
	}

	public static TameMode byName(final @Nullable String name) {
		for (TameMode mode : values()) {
			if (mode.serializedName.equals(name)) {
				return mode;
			}
		}

		return FOLLOW;
	}
}
