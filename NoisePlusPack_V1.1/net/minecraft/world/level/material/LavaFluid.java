package net.minecraft.world.level.material;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.InsideBlockEffectType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.gamerules.GameRules;
import org.jspecify.annotations.Nullable;

public abstract class LavaFluid extends FlowingFluid {
   public static final float MIN_LEVEL_CUTOFF = 0.44444445F;

   @Override
   public Fluid getFlowing() {
      return Fluids.FLOWING_LAVA;
   }

   @Override
   public Fluid getSource() {
      return Fluids.LAVA;
   }

   @Override
   public Item getBucket() {
      return Items.LAVA_BUCKET;
   }

   @Override
   public void animateTick(Level p_230567_, BlockPos p_230568_, FluidState p_230569_, RandomSource p_230570_) {
      BlockPos blockpos = p_230568_.above();
      if (p_230567_.getBlockState(blockpos).isAir() && !p_230567_.getBlockState(blockpos).isSolidRender()) {
         if (p_230570_.nextInt(100) == 0) {
            double d0 = p_230568_.getX() + p_230570_.nextDouble();
            double d1 = p_230568_.getY() + 1.0;
            double d2 = p_230568_.getZ() + p_230570_.nextDouble();
            p_230567_.addParticle(ParticleTypes.LAVA, d0, d1, d2, 0.0, 0.0, 0.0);
            p_230567_.playLocalSound(
               d0, d1, d2, SoundEvents.LAVA_POP, SoundSource.AMBIENT, 0.2F + p_230570_.nextFloat() * 0.2F, 0.9F + p_230570_.nextFloat() * 0.15F, false
            );
         }

         if (p_230570_.nextInt(200) == 0) {
            p_230567_.playLocalSound(
               p_230568_.getX(),
               p_230568_.getY(),
               p_230568_.getZ(),
               SoundEvents.LAVA_AMBIENT,
               SoundSource.AMBIENT,
               0.2F + p_230570_.nextFloat() * 0.2F,
               0.9F + p_230570_.nextFloat() * 0.15F,
               false
            );
         }
      }
   }

   @Override
   public void randomTick(ServerLevel p_367000_, BlockPos p_230573_, FluidState p_230574_, RandomSource p_230575_) {
      if (p_367000_.canSpreadFireAround(p_230573_)) {
         int i = p_230575_.nextInt(3);
         if (i > 0) {
            BlockPos blockpos = p_230573_;

            for (int j = 0; j < i; j++) {
               blockpos = blockpos.offset(p_230575_.nextInt(3) - 1, 1, p_230575_.nextInt(3) - 1);
               if (!p_367000_.isLoaded(blockpos)) {
                  return;
               }

               BlockState blockstate = p_367000_.getBlockState(blockpos);
               if (blockstate.isAir()) {
                  if (this.hasFlammableNeighbours(p_367000_, blockpos)) {
                     p_367000_.setBlockAndUpdate(blockpos, BaseFireBlock.getState(p_367000_, blockpos));
                     return;
                  }
               } else if (blockstate.blocksMotion()) {
                  return;
               }
            }
         } else {
            for (int k = 0; k < 3; k++) {
               BlockPos blockpos1 = p_230573_.offset(p_230575_.nextInt(3) - 1, 0, p_230575_.nextInt(3) - 1);
               if (!p_367000_.isLoaded(blockpos1)) {
                  return;
               }

               if (p_367000_.isEmptyBlock(blockpos1.above()) && this.isFlammable(p_367000_, blockpos1)) {
                  p_367000_.setBlockAndUpdate(blockpos1.above(), BaseFireBlock.getState(p_367000_, blockpos1));
               }
            }
         }
      }
   }

   @Override
   protected void entityInside(Level p_397670_, BlockPos p_395605_, Entity p_396547_, InsideBlockEffectApplier p_395270_) {
      p_395270_.apply(InsideBlockEffectType.CLEAR_FREEZE);
      p_395270_.apply(InsideBlockEffectType.LAVA_IGNITE);
      p_395270_.runAfter(InsideBlockEffectType.LAVA_IGNITE, Entity::lavaHurt);
   }

   private boolean hasFlammableNeighbours(LevelReader p_76228_, BlockPos p_76229_) {
      for (Direction direction : Direction.values()) {
         if (this.isFlammable(p_76228_, p_76229_.relative(direction))) {
            return true;
         }
      }

      return false;
   }

