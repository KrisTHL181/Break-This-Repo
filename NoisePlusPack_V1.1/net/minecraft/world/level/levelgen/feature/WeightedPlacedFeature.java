package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class WeightedPlacedFeature {
   public static final Codec<WeightedPlacedFeature> CODEC = RecordCodecBuilder.create(
      p_191187_ -> p_191187_.group(
            PlacedFeature.CODEC.fieldOf("feature").forGetter(p_204789_ -> p_204789_.feature),
            Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(p_191189_ -> p_191189_.chance)
         )
         .apply(p_191187_, WeightedPlacedFeature::new)
   );
   public final Holder<PlacedFeature> feature;
   public final float chance;

   public WeightedPlacedFeature(Holder<PlacedFeature> p_204786_, float p_204787_) {
      this.feature = p_204786_;
      this.chance = p_204787_;
   }

   public boolean place(WorldGenLevel p_225368_, ChunkGenerator p_225369_, RandomSource p_225370_, BlockPos p_225371_) {
      return this.feature.value().place(p_225368_, p_225369_, p_225370_, p_225371_);
   }
}
