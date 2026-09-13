package net.minecraft.world.level.block;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.animal.frog.Tadpole;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FrogspawnBlock extends Block {
   public static final MapCodec<FrogspawnBlock> CODEC = simpleCodec(FrogspawnBlock::new);
   private static final int MIN_TADPOLES_SPAWN = 2;
   private static final int MAX_TADPOLES_SPAWN = 5;
   private static final int DEFAULT_MIN_HATCH_TICK_DELAY = 3600;
   private static final int DEFAULT_MAX_HATCH_TICK_DELAY = 12000;
   private static final VoxelShape SHAPE = Block.column(16.0, 0.0, 1.5);
   private static int minHatchTickDelay = 3600;
   private static int maxHatchTickDelay = 12000;

   @Override
   public MapCodec<FrogspawnBlock> codec() {
      return CODEC;
   }

   public FrogspawnBlock(BlockBehaviour.Properties p_221177_) {
      super(p_221177_);
   }

   @Override
   protected VoxelShape getShape(BlockState p_221199_, BlockGetter p_221200_, BlockPos p_221201_, CollisionContext p_221202_) {
      return SHAPE;
   }

   @Override
   protected boolean canSurvive(BlockState p_221209_, LevelReader p_221210_, BlockPos p_221211_) {
      return mayPlaceOn(p_221210_, p_221211_.below());
   }

   @Override
   protected void onPlace(BlockState p_221227_, Level p_221228_, BlockPos p_221229_, BlockState p_221230_, boolean p_221231_) {
      p_221228_.scheduleTick(p_221229_, this, getFrogspawnHatchDelay(p_221228_.getRandom()));
   }

   private static int getFrogspawnHatchDelay(RandomSource p_221186_) {
      return p_221186_.nextInt(minHatchTickDelay, maxHatchTickDelay);
   }

   @Override
   protected BlockState updateShape(
      BlockState p_221213_,
      LevelReader p_365824_,
      ScheduledTickAccess p_361762_,
      BlockPos p_221217_,
      Direction p_221214_,
      BlockPos p_221218_,
      BlockState p_221215_,
      RandomSource p_364719_
   ) {
      return !this.canSurvive(p_221213_, p_365824_, p_221217_)
         ? Blocks.AIR.defaultBlockState()
         : super.updateShape(p_221213_, p_365824_, p_361762_, p_221217_, p_221214_, p_221218_, p_221215_, p_364719_);
   }

   @Override
   protected void tick(BlockState p_221194_, ServerLevel p_221195_, BlockPos p_221196_, RandomSource p_221197_) {
      if (!this.canSurvive(p_221194_, p_221195_, p_221196_)) {
         this.destroyBlock(p_221195_, p_221196_);
      } else {
         this.hatchFrogspawn(p_221195_, p_221196_, p_221197_);
      }
   }

   @Override
   protected void entityInside(
      BlockState p_221204_, Level p_221205_, BlockPos p_221206_, Entity p_221207_, InsideBlockEffectApplier p_392110_, boolean p_432029_
   ) {
      if (p_221207_.getType().equals(EntityType.FALLING_BLOCK)) {
         this.destroyBlock(p_221205_, p_221206_);
      }
   }

   private static boolean mayPlaceOn(BlockGetter p_221188_, BlockPos p_221189_) {
      FluidState fluidstate = p_221188_.getFluidState(p_221189_);
      FluidState fluidstate1 = p_221188_.getFluidState(p_221189_.above());
      return fluidstate.getType() == Fluids.WATER && fluidstate1.getType() == Fluids.EMPTY;
   }

   private void hatchFrogspawn(ServerLevel p_221182_, BlockPos p_221183_, RandomSource p_221184_) {
      this.destroyBlock(p_221182_, p_221183_);
      p_221182_.playSound(null, p_221183_, SoundEvents.FROGSPAWN_HATCH, SoundSource.BLOCKS, 1.0F, 1.0F);
      this.spawnTadpoles(p_221182_, p_221183_, p_221184_);
   }

   private void destroyBlock(Level p_221191_, BlockPos p_221192_) {
      p_221191_.destroyBlock(p_221192_, false);
   }

   private void spawnTadpoles(ServerLevel p_221221_, BlockPos p_221222_, RandomSource p_221223_) {
      int i = p_221223_.nextInt(2, 6);

      for (int j = 1; j <= i; j++) {
         Tadpole tadpole = EntityType.TADPOLE.create(p_221221_, EntitySpawnReason.BREEDING);
         if (tadpole != null) {
            double d0 = p_221222_.getX() + this.getRandomTadpolePositionOffset(p_221223_);
            double d1 = p_221222_.getZ() + this.getRandomTadpolePositionOffset(p_221223_);
            int k = p_221223_.nextInt(1, 361);
            tadpole.snapTo(d0, p_221222_.getY() - 0.5, d1, k, 0.0F);
            tadpole.setPersistenceRequired();
            p_221221_.addFreshEntity(tadpole);
         }
      }
   }

   private double getRandomTadpolePositionOffset(RandomSource p_221225_) {
      double d0 = 0.2F;
      return Mth.clamp(p_221225_.nextDouble(), 0.2F, 0.7999999970197678);
   }

   @VisibleForTesting
   public static void setHatchDelay(int p_221179_, int p_221180_) {
      minHatchTickDelay = p_221179_;
      maxHatchTickDelay = p_221180_;
   }

   @VisibleForTesting
   public static void setDefaultHatchDelay() {
      minHatchTickDelay = 3600;
      maxHatchTickDelay = 12000;
   }
}
