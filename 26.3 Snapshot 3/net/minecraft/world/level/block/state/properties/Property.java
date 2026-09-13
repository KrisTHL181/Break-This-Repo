package net.minecraft.world.level.block.state.properties;

import com.google.common.base.MoreObjects;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.world.level.block.state.StateHolder;
import org.jspecify.annotations.Nullable;

public abstract class Property<T extends Comparable<T>> {
   private final Class<T> clazz;
   private final String name;
   private @Nullable Integer hashCode;
   private final Codec<T> codec = Codec.STRING
      .comapFlatMap(
         namex -> this.getValue(namex)
            .<DataResult>map(DataResult::success)
            .orElseGet(() -> DataResult.error(() -> "Unable to read property: " + this + " with value: " + namex)),
         this::getName
      );
   private final Codec<Property.Value<T>> valueCodec = this.codec.xmap(this::value, Property.Value::value);

   protected Property(final String name, final Class<T> clazz) {
      this.clazz = clazz;
      this.name = name;
   }

   public Property.Value<T> value(final T value) {
      return new Property.Value<>(this, value);
   }

   public Property.Value<T> value(final StateHolder<?, ?> stateHolder) {
      return new Property.Value<>(this, stateHolder.getValue(this));
   }

   public Stream<Property.Value<T>> getAllValues() {
      return this.getPossibleValues().stream().map(this::value);
   }

   public Codec<T> codec() {
      return this.codec;
   }

   public Codec<Property.Value<T>> valueCodec() {
      return this.valueCodec;
   }

   public String getName() {
      return this.name;
   }

   public Class<T> getValueClass() {
      return this.clazz;
   }

   public abstract List<T> getPossibleValues();

   public abstract String getName(final T value);

   public abstract Optional<T> getValue(final String name);

   public abstract int getInternalIndex(final T value);

   @Override
   public String toString() {
      return MoreObjects.toStringHelper(this).add("name", this.name).add("clazz", this.clazz).add("values", this.getPossibleValues()).toString();
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      } else {
         return !(o instanceof Property<?> that) ? false : this.clazz.equals(that.clazz) && this.name.equals(that.name);
      }
   }

   @Override
   public final int hashCode() {
      if (this.hashCode == null) {
         this.hashCode = this.generateHashCode();
      }

      return this.hashCode;
   }

   public int generateHashCode() {
      return 31 * this.clazz.hashCode() + this.name.hashCode();
   }

   public <U, S extends StateHolder<?, S>> DataResult<S> parseValue(final DynamicOps<U> ops, final S state, final U value) {
      DataResult<T> parsed = this.codec.parse(ops, value);
      return parsed.map(v -> state.setValue(this, v)).setPartial(state);
   }

   public record Value<T extends Comparable<T>>(Property<T> property, T value) {
      public Value {
         if (!property.getPossibleValues().contains(value)) {
            throw new IllegalArgumentException("Value " + value + " does not belong to property " + property);
         }
      }

      @Override
      public String toString() {
         return this.property.getName() + "=" + this.property.getName(this.value);
      }

      public String valueName() {
         return this.property.getName(this.value);
      }
   }
}
