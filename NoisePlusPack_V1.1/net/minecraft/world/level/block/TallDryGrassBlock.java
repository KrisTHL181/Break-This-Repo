package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.sounds.AmbientDesertBlockSoundsPlayer;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TallDryGrassBlock extends DryVegetationBlock implements BonemealableBlock {
   public static final MapCodec<TallDryGrassBlock> CODEC = simpleCodec(TallDryGrassBlock::new);
   private static final VoxelShape SHAPE = Block.column(14.0, 0.0, 16.0);

   @Override
   public MapCodec<TallDryGrassBlock> codec() {
      return CODEC;
   }

   protected TallDryGrassBlock(BlockBehaviour.Properties p_395204_) {
      super(p_395204_);
   }

   @Override
   protected VoxelShape getShape(BlockState p_393637_, BlockGetter p_393034_, BlockPos p_397737_, CollisionContext p_393523_) {
      return SHAPE;
   }

   @Override
   public void animateTick(BlockState p_408366_, Level p_407754_, BlockPos p_407171_, RandomSource p_410543_) {
      AmbientDesertBlockSoundsPlayer.playAmbientDryGrassSounds(p_407754_, p_407171_, p_410543_);
   }

   @Override
   public boolean isValidBonemealTarget(LevelReader p_395858_, BlockPos p_392247_, BlockState p_394360_) {
      return BonemealableBlock.hasSpreadableNeighbourPos(p_395858_, p_392247_, Blocks.SHORT_DRY_GRASS.defaultBlockState());
   }

   @Override
   public boolean isBonemealSuccess(Level p_395047_, RandomSource p_395975_, BlockPos p_391643_, BlockState p_395606_) {
      return true;
   }

   @Override
   public void performBonemeal(ServerLevel p_395820_, RandomSource p_397995_, BlockPos p_394897_, BlockState p_395908_) {
      BonemealableBlock.findSpreadableNeighbourPos(p_395820_, p_394897_, Blocks.SHORT_DRY_GRASS.defaultBlockState())
         .ifPresent(p_405697_ -> p_395820_.setBlockAndUpdate(p_405697_, Blocks.SHORT_DRY_GRASS.defaultBlockState()));
   }
}
