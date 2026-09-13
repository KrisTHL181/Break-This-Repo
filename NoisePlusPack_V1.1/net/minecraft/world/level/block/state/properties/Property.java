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
         p_61698_ -> this.getValue(p_61698_)
            .<DataResult>map(DataResult::success)
            .orElseGet(() -> DataResult.error(() -> "Unable to read property: " + this + " with value: " + p_61698_)),
         this::getName
      );
   private final Codec<Property.Value<T>> valueCodec = this.codec.xmap(this::value, Property.Value::value);

   protected Property(String p_61692_, Class<T> p_61693_) {
      this.clazz = p_61693_;
      this.name = p_61692_;
   }

   public Property.Value<T> value(T p_61700_) {
      return new Property.Value<>(this, p_61700_);
   }

   public Property.Value<T> value(StateHolder<?, ?> p_61695_) {
      return new Property.Value<>(this, p_61695_.getValue(this));
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

   public abstract String getName(T var1);

   public abstract Optional<T> getValue(String var1);

   public abstract int getInternalIndex(T var1);

   @Override
   public String toString() {
      return MoreObjects.toStringHelper(this).add("name", this.name).add("clazz", this.clazz).add("values", this.getPossibleValues()).toString();
   }

   @Override
   public boolean equals(Object p_61707_) {
      if (this == p_61707_) {
         return true;
      } else {
         return p_61707_ instanceof Property<?> property ? this.clazz.equals(property.clazz) && this.name.equals(property.name) : false;
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

   public <U, S extends StateHolder<?, S>> DataResult<S> parseValue(DynamicOps<U> p_156032_, S p_156033_, U p_156034_) {
      DataResult<T> dataresult = this.codec.parse(p_156032_, p_156034_);
      return dataresult.map(p_156030_ -> p_156033_.setValue(this, p_156030_)).setPartial(p_156033_);
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
   }
}
