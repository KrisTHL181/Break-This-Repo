package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;

public class CountPlacement extends RepeatingPlacement {
   public static final MapCodec<CountPlacement> CODEC = IntProvider.codec(0, 256).fieldOf("count").xmap(CountPlacement::new, p_191633_ -> p_191633_.count);
   private final IntProvider count;

   private CountPlacement(IntProvider p_191627_) {
      this.count = p_191627_;
   }

   public static CountPlacement of(IntProvider p_191631_) {
      return new CountPlacement(p_191631_);
   }

   public static CountPlacement of(int p_191629_) {
      return of(ConstantInt.of(p_191629_));
   }

   @Override
   protected int count(RandomSource p_226333_, BlockPos p_226334_) {
      return this.count.sample(p_226333_);
   }

   @Override
   public PlacementModifierType<?> type() {
      return PlacementModifierType.COUNT;
   }
}
