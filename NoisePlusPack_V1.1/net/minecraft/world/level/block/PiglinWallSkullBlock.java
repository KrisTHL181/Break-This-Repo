package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PiglinWallSkullBlock extends WallSkullBlock {
   public static final MapCodec<PiglinWallSkullBlock> CODEC = simpleCodec(PiglinWallSkullBlock::new);
   private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(Block.boxZ(10.0, 8.0, 8.0, 16.0));

   @Override
   public MapCodec<PiglinWallSkullBlock> codec() {
      return CODEC;
   }

   public PiglinWallSkullBlock(BlockBehaviour.Properties p_261530_) {
      super(SkullBlock.Types.PIGLIN, p_261530_);
   }

   @Override
   protected VoxelShape getShape(BlockState p_261765_, BlockGetter p_261604_, BlockPos p_261948_, CollisionContext p_261889_) {
      return SHAPES.get(p_261765_.getValue(FACING));
   }
}
