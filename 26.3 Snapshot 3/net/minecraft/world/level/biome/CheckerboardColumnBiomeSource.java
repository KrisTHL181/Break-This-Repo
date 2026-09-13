package net.minecraft.world.level.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;

public class CheckerboardColumnBiomeSource extends BiomeSource {
   public static final MapCodec<CheckerboardColumnBiomeSource> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(Biome.LIST_CODEC.fieldOf("biomes").forGetter(s -> s.allowedBiomes), Codec.intRange(0, 62).optionalFieldOf("scale", 2).forGetter(s -> s.size))
         .apply(i, CheckerboardColumnBiomeSource::new)
   );
   private final HolderSet<Biome> allowedBiomes;
   private final int bitShift;
   private final int size;

   public CheckerboardColumnBiomeSource(final HolderSet<Biome> allowedBiomes, final int size) {
      this.allowedBiomes = allowedBiomes;
      this.bitShift = size + 2;
      this.size = size;
   }

   @Override
   protected Stream<Holder<Biome>> collectPossibleBiomes() {
      return this.allowedBiomes.stream();
   }

   @Override
   protected MapCodec<? extends BiomeSource> codec() {
      return CODEC;
   }

   @Override
   public Holder<Biome> getNoiseBiome(final int quartX, final int quartY, final int quartZ, final Climate.Sampler sampler) {
      return this.allowedBiomes.get(Math.floorMod((quartX >> this.bitShift) + (quartZ >> this.bitShift), this.allowedBiomes.size()));
   }
}
