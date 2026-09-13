package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;

public record RandomChancePlacement(float chance) implements PlacementFilter {
   public static final MapCodec<RandomChancePlacement> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(RandomChancePlacement::chance)).apply(i, RandomChancePlacement::new)
   );

   @Override
   public MapCodec<RandomChancePlacement> codec() {
      return CODEC;
   }

   @Override
   public boolean shouldPlace(final PlacementContext context, final RandomSource random, final BlockPos origin) {
      return random.nextFloat() < this.chance;
   }
}
