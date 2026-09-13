package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AirBlock extends Block {
   public static final MapCodec<AirBlock> CODEC = simpleCodec(AirBlock::new);

   @Override
   public MapCodec<AirBlock> codec() {
      return CODEC;
   }

   public AirBlock(BlockBehaviour.Properties p_48756_) {
      super(p_48756_);
   }

   @Override
   protected RenderShape getRenderShape(BlockState p_48758_) {
      return RenderShape.INVISIBLE;
   }

   @Override
   protected VoxelShape getShape(BlockState p_48760_, BlockGetter p_48761_, BlockPos p_48762_, CollisionContext p_48763_) {
      return Shapes.empty();
   }
}
