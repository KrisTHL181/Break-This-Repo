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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.InsideBlockEffectType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.gamerules.GameRules;
import org.jspecify.annotations.Nullable;

public abstract class WaterFluid extends FlowingFluid {
   @Override
   public Fluid getFlowing() {
      return Fluids.FLOWING_WATER;
   }

   @Override
   public Fluid getSource() {
      return Fluids.WATER;
   }

   @Override
   public Item getBucket() {
      return Items.WATER_BUCKET;
   }

   @Override
   public void animateTick(final Level level, final BlockPos pos, final FluidState fluidState, final RandomSource random) {
      if (!fluidState.isSource() && !fluidState.getValue(FALLING)) {
         if (random.nextInt(64) == 0) {
            level.playLocalSound(
               pos.getX() + 0.5,
               pos.getY() + 0.5,
               pos.getZ() + 0.5,
               SoundEvents.WATER_AMBIENT,
               SoundSource.AMBIENT,
               random.nextFloat() * 0.25F + 0.75F,
               random.nextFloat() + 0.5F,
               false
            );
         }
      } else if (random.nextInt(10) == 0) {
         level.addParticle(
            ParticleTypes.UNDERWATER, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(), pos.getZ() + random.nextDouble(), 0.0, 0.0, 0.0
         );
      }
   }

   @Override
   public @Nullable ParticleOptions getDripParticle() {
      return ParticleTypes.DRIPPING_WATER;
   }

   @Override
   protected boolean canConvertToSource(final ServerLevel level) {
      return level.getGameRules().get(GameRules.WATER_SOURCE_CONVERSION);
   }

   @Override
   protected void beforeDestroyingBlock(final LevelAccessor level, final BlockPos pos, final BlockState state) {
      BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
      Block.dropResources(state, level, pos, blockEntity);
   }

   @Override
   protected void entityInside(final Level level, final BlockPos pos, final Entity entity, final InsideBlockEffectApplier effectApplier) {
      effectApplier.apply(InsideBlockEffectType.EXTINGUISH);
   }

   @Override
   public int getSlopeFindDistance(final LevelReader level) {
      return 4;
   }

   @Override
   public BlockState createLegacyBlock(final FluidState fluidState) {
      return Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(fluidState));
   }

   @Override
   public boolean isSame(final Fluid other) {
      return other == Fluids.WATER || other == Fluids.FLOWING_WATER;
   }

   @Override
   public int getDropOff(final LevelReader level) {
      return 1;
   }

   @Override
   public int getTickDelay(final LevelReader level) {
      return 5;
   }

   @Override
   public boolean canBeReplacedWith(final FluidState state, final BlockGetter level, final BlockPos pos, final Fluid other, final Direction direction) {
      return direction == Direction.DOWN && !other.is(FluidTags.WATER);
   }

   @Override
   protected float getExplosionResistance() {
      return 100.0F;
   }

   @Override
   public Optional<SoundEvent> getPickupSound() {
      return Optional.of(SoundEvents.BUCKET_FILL);
   }

   public static class Flowing extends WaterFluid {
      @Override
      protected void createFluidStateDefinition(final StateDefinition.Builder<Fluid, FluidState> builder) {
         super.createFluidStateDefinition(builder);
         builder.add(LEVEL);
      }

      @Override
      public int getAmount(final FluidState fluidState) {
         return fluidState.getValue(LEVEL);
      }

      @Override
      public boolean isSource(final FluidState fluidState) {
         return false;
      }
   }

   public static class Source extends WaterFluid {
      @Override
      public int getAmount(final FluidState fluidState) {
         return 8;
      }

      @Override
      public boolean isSource(final FluidState fluidState) {
         return true;
      }
   }
}
