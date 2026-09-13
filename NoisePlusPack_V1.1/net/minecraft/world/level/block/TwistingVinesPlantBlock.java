package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TwistingVinesPlantBlock extends GrowingPlantBodyBlock {
   public static final MapCodec<TwistingVinesPlantBlock> CODEC = simpleCodec(TwistingVinesPlantBlock::new);
   private static final VoxelShape SHAPE = Block.column(8.0, 0.0, 16.0);

   @Override
   public MapCodec<TwistingVinesPlantBlock> codec() {
      return CODEC;
   }

   public TwistingVinesPlantBlock(BlockBehaviour.Properties p_154873_) {
      super(p_154873_, Direction.UP, SHAPE, false);
   }

   @Override
   protected GrowingPlantHeadBlock getHeadBlock() {
      return (GrowingPlantHeadBlock)Blocks.TWISTING_VINES;
   }
}
