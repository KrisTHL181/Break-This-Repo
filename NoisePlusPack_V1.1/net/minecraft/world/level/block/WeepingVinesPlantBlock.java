package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WeepingVinesPlantBlock extends GrowingPlantBodyBlock {
   public static final MapCodec<WeepingVinesPlantBlock> CODEC = simpleCodec(WeepingVinesPlantBlock::new);
   private static final VoxelShape SHAPE = Block.column(14.0, 0.0, 16.0);

   @Override
   public MapCodec<WeepingVinesPlantBlock> codec() {
      return CODEC;
   }

   public WeepingVinesPlantBlock(BlockBehaviour.Properties p_154975_) {
      super(p_154975_, Direction.DOWN, SHAPE, false);
   }

   @Override
   protected GrowingPlantHeadBlock getHeadBlock() {
      return (GrowingPlantHeadBlock)Blocks.WEEPING_VINES;
   }
}
