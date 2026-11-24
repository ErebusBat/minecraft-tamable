package net.erebusbat.tameablemooshrooms.behavior;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Minimal map-backed {@link ValueOutput}/{@link ValueInput} pair for testing
 * serialization logic outside the game. Only the methods exercised by
 * {@link MooshroomTameState} are implemented; anything else throws so an
 * unexpected code path fails loudly instead of silently passing.
 */
final class MapValueStorage {
	private MapValueStorage() {
	}

	static final class Output implements ValueOutput {
		final Map<String, Object> map = new HashMap<>();

		@Override
		public <T> void store(final String name, final Codec<T> codec, final T value) {
			this.map.put(name, value);
		}

		@Override
		public <T> void storeNullable(final String name, final Codec<T> codec, final @Nullable T value) {
			if (value != null) {
				this.map.put(name, value);
			}
		}

		@Override
		public void putBoolean(final String name, final boolean value) {
			this.map.put(name, value);
		}

		@Override
		public void putInt(final String name, final int value) {
			this.map.put(name, value);
		}

		@Override
		public void putString(final String name, final String value) {
			this.map.put(name, value);
		}

		@Override
		public ValueOutput child(final String name) {
			Output child = new Output();
			this.map.put(name, child);
			return child;
		}

		@Override
		public boolean isEmpty() {
			return this.map.isEmpty();
		}

		// --- unused by the state model ---

		@Override
		public <T> void store(final MapCodec<T> codec, final T value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void putByte(final String name, final byte value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void putShort(final String name, final short value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void putLong(final String name, final long value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void putFloat(final String name, final float value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void putDouble(final String name, final double value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void putIntArray(final String name, final int[] value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ValueOutputList childrenList(final String name) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> TypedOutputList<T> list(final String name, final Codec<T> codec) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void discard(final String name) {
			throw new UnsupportedOperationException();
		}
	}

	static final class Input implements ValueInput {
		private final Map<String, Object> map;

		Input(final Map<String, Object> map) {
			this.map = map;
		}

		static Input of(final Output output) {
			return new Input(output.map);
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> Optional<T> read(final String name, final Codec<T> codec) {
			return Optional.ofNullable((T)this.map.get(name));
		}

		@Override
		public Optional<ValueInput> child(final String name) {
			Object value = this.map.get(name);
			return value instanceof Output output ? Optional.of(new Input(output.map)) : Optional.empty();
		}

		@Override
		public boolean getBooleanOr(final String name, final boolean defaultValue) {
			return this.map.get(name) instanceof Boolean b ? b : defaultValue;
		}

		@Override
		public int getIntOr(final String name, final int defaultValue) {
			return this.map.get(name) instanceof Integer i ? i : defaultValue;
		}

		@Override
		public String getStringOr(final String name, final String defaultValue) {
			return this.map.get(name) instanceof String s ? s : defaultValue;
		}

		// --- unused by the state model ---

		@Override
		public <T> Optional<T> read(final MapCodec<T> codec) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ValueInput childOrEmpty(final String name) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Optional<ValueInputList> childrenList(final String name) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ValueInputList childrenListOrEmpty(final String name) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> Optional<TypedInputList<T>> list(final String name, final Codec<T> codec) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> TypedInputList<T> listOrEmpty(final String name, final Codec<T> codec) {
			throw new UnsupportedOperationException();
		}

		@Override
		public byte getByteOr(final String name, final byte defaultValue) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int getShortOr(final String name, final short defaultValue) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Optional<Integer> getInt(final String name) {
			throw new UnsupportedOperationException();
		}

		@Override
		public long getLongOr(final String name, final long defaultValue) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Optional<Long> getLong(final String name) {
			throw new UnsupportedOperationException();
		}

		@Override
		public float getFloatOr(final String name, final float defaultValue) {
			throw new UnsupportedOperationException();
		}

		@Override
		public double getDoubleOr(final String name, final double defaultValue) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Optional<String> getString(final String name) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Optional<int[]> getIntArray(final String name) {
			throw new UnsupportedOperationException();
		}

		@Override
		public HolderLookup.Provider lookup() {
			throw new UnsupportedOperationException();
		}
	}
}
