package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CactusFlowerBlock extends VegetationBlock {
   public static final MapCodec<CactusFlowerBlock> CODEC = simpleCodec(CactusFlowerBlock::new);
   private static final VoxelShape SHAPE = Block.column(14.0, 0.0, 12.0);

   @Override
   public MapCodec<? extends CactusFlowerBlock> codec() {
      return CODEC;
   }

   public CactusFlowerBlock(BlockBehaviour.Properties p_393021_) {
      super(p_393021_);
   }

   @Override
   protected VoxelShape getShape(BlockState p_397058_, BlockGetter p_391254_, BlockPos p_397308_, CollisionContext p_397333_) {
      return SHAPE;
   }

   @Override
   protected boolean mayPlaceOn(BlockState p_395694_, BlockGetter p_391810_, BlockPos p_391352_) {
      BlockState blockstate = p_391810_.getBlockState(p_391352_);
      return blockstate.is(Blocks.CACTUS) || blockstate.is(Blocks.FARMLAND) || blockstate.isFaceSturdy(p_391810_, p_391352_, Direction.UP, SupportType.CENTER);
   }
}
