package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;

public record RarityFilter(int chance) implements PlacementFilter {
   public static final MapCodec<RarityFilter> CODEC = ExtraCodecs.POSITIVE_INT.fieldOf("chance").xmap(RarityFilter::new, RarityFilter::chance);

   public static RarityFilter onAverageOnceEvery(final int chance) {
      return new RarityFilter(chance);
   }

   @Override
   public boolean shouldPlace(final PlacementContext context, final RandomSource random, final BlockPos origin) {
      return random.nextFloat() < 1.0F / this.chance;
   }

   @Override
   public MapCodec<RarityFilter> codec() {
      return CODEC;
   }
}
