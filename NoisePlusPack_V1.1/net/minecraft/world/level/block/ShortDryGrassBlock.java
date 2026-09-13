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

public class ShortDryGrassBlock extends DryVegetationBlock implements BonemealableBlock {
   public static final MapCodec<ShortDryGrassBlock> CODEC = simpleCodec(ShortDryGrassBlock::new);
   private static final VoxelShape SHAPE = Block.column(12.0, 0.0, 10.0);

   @Override
   public MapCodec<ShortDryGrassBlock> codec() {
      return CODEC;
   }

   protected ShortDryGrassBlock(BlockBehaviour.Properties p_394311_) {
      super(p_394311_);
   }

   @Override
   protected VoxelShape getShape(BlockState p_397731_, BlockGetter p_396409_, BlockPos p_396288_, CollisionContext p_396529_) {
      return SHAPE;
   }

   @Override
   public void animateTick(BlockState p_407460_, Level p_409228_, BlockPos p_407025_, RandomSource p_406398_) {
      AmbientDesertBlockSoundsPlayer.playAmbientDryGrassSounds(p_409228_, p_407025_, p_406398_);
   }

   @Override
   public boolean isValidBonemealTarget(LevelReader p_393202_, BlockPos p_395169_, BlockState p_392225_) {
      return true;
   }

   @Override
   public boolean isBonemealSuccess(Level p_393007_, RandomSource p_392616_, BlockPos p_396855_, BlockState p_393878_) {
      return true;
   }

   @Override
   public void performBonemeal(ServerLevel p_395274_, RandomSource p_394266_, BlockPos p_394099_, BlockState p_396116_) {
      p_395274_.setBlockAndUpdate(p_394099_, Blocks.TALL_DRY_GRASS.defaultBlockState());
   }
}