   private boolean isFlammable(LevelReader p_76246_, BlockPos p_76247_) {
      return p_76246_.isInsideBuildHeight(p_76247_.getY()) && !p_76246_.hasChunkAt(p_76247_) ? false : p_76246_.getBlockState(p_76247_).ignitedByLava();
   }

   @Override
   public @Nullable ParticleOptions getDripParticle() {
      return ParticleTypes.DRIPPING_LAVA;
   }

   @Override
   protected void beforeDestroyingBlock(LevelAccessor p_76216_, BlockPos p_76217_, BlockState p_76218_) {
      this.fizz(p_76216_, p_76217_);
   }

   @Override
   public int getSlopeFindDistance(LevelReader p_76244_) {
      return isFastLava(p_76244_) ? 4 : 2;
   }

   @Override
   public BlockState createLegacyBlock(FluidState p_76249_) {
      return Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(p_76249_));
   }

   @Override
   public boolean isSame(Fluid p_76231_) {
      return p_76231_ == Fluids.LAVA || p_76231_ == Fluids.FLOWING_LAVA;
   }

   @Override
   public int getDropOff(LevelReader p_76252_) {
      return isFastLava(p_76252_) ? 1 : 2;
   }

   @Override
   public boolean canBeReplacedWith(FluidState p_76233_, BlockGetter p_76234_, BlockPos p_76235_, Fluid p_76236_, Direction p_76237_) {
      return p_76233_.getHeight(p_76234_, p_76235_) >= 0.44444445F && p_76236_.is(FluidTags.WATER);
   }

   @Override
   public int getTickDelay(LevelReader p_76226_) {
      return isFastLava(p_76226_) ? 10 : 30;
   }

   @Override
   public int getSpreadDelay(Level p_76203_, BlockPos p_76204_, FluidState p_76205_, FluidState p_76206_) {
      int i = this.getTickDelay(p_76203_);
      if (!p_76205_.isEmpty()
         && !p_76206_.isEmpty()
         && !p_76205_.getValue(FALLING)
         && !p_76206_.getValue(FALLING)
         && p_76206_.getHeight(p_76203_, p_76204_) > p_76205_.getHeight(p_76203_, p_76204_)
         && p_76203_.getRandom().nextInt(4) != 0) {
         i *= 4;
      }

      return i;
   }

   private void fizz(LevelAccessor p_76213_, BlockPos p_76214_) {
      p_76213_.levelEvent(1501, p_76214_, 0);
   }

   @Override
   protected boolean canConvertToSource(ServerLevel p_362658_) {
      return p_362658_.getGameRules().get(GameRules.LAVA_SOURCE_CONVERSION);
   }

   @Override
   protected void spreadTo(LevelAccessor p_76220_, BlockPos p_76221_, BlockState p_76222_, Direction p_76223_, FluidState p_76224_) {
      if (p_76223_ == Direction.DOWN) {
         FluidState fluidstate = p_76220_.getFluidState(p_76221_);
         if (this.is(FluidTags.LAVA) && fluidstate.is(FluidTags.WATER)) {
            if (p_76222_.getBlock() instanceof LiquidBlock) {
               p_76220_.setBlock(p_76221_, Blocks.STONE.defaultBlockState(), 3);
            }

            this.fizz(p_76220_, p_76221_);
            return;
         }
      }

      super.spreadTo(p_76220_, p_76221_, p_76222_, p_76223_, p_76224_);
   }

   @Override
   protected boolean isRandomlyTicking() {
      return true;
   }

   @Override
   protected float getExplosionResistance() {
      return 100.0F;
   }

   @Override
   public Optional<SoundEvent> getPickupSound() {
      return Optional.of(SoundEvents.BUCKET_FILL_LAVA);
   }

   private static boolean isFastLava(LevelReader p_458627_) {
      return p_458627_.environmentAttributes().getDimensionValue(EnvironmentAttributes.FAST_LAVA);
   }

   public static class Flowing extends LavaFluid {
      @Override
      protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> p_76260_) {
         super.createFluidStateDefinition(p_76260_);
         p_76260_.add(LEVEL);
      }

      @Override
      public int getAmount(FluidState p_76264_) {
         return p_76264_.getValue(LEVEL);
      }

      @Override
      public boolean isSource(FluidState p_76262_) {
         return false;
      }
   }

   public static class Source extends LavaFluid {
      @Override
      public int getAmount(FluidState p_76269_) {
         return 8;
      }

      @Override
      public boolean isSource(FluidState p_76267_) {
         return true;
      }
   }
}
