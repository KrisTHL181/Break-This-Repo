package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

public class FireflyBushBlock extends VegetationBlock implements BonemealableBlock {
   private static final double FIREFLY_CHANCE_PER_TICK = 0.7;
   private static final double FIREFLY_HORIZONTAL_RANGE = 10.0;
   private static final double FIREFLY_VERTICAL_RANGE = 5.0;
   private static final int FIREFLY_SPAWN_MAX_BRIGHTNESS_LEVEL = 13;
   private static final int FIREFLY_AMBIENT_SOUND_CHANCE_ONE_IN = 30;
   public static final MapCodec<FireflyBushBlock> CODEC = simpleCodec(FireflyBushBlock::new);

   public FireflyBushBlock(BlockBehaviour.Properties p_395309_) {
      super(p_395309_);
   }

   @Override
   protected MapCodec<? extends FireflyBushBlock> codec() {
      return CODEC;
   }

   @Override
   public void animateTick(BlockState p_397225_, Level p_392787_, BlockPos p_393542_, RandomSource p_393796_) {
      if (p_393796_.nextInt(30) == 0
         && p_392787_.environmentAttributes().getValue(EnvironmentAttributes.FIREFLY_BUSH_SOUNDS, p_393542_)
         && p_392787_.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p_393542_) <= p_393542_.getY()) {
         p_392787_.playLocalSound(p_393542_, SoundEvents.FIREFLY_BUSH_IDLE, SoundSource.AMBIENT, 1.0F, 1.0F, false);
      }

      if (p_392787_.getMaxLocalRawBrightness(p_393542_) <= 13 && p_393796_.nextDouble() <= 0.7) {
         double d0 = p_393542_.getX() + p_393796_.nextDouble() * 10.0 - 5.0;
         double d1 = p_393542_.getY() + p_393796_.nextDouble() * 5.0;
         double d2 = p_393542_.getZ() + p_393796_.nextDouble() * 10.0 - 5.0;
         p_392787_.addParticle(ParticleTypes.FIREFLY, d0, d1, d2, 0.0, 0.0, 0.0);
      }
   }

   @Override
   public boolean isValidBonemealTarget(LevelReader p_393203_, BlockPos p_396880_, BlockState p_391928_) {
      return BonemealableBlock.hasSpreadableNeighbourPos(p_393203_, p_396880_, p_391928_);
   }

   @Override
   public boolean isBonemealSuccess(Level p_393377_, RandomSource p_394134_, BlockPos p_391674_, BlockState p_396838_) {
      return true;
   }

   @Override
   public void performBonemeal(ServerLevel p_393169_, RandomSource p_396608_, BlockPos p_392622_, BlockState p_395668_) {
      BonemealableBlock.findSpreadableNeighbourPos(p_393169_, p_392622_, p_395668_)
         .ifPresent(p_405689_ -> p_393169_.setBlockAndUpdate(p_405689_, this.defaultBlockState()));
   }
}
