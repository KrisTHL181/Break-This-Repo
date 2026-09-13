package net.minecraft.world.attribute;

import com.google.common.collect.ImmutableBiMap;
import com.mojang.serialization.Codec;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.ToFloatFunction;
import net.minecraft.util.Util;
import net.minecraft.world.attribute.modifier.AttributeModifier;
import org.jspecify.annotations.Nullable;

public record AttributeType<Value>(
   Codec<Value> valueCodec,
   Map<AttributeModifier.OperationId, AttributeModifier<Value, ?>> modifierLibrary,
   Codec<AttributeModifier<Value, ?>> modifierCodec,
   LerpFunction<Value> keyframeLerp,
   LerpFunction<Value> stateChangeLerp,
   LerpFunction<Value> spatialLerp,
   LerpFunction<Value> partialTickLerp,
   @Nullable ToFloatFunction<Value> toFloat
) {
   public static <Value> AttributeType<Value> ofInterpolated(
      final Codec<Value> valueCodec, final Map<AttributeModifier.OperationId, AttributeModifier<Value, ?>> modifierLibrary, final LerpFunction<Value> lerp
   ) {
      return ofInterpolated(valueCodec, modifierLibrary, lerp, lerp, null);
   }

   public static <Value> AttributeType<Value> ofInterpolated(
      final Codec<Value> valueCodec,
      final Map<AttributeModifier.OperationId, AttributeModifier<Value, ?>> modifierLibrary,
      final LerpFunction<Value> lerp,
      final LerpFunction<Value> partialTickLerp,
      final @Nullable ToFloatFunction<Value> toFloat
   ) {
      return new AttributeType<>(valueCodec, modifierLibrary, createModifierCodec(modifierLibrary), lerp, lerp, lerp, partialTickLerp, toFloat);
   }

   public static <Value> AttributeType<Value> ofNotInterpolated(
      final Codec<Value> valueCodec, final Map<AttributeModifier.OperationId, AttributeModifier<Value, ?>> modifierLibrary
   ) {
      return new AttributeType<>(
         valueCodec,
         modifierLibrary,
         createModifierCodec(modifierLibrary),
         LerpFunction.ofStep(1.0F),
         LerpFunction.ofStep(0.0F),
         LerpFunction.ofStep(0.5F),
         LerpFunction.ofStep(0.0F),
         null
      );
   }

   public static <Value> AttributeType<Value> ofNotInterpolated(final Codec<Value> valueCodec) {
      return ofNotInterpolated(valueCodec, Map.of());
   }

   private static <Value> Codec<AttributeModifier<Value, ?>> createModifierCodec(
      final Map<AttributeModifier.OperationId, AttributeModifier<Value, ?>> modifiers
   ) {
      ImmutableBiMap<AttributeModifier.OperationId, AttributeModifier<Value, ?>> modifierLookup = ImmutableBiMap.builder()
         .put(AttributeModifier.OperationId.OVERRIDE, AttributeModifier.override())
         .putAll(modifiers)
         .buildOrThrow();
      return ExtraCodecs.idResolverCodec(AttributeModifier.OperationId.CODEC, modifierLookup::get, modifierLookup.inverse()::get);
   }

   public void checkAllowedModifier(final AttributeModifier<Value, ?> modifier) {
      if (modifier != AttributeModifier.override() && !this.modifierLibrary.containsValue(modifier)) {
         throw new IllegalArgumentException("Modifier " + modifier + " is not valid for " + this);
      }
   }

   public float toFloat(final Value value) {
      if (this.toFloat == null) {
         throw new IllegalStateException(value + " cannot be represented as a float");
      } else {
         return this.toFloat.applyAsFloat(value);
      }
   }

   @Override
   public String toString() {
      return Util.getRegisteredName(BuiltInRegistries.ATTRIBUTE_TYPE, this);
   }
}
