package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class NetherSproutsBlock extends VegetationBlock {
   public static final MapCodec<NetherSproutsBlock> CODEC = simpleCodec(NetherSproutsBlock::new);
   private static final VoxelShape SHAPE = Block.column(12.0, 0.0, 3.0);

   @Override
   public MapCodec<NetherSproutsBlock> codec() {
      return CODEC;
   }

   public NetherSproutsBlock(BlockBehaviour.Properties p_54952_) {
      super(p_54952_);
   }

   @Override
   protected VoxelShape getShape(BlockState p_54955_, BlockGetter p_54956_, BlockPos p_54957_, CollisionContext p_54958_) {
      return SHAPE;
   }

   @Override
   protected boolean mayPlaceOn(BlockState p_54960_, BlockGetter p_54961_, BlockPos p_54962_) {
      return p_54960_.is(BlockTags.NYLIUM) || p_54960_.is(Blocks.SOUL_SOIL) || super.mayPlaceOn(p_54960_, p_54961_, p_54962_);
   }
}
