package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.MapCodec;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

@Deprecated
public class CountOnEveryLayerPlacement extends PlacementModifier {
   public static final MapCodec<CountOnEveryLayerPlacement> CODEC = IntProvider.codec(0, 256)
      .fieldOf("count")
      .xmap(CountOnEveryLayerPlacement::new, p_191611_ -> p_191611_.count);
   private final IntProvider count;

   private CountOnEveryLayerPlacement(IntProvider p_191603_) {
      this.count = p_191603_;
   }

   public static CountOnEveryLayerPlacement of(IntProvider p_191607_) {
      return new CountOnEveryLayerPlacement(p_191607_);
   }

   public static CountOnEveryLayerPlacement of(int p_191605_) {
      return of(ConstantInt.of(p_191605_));
   }

   @Override
   public Stream<BlockPos> getPositions(PlacementContext p_226329_, RandomSource p_226330_, BlockPos p_226331_) {
      Builder<BlockPos> builder = Stream.builder();
      int i = 0;

      boolean flag;
      do {
         flag = false;

         for (int j = 0; j < this.count.sample(p_226330_); j++) {
            int k = p_226330_.nextInt(16) + p_226331_.getX();
            int l = p_226330_.nextInt(16) + p_226331_.getZ();
            int i1 = p_226329_.getHeight(Heightmap.Types.MOTION_BLOCKING, k, l);
            int j1 = findOnGroundYPosition(p_226329_, k, i1, l, i);
            if (j1 != Integer.MAX_VALUE) {
               builder.add(new BlockPos(k, j1, l));
               flag = true;
            }
         }

         i++;
      } while (flag);

      return builder.build();
   }

   @Override
   public PlacementModifierType<?> type() {
      return PlacementModifierType.COUNT_ON_EVERY_LAYER;
   }

   private static int findOnGroundYPosition(PlacementContext p_191613_, int p_191614_, int p_191615_, int p_191616_, int p_191617_) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos(p_191614_, p_191615_, p_191616_);
      int i = 0;
      BlockState blockstate = p_191613_.getBlockState(blockpos$mutableblockpos);

      for (int j = p_191615_; j >= p_191613_.getMinY() + 1; j--) {
         blockpos$mutableblockpos.setY(j - 1);
         BlockState blockstate1 = p_191613_.getBlockState(blockpos$mutableblockpos);
         if (!isEmpty(blockstate1) && isEmpty(blockstate) && !blockstate1.is(Blocks.BEDROCK)) {
            if (i == p_191617_) {
               return blockpos$mutableblockpos.getY() + 1;
            }

            i++;
         }

         blockstate = blockstate1;
      }

      return Integer.MAX_VALUE;
   }

   private static boolean isEmpty(BlockState p_191609_) {
      return p_191609_.isAir() || p_191609_.is(Blocks.WATER) || p_191609_.is(Blocks.LAVA);
   }
}
