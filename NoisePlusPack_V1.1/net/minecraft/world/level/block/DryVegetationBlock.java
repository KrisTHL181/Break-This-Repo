package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.sounds.AmbientDesertBlockSoundsPlayer;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DryVegetationBlock extends VegetationBlock {
   public static final MapCodec<DryVegetationBlock> CODEC = simpleCodec(DryVegetationBlock::new);
   private static final VoxelShape SHAPE = Block.column(12.0, 0.0, 13.0);

   @Override
   public MapCodec<? extends DryVegetationBlock> codec() {
      return CODEC;
   }

   protected DryVegetationBlock(BlockBehaviour.Properties p_397903_) {
      super(p_397903_);
   }

   @Override
   protected VoxelShape getShape(BlockState p_392262_, BlockGetter p_392875_, BlockPos p_394920_, CollisionContext p_395697_) {
      return SHAPE;
   }

   @Override
   protected boolean mayPlaceOn(BlockState p_392399_, BlockGetter p_393872_, BlockPos p_395819_) {
      return p_392399_.is(BlockTags.DRY_VEGETATION_MAY_PLACE_ON);
   }

   @Override
   public void animateTick(BlockState p_392005_, Level p_391577_, BlockPos p_396013_, RandomSource p_394858_) {
      AmbientDesertBlockSoundsPlayer.playAmbientDeadBushSounds(p_391577_, p_396013_, p_394858_);
   }
}
