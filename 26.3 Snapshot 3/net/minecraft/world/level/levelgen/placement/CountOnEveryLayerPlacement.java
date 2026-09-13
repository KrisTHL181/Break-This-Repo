package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

@Deprecated
public record CountOnEveryLayerPlacement(IntProvider count) implements PlacementModifier {
   public static final MapCodec<CountOnEveryLayerPlacement> CODEC = IntProviders.codec(0, 256)
      .fieldOf("count")
      .xmap(CountOnEveryLayerPlacement::new, CountOnEveryLayerPlacement::count);

   public static CountOnEveryLayerPlacement of(final IntProvider count) {
      return new CountOnEveryLayerPlacement(count);
   }

   public static CountOnEveryLayerPlacement of(final int count) {
      return of(ConstantInt.of(count));
   }

   @Override
   public void modify(final PlacementContext context, final RandomSource random, final BlockPos origin, final Consumer<BlockPos> output) {
      int layer = 0;

      boolean foundAny;
      do {
         foundAny = false;

         for (int i = 0; i < this.count.sample(random); i++) {
            int x = random.nextInt(16) + origin.getX();
            int z = random.nextInt(16) + origin.getZ();
            int startY = context.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            int y = findOnGroundYPosition(context, x, startY, z, layer);
            if (y != Integer.MAX_VALUE) {
               output.accept(new BlockPos(x, y, z));
               foundAny = true;
            }
         }

         layer++;
      } while (foundAny);
   }

   @Override
   public MapCodec<CountOnEveryLayerPlacement> codec() {
      return CODEC;
   }

   private static int findOnGroundYPosition(final PlacementContext context, final int xStart, final int yStart, final int zStart, final int layerToPlaceOn) {
      BlockPos.MutableBlockPos currentPos = new BlockPos.MutableBlockPos(xStart, yStart, zStart);
      int currentLayer = 0;
      BlockState currentBlock = context.getBlockState(currentPos);

      for (int y = yStart; y >= context.getMinY() + 1; y--) {
         currentPos.setY(y - 1);
         BlockState belowBlock = context.getBlockState(currentPos);
         if (!isEmpty(belowBlock) && isEmpty(currentBlock) && !belowBlock.is(Blocks.BEDROCK)) {
            if (currentLayer == layerToPlaceOn) {
               return currentPos.getY() + 1;
            }

            currentLayer++;
         }

         currentBlock = belowBlock;
      }

      return Integer.MAX_VALUE;
   }

   private static boolean isEmpty(final BlockState blockState) {
      return blockState.isAir() || blockState.is(Blocks.WATER) || blockState.is(Blocks.LAVA);
   }
}
