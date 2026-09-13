package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.VineBlock;

public class TrunkVineDecorator extends TreeDecorator {
   public static final MapCodec<TrunkVineDecorator> CODEC = MapCodec.unit(() -> TrunkVineDecorator.INSTANCE);
   public static final TrunkVineDecorator INSTANCE = new TrunkVineDecorator();

   @Override
   protected TreeDecoratorType<?> type() {
      return TreeDecoratorType.TRUNK_VINE;
   }

   @Override
   public void place(TreeDecorator.Context p_226077_) {
      RandomSource randomsource = p_226077_.random();
      p_226077_.logs().forEach(p_226075_ -> {
         if (randomsource.nextInt(3) > 0) {
            BlockPos blockpos = p_226075_.west();
            if (p_226077_.isAir(blockpos)) {
               p_226077_.placeVine(blockpos, VineBlock.EAST);
            }
         }

         if (randomsource.nextInt(3) > 0) {
            BlockPos blockpos1 = p_226075_.east();
            if (p_226077_.isAir(blockpos1)) {
               p_226077_.placeVine(blockpos1, VineBlock.WEST);
            }
         }

         if (randomsource.nextInt(3) > 0) {
            BlockPos blockpos2 = p_226075_.north();
            if (p_226077_.isAir(blockpos2)) {
               p_226077_.placeVine(blockpos2, VineBlock.SOUTH);
            }
         }

         if (randomsource.nextInt(3) > 0) {
            BlockPos blockpos3 = p_226075_.south();
            if (p_226077_.isAir(blockpos3)) {
               p_226077_.placeVine(blockpos3, VineBlock.NORTH);
            }
         }
      });
   }
}
