package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public class InfestedRotatedPillarBlock extends InfestedBlock {
   public static final MapCodec<InfestedRotatedPillarBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_422114_ -> p_422114_.group(BuiltInRegistries.BLOCK.byNameCodec().fieldOf("host").forGetter(InfestedBlock::getHostBlock), propertiesCodec())
         .apply(p_422114_, InfestedRotatedPillarBlock::new)
   );

   @Override
   public MapCodec<InfestedRotatedPillarBlock> codec() {
      return CODEC;
   }

   public InfestedRotatedPillarBlock(Block p_153438_, BlockBehaviour.Properties p_153439_) {
      super(p_153438_, p_153439_);
      this.registerDefaultState(this.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y));
   }

   @Override
   protected BlockState rotate(BlockState p_153443_, Rotation p_153444_) {
      return RotatedPillarBlock.rotatePillar(p_153443_, p_153444_);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_153446_) {
      p_153446_.add(RotatedPillarBlock.AXIS);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_153441_) {
      return this.defaultBlockState().setValue(RotatedPillarBlock.AXIS, p_153441_.getClickedFace().getAxis());
   }
}
