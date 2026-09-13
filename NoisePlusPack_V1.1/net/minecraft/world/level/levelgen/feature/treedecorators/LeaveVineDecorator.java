package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class LeaveVineDecorator extends TreeDecorator {
   public static final MapCodec<LeaveVineDecorator> CODEC = Codec.floatRange(0.0F, 1.0F)
      .fieldOf("probability")
      .xmap(LeaveVineDecorator::new, p_226037_ -> p_226037_.probability);
   private final float probability;

   @Override
   protected TreeDecoratorType<?> type() {
      return TreeDecoratorType.LEAVE_VINE;
   }

   public LeaveVineDecorator(float p_226031_) {
      this.probability = p_226031_;
   }

   @Override
   public void place(TreeDecorator.Context p_226039_) {
      RandomSource randomsource = p_226039_.random();
      p_226039_.leaves().forEach(p_226035_ -> {
         if (randomsource.nextFloat() < this.probability) {
            BlockPos blockpos = p_226035_.west();
            if (p_226039_.isAir(blockpos)) {
               addHangingVine(blockpos, VineBlock.EAST, p_226039_);
            }
         }

         if (randomsource.nextFloat() < this.probability) {
            BlockPos blockpos1 = p_226035_.east();
            if (p_226039_.isAir(blockpos1)) {
               addHangingVine(blockpos1, VineBlock.WEST, p_226039_);
            }
         }

         if (randomsource.nextFloat() < this.probability) {
            BlockPos blockpos2 = p_226035_.north();
            if (p_226039_.isAir(blockpos2)) {
               addHangingVine(blockpos2, VineBlock.SOUTH, p_226039_);
            }
         }

         if (randomsource.nextFloat() < this.probability) {
            BlockPos blockpos3 = p_226035_.south();
            if (p_226039_.isAir(blockpos3)) {
               addHangingVine(blockpos3, VineBlock.NORTH, p_226039_);
            }
         }
      });
   }

   private static void addHangingVine(BlockPos p_226041_, BooleanProperty p_226042_, TreeDecorator.Context p_226043_) {
      p_226043_.placeVine(p_226041_, p_226042_);
      int i = 4;

      for (BlockPos blockpos = p_226041_.below(); p_226043_.isAir(blockpos) && i > 0; i--) {
         p_226043_.placeVine(blockpos, p_226042_);
         blockpos = blockpos.below();
      }
   }
}
