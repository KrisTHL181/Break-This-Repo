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

   EnvironmentAttribute(
      AttributeType<Value> p_450629_, Value p_452677_, AttributeRange<Value> p_459529_, boolean p_456259_, boolean p_455306_, boolean p_460108_
   ) {
      this.type = p_450629_;
      this.defaultValue = p_452677_;
      this.valueRange = p_459529_;
      this.isSyncable = p_456259_;
      this.isPositional = p_455306_;
      this.isSpatiallyInterpolated = p_460108_;
   }

   public static <Value> EnvironmentAttribute.Builder<Value> builder(AttributeType<Value> p_451655_) {
      return new EnvironmentAttribute.Builder<>(p_451655_);
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

   public Value sanitizeValue(Value p_451273_) {
      return this.valueRange.sanitize(p_451273_);
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

      public Builder(AttributeType<Value> p_455229_) {
         this.type = p_455229_;
      }

      public EnvironmentAttribute.Builder<Value> defaultValue(Value p_458717_) {
         this.defaultValue = p_458717_;
         return this;
      }

      public EnvironmentAttribute.Builder<Value> valueRange(AttributeRange<Value> p_452684_) {
         this.valueRange = p_452684_;
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
