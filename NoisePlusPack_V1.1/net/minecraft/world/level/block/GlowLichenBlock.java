package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class GlowLichenBlock extends MultifaceSpreadeableBlock implements BonemealableBlock {
   public static final MapCodec<GlowLichenBlock> CODEC = simpleCodec(GlowLichenBlock::new);
   private final MultifaceSpreader spreader = new MultifaceSpreader(this);

   @Override
   public MapCodec<GlowLichenBlock> codec() {
      return CODEC;
   }

   public GlowLichenBlock(BlockBehaviour.Properties p_153282_) {
      super(p_153282_);
   }

   public static ToIntFunction<BlockState> emission(int p_181223_) {
      return p_181221_ -> MultifaceBlock.hasAnyFace(p_181221_) ? p_181223_ : 0;
   }

   @Override
   public boolean isValidBonemealTarget(LevelReader p_256569_, BlockPos p_153290_, BlockState p_153291_) {
      return Direction.stream().anyMatch(p_153316_ -> this.spreader.canSpreadInAnyDirection(p_153291_, p_256569_, p_153290_, p_153316_.getOpposite()));
   }

   @Override
   public boolean isBonemealSuccess(Level p_221264_, RandomSource p_221265_, BlockPos p_221266_, BlockState p_221267_) {
      return true;
   }

   @Override
   public void performBonemeal(ServerLevel p_221259_, RandomSource p_221260_, BlockPos p_221261_, BlockState p_221262_) {
      this.spreader.spreadFromRandomFaceTowardRandomDirection(p_221262_, p_221259_, p_221261_, p_221260_);
   }

   @Override
   protected boolean propagatesSkylightDown(BlockState p_181225_) {
      return p_181225_.getFluidState().isEmpty();
   }

   @Override
   public MultifaceSpreader getSpreader() {
      return this.spreader;
   }
}
