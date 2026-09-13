package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;

public class RarityFilter extends PlacementFilter {
   public static final MapCodec<RarityFilter> CODEC = ExtraCodecs.POSITIVE_INT.fieldOf("chance").xmap(RarityFilter::new, p_191907_ -> p_191907_.chance);
   private final int chance;

   private RarityFilter(int p_191899_) {
      this.chance = p_191899_;
   }

   public static RarityFilter onAverageOnceEvery(int p_191901_) {
      return new RarityFilter(p_191901_);
   }

   @Override
   protected boolean shouldPlace(PlacementContext p_226397_, RandomSource p_226398_, BlockPos p_226399_) {
      return p_226398_.nextFloat() < 1.0F / this.chance;
   }

   @Override
   public PlacementModifierType<?> type() {
      return PlacementModifierType.RARITY_FILTER;
   }
}
