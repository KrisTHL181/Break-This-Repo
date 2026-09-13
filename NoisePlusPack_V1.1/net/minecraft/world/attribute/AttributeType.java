package net.minecraft.world.attribute;

import com.google.common.collect.ImmutableBiMap;
import com.mojang.serialization.Codec;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Util;
import net.minecraft.world.attribute.modifier.AttributeModifier;

public record AttributeType<Value>(
   Codec<Value> valueCodec,
   Map<AttributeModifier.OperationId, AttributeModifier<Value, ?>> modifierLibrary,
   Codec<AttributeModifier<Value, ?>> modifierCodec,
   LerpFunction<Value> keyframeLerp,
   LerpFunction<Value> stateChangeLerp,
   LerpFunction<Value> spatialLerp,
   LerpFunction<Value> partialTickLerp
) {
   public static <Value> AttributeType<Value> ofInterpolated(
      Codec<Value> p_452435_, Map<AttributeModifier.OperationId, AttributeModifier<Value, ?>> p_458803_, LerpFunction<Value> p_457385_
   ) {
      return ofInterpolated(p_452435_, p_458803_, p_457385_, p_457385_);
   }

   public static <Value> AttributeType<Value> ofInterpolated(
      Codec<Value> p_456291_,
      Map<AttributeModifier.OperationId, AttributeModifier<Value, ?>> p_452389_,
      LerpFunction<Value> p_459023_,
      LerpFunction<Value> p_460276_
   ) {
      return new AttributeType<>(p_456291_, p_452389_, createModifierCodec(p_452389_), p_459023_, p_459023_, p_459023_, p_460276_);
   }

   public static <Value> AttributeType<Value> ofNotInterpolated(
      Codec<Value> p_457138_, Map<AttributeModifier.OperationId, AttributeModifier<Value, ?>> p_453935_
   ) {
      return new AttributeType<>(
         p_457138_,
         p_453935_,
         createModifierCodec(p_453935_),
         LerpFunction.ofStep(1.0F),
         LerpFunction.ofStep(0.0F),
         LerpFunction.ofStep(0.5F),
         LerpFunction.ofStep(0.0F)
      );
   }

   public static <Value> AttributeType<Value> ofNotInterpolated(Codec<Value> p_458235_) {
      return ofNotInterpolated(p_458235_, Map.of());
   }

   private static <Value> Codec<AttributeModifier<Value, ?>> createModifierCodec(Map<AttributeModifier.OperationId, AttributeModifier<Value, ?>> p_458451_) {
      ImmutableBiMap<AttributeModifier.OperationId, AttributeModifier<Value, ?>> immutablebimap = ImmutableBiMap.builder()
         .put(AttributeModifier.OperationId.OVERRIDE, AttributeModifier.override())
         .putAll(p_458451_)
         .buildOrThrow();
      return ExtraCodecs.idResolverCodec(AttributeModifier.OperationId.CODEC, immutablebimap::get, immutablebimap.inverse()::get);
   }

   public void checkAllowedModifier(AttributeModifier<Value, ?> p_460861_) {
      if (p_460861_ != AttributeModifier.override() && !this.modifierLibrary.containsValue(p_460861_)) {
         throw new IllegalArgumentException("Modifier " + p_460861_ + " is not valid for " + this);
      }
   }

   @Override
   public String toString() {
      return Util.getRegisteredName(BuiltInRegistries.ATTRIBUTE_TYPE, this);
   }
}
