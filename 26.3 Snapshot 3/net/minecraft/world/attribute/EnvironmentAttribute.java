package net.minecraft.world.attribute;

import com.mojang.serialization.Codec;
import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public class EnvironmentAttribute<Value> {
   private final AttributeType<Value> type;
   private final Value defaultValue;
   private final AttributeRange<Value> valueRange;
   private final boolean isSyncable;
   private final boolean isPositional;
   private final boolean isSpatiallyInterpolated;

   private EnvironmentAttribute(
      final AttributeType<Value> type,
      final Value defaultValue,
      final AttributeRange<Value> valueRange,
      final boolean isSyncable,
      final boolean isPositional,
      final boolean isSpatiallyInterpolated
   ) {
      this.type = type;
      this.defaultValue = defaultValue;
      this.valueRange = valueRange;
      this.isSyncable = isSyncable;
      this.isPositional = isPositional;
      this.isSpatiallyInterpolated = isSpatiallyInterpolated;
   }

   public static <Value> EnvironmentAttribute.Builder<Value> builder(final AttributeType<Value> type) {
      return new EnvironmentAttribute.Builder<>(type);
   }

   public AttributeType<Value> type() {
      return this.type;
   }

   public Value defaultValue() {
      return this.defaultValue;
   }

   public Codec<Value> valueCodec() {
      return this.type.valueCodec().validate(this.valueRange::validate);
   }

   public Value sanitizeValue(final Value value) {
      return this.valueRange.sanitize(value);
   }

   public boolean isSyncable() {
      return this.isSyncable;
   }

   public boolean isPositional() {
      return this.isPositional;
   }

   public boolean isSpatiallyInterpolated() {
      return this.isSpatiallyInterpolated;
   }

   @Override
   public String toString() {
      return Util.getRegisteredName(BuiltInRegistries.ENVIRONMENT_ATTRIBUTE, this);
   }

   public static class Builder<Value> {
      private final AttributeType<Value> type;
      private @Nullable Value defaultValue;
      private AttributeRange<Value> valueRange = AttributeRange.any();
      private boolean isSyncable = false;
      private boolean isPositional = true;
      private boolean isSpatiallyInterpolated = false;

      public Builder(final AttributeType<Value> type) {
         this.type = type;
      }

      public EnvironmentAttribute.Builder<Value> defaultValue(final Value defaultValue) {
         this.defaultValue = defaultValue;
         return this;
      }

      public EnvironmentAttribute.Builder<Value> valueRange(final AttributeRange<Value> valueRange) {
         this.valueRange = valueRange;
         return this;
      }

      public EnvironmentAttribute.Builder<Value> syncable() {
         this.isSyncable = true;
         return this;
      }

      public EnvironmentAttribute.Builder<Value> notPositional() {
         this.isPositional = false;
         return this;
      }

      public EnvironmentAttribute.Builder<Value> spatiallyInterpolated() {
         this.isSpatiallyInterpolated = true;
         return this;
      }

      public EnvironmentAttribute<Value> build() {
         return new EnvironmentAttribute<>(
            this.type,
            Objects.requireNonNull(this.defaultValue, "Missing default value"),
            this.valueRange,
            this.isSyncable,
            this.isPositional,
            this.isSpatiallyInterpolated
         );
      }
   }
}
