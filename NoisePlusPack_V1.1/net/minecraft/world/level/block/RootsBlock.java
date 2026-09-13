package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RootsBlock extends VegetationBlock {
   public static final MapCodec<RootsBlock> CODEC = simpleCodec(RootsBlock::new);
   private static final VoxelShape SHAPE = Block.column(12.0, 0.0, 13.0);

   @Override
   public MapCodec<RootsBlock> codec() {
      return CODEC;
   }

   protected RootsBlock(BlockBehaviour.Properties p_55912_) {
      super(p_55912_);
   }

   @Override
   protected VoxelShape getShape(BlockState p_55915_, BlockGetter p_55916_, BlockPos p_55917_, CollisionContext p_55918_) {
      return SHAPE;
   }

   @Override
   protected boolean mayPlaceOn(BlockState p_55920_, BlockGetter p_55921_, BlockPos p_55922_) {
      return p_55920_.is(BlockTags.NYLIUM) || p_55920_.is(Blocks.SOUL_SOIL) || super.mayPlaceOn(p_55920_, p_55921_, p_55922_);
   }
}
