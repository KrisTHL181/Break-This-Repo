package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;

public record CountPlacement(IntProvider count) implements RepeatingPlacement {
   public static final MapCodec<CountPlacement> CODEC = IntProviders.codec(0, 4096).fieldOf("count").xmap(CountPlacement::new, CountPlacement::count);

   public static CountPlacement of(final IntProvider count) {
      return new CountPlacement(count);
   }

   public static CountPlacement of(final int count) {
      return of(ConstantInt.of(count));
   }

   @Override
   public int count(final RandomSource random, final BlockPos origin) {
      return this.count.sample(random);
   }

   @Override
   public MapCodec<CountPlacement> codec() {
      return CODEC;
   }
}
