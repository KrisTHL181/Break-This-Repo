package net.minecraft.world.level.levelgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;

public record WorldGenSettings(WorldOptions options, WorldDimensions dimensions) {
   public static final Codec<WorldGenSettings> CODEC = RecordCodecBuilder.create(
      p_248477_ -> p_248477_.group(WorldOptions.CODEC.forGetter(WorldGenSettings::options), WorldDimensions.CODEC.forGetter(WorldGenSettings::dimensions))
         .apply(p_248477_, p_248477_.stable(WorldGenSettings::new))
   );

   public static <T> DataResult<T> encode(DynamicOps<T> p_250104_, WorldOptions p_250578_, WorldDimensions p_249244_) {
      return CODEC.encodeStart(p_250104_, new WorldGenSettings(p_250578_, p_249244_));
   }

   public static <T> DataResult<T> encode(DynamicOps<T> p_250917_, WorldOptions p_250366_, RegistryAccess p_251515_) {
      return encode(p_250917_, p_250366_, new WorldDimensions(p_251515_.lookupOrThrow(Registries.LEVEL_STEM)));
   }
}
