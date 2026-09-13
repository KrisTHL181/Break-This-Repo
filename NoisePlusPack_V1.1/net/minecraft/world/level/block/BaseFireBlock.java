package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.InsideBlockEffectType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class BaseFireBlock extends Block {
   private static final int SECONDS_ON_FIRE = 8;
   private static final int MIN_FIRE_TICKS_TO_ADD = 1;
   private static final int MAX_FIRE_TICKS_TO_ADD = 3;
   private final float fireDamage;
   protected static final VoxelShape SHAPE = Block.column(16.0, 0.0, 1.0);

   public BaseFireBlock(BlockBehaviour.Properties p_49241_, float p_49242_) {
      super(p_49241_);
      this.fireDamage = p_49242_;
   }

   @Override
   protected abstract MapCodec<? extends BaseFireBlock> codec();

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_49244_) {
      return getState(p_49244_.getLevel(), p_49244_.getClickedPos());
   }

   public static BlockState getState(BlockGetter p_49246_, BlockPos p_49247_) {
      BlockPos blockpos = p_49247_.below();
      BlockState blockstate = p_49246_.getBlockState(blockpos);
      return SoulFireBlock.canSurviveOnBlock(blockstate)
         ? Blocks.SOUL_FIRE.defaultBlockState()
         : ((FireBlock)Blocks.FIRE).getStateForPlacement(p_49246_, p_49247_);
   }

   @Override
   protected VoxelShape getShape(BlockState p_49274_, BlockGetter p_49275_, BlockPos p_49276_, CollisionContext p_49277_) {
      return SHAPE;
   }

   @Override
   public void animateTick(BlockState p_220763_, Level p_220764_, BlockPos p_220765_, RandomSource p_220766_) {
      if (p_220766_.nextInt(24) == 0) {
         p_220764_.playLocalSound(
            p_220765_.getX() + 0.5,
            p_220765_.getY() + 0.5,
            p_220765_.getZ() + 0.5,
            SoundEvents.FIRE_AMBIENT,
            SoundSource.BLOCKS,
            1.0F + p_220766_.nextFloat(),
            p_220766_.nextFloat() * 0.7F + 0.3F,
            false
         );
      }

      BlockPos blockpos = p_220765_.below();
      BlockState blockstate = p_220764_.getBlockState(blockpos);
      if (!this.canBurn(blockstate) && !blockstate.isFaceSturdy(p_220764_, blockpos, Direction.UP)) {
         if (this.canBurn(p_220764_.getBlockState(p_220765_.west()))) {
            for (int j = 0; j < 2; j++) {
               double d3 = p_220765_.getX() + p_220766_.nextDouble() * 0.1F;
               double d8 = p_220765_.getY() + p_220766_.nextDouble();
               double d13 = p_220765_.getZ() + p_220766_.nextDouble();
               p_220764_.addParticle(ParticleTypes.LARGE_SMOKE, d3, d8, d13, 0.0, 0.0, 0.0);
            }
         }

         if (this.canBurn(p_220764_.getBlockState(p_220765_.east()))) {
            for (int k = 0; k < 2; k++) {
               double d4 = p_220765_.getX() + 1 - p_220766_.nextDouble() * 0.1F;
               double d9 = p_220765_.getY() + p_220766_.nextDouble();
               double d14 = p_220765_.getZ() + p_220766_.nextDouble();
               p_220764_.addParticle(ParticleTypes.LARGE_SMOKE, d4, d9, d14, 0.0, 0.0, 0.0);
            }
         }

         if (this.canBurn(p_220764_.getBlockState(p_220765_.north()))) {
            for (int l = 0; l < 2; l++) {
               double d5 = p_220765_.getX() + p_220766_.nextDouble();
               double d10 = p_220765_.getY() + p_220766_.nextDouble();
               double d15 = p_220765_.getZ() + p_220766_.nextDouble() * 0.1F;
               p_220764_.addParticle(ParticleTypes.LARGE_SMOKE, d5, d10, d15, 0.0, 0.0, 0.0);
            }
         }

         if (this.canBurn(p_220764_.getBlockState(p_220765_.south()))) {
            for (int i1 = 0; i1 < 2; i1++) {
               double d6 = p_220765_.getX() + p_220766_.nextDouble();
               double d11 = p_220765_.getY() + p_220766_.nextDouble();
               double d16 = p_220765_.getZ() + 1 - p_220766_.nextDouble() * 0.1F;
               p_220764_.addParticle(ParticleTypes.LARGE_SMOKE, d6, d11, d16, 0.0, 0.0, 0.0);
            }
         }

         if (this.canBurn(p_220764_.getBlockState(p_220765_.above()))) {
            for (int j1 = 0; j1 < 2; j1++) {
               double d7 = p_220765_.getX() + p_220766_.nextDouble();
               double d12 = p_220765_.getY() + 1 - p_220766_.nextDouble() * 0.1F;
               double d17 = p_220765_.getZ() + p_220766_.nextDouble();
               p_220764_.addParticle(ParticleTypes.LARGE_SMOKE, d7, d12, d17, 0.0, 0.0, 0.0);
            }
         }
      } else {
         for (int i = 0; i < 3; i++) {
            double d0 = p_220765_.getX() + p_220766_.nextDouble();
            double d1 = p_220765_.getY() + p_220766_.nextDouble() * 0.5 + 0.5;
            double d2 = p_220765_.getZ() + p_220766_.nextDouble();
            p_220764_.addParticle(ParticleTypes.LARGE_SMOKE, d0, d1, d2, 0.0, 0.0, 0.0);
         }
      }
   }

   protected abstract boolean canBurn(BlockState var1);

   @Override
   protected void entityInside(BlockState p_49260_, Level p_49261_, BlockPos p_49262_, Entity p_49263_, InsideBlockEffectApplier p_397865_, boolean p_432039_) {
      p_397865_.apply(InsideBlockEffectType.CLEAR_FREEZE);
      p_397865_.apply(InsideBlockEffectType.FIRE_IGNITE);
      p_397865_.runAfter(InsideBlockEffectType.FIRE_IGNITE, p_394328_ -> p_394328_.hurt(p_394328_.level().damageSources().inFire(), this.fireDamage));
   }

   public static void fireIgnite(Entity p_396191_) {
      if (!p_396191_.fireImmune()) {
         if (p_396191_.getRemainingFireTicks() < 0) {
            p_396191_.setRemainingFireTicks(p_396191_.getRemainingFireTicks() + 1);
         } else if (p_396191_ instanceof ServerPlayer) {
            int i = p_396191_.level().getRandom().nextInt(1, 3);
            p_396191_.setRemainingFireTicks(p_396191_.getRemainingFireTicks() + i);
         }

         if (p_396191_.getRemainingFireTicks() >= 0) {
            p_396191_.igniteForSeconds(8.0F);
         }
      }
   }

   @Override
   protected void onPlace(BlockState p_49279_, Level p_49280_, BlockPos p_49281_, BlockState p_49282_, boolean p_49283_) {
      if (!p_49282_.is(p_49279_.getBlock())) {
         if (inPortalDimension(p_49280_)) {
            Optional<PortalShape> optional = PortalShape.findEmptyPortalShape(p_49280_, p_49281_, Direction.Axis.X);
            if (optional.isPresent()) {
               optional.get().createPortalBlocks(p_49280_);
               return;
            }
         }

         if (!p_49279_.canSurvive(p_49280_, p_49281_)) {
            p_49280_.removeBlock(p_49281_, false);
         }
      }
   }

   private static boolean inPortalDimension(Level p_49249_) {
      return p_49249_.dimension() == Level.OVERWORLD || p_49249_.dimension() == Level.NETHER;
   }

   @Override
   protected void spawnDestroyParticles(Level p_152139_, Player p_152140_, BlockPos p_152141_, BlockState p_152142_) {
   }

   @Override
   public BlockState playerWillDestroy(Level p_49251_, BlockPos p_49252_, BlockState p_49253_, Player p_49254_) {
      if (!p_49251_.isClientSide()) {
         p_49251_.levelEvent(null, 1009, p_49252_, 0);
      }

      return super.playerWillDestroy(p_49251_, p_49252_, p_49253_, p_49254_);
   }

   public static boolean canBePlacedAt(Level p_49256_, BlockPos p_49257_, Direction p_49258_) {
      BlockState blockstate = p_49256_.getBlockState(p_49257_);
      return !blockstate.isAir() ? false : getState(p_49256_, p_49257_).canSurvive(p_49256_, p_49257_) || isPortal(p_49256_, p_49257_, p_49258_);
   }

   private static boolean isPortal(Level p_49270_, BlockPos p_49271_, Direction p_49272_) {
      if (!inPortalDimension(p_49270_)) {
         return false;
      }

      BlockPos.MutableBlockPos blockpos$mutableblockpos = p_49271_.mutable();
      boolean flag = false;

      for (Direction direction : Direction.values()) {
         if (p_49270_.getBlockState(blockpos$mutableblockpos.set(p_49271_).move(direction)).is(Blocks.OBSIDIAN)) {
            flag = true;
            break;
         }
      }

      if (!flag) {
         return false;
      }

      Direction.Axis direction$axis = p_49272_.getAxis().isHorizontal()
         ? p_49272_.getCounterClockWise().getAxis()
         : Direction.Plane.HORIZONTAL.getRandomAxis(p_49270_.random);
      return PortalShape.findEmptyPortalShape(p_49270_, p_49271_, direction$axis).isPresent();
   }
}
