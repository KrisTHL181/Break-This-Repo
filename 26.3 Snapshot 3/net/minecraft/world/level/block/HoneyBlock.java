package net.minecraft.world.level.block;

import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class HoneyBlock extends HalfTransparentBlock {
   private static final double SLIDE_STARTS_WHEN_VERTICAL_SPEED_IS_AT_LEAST = 0.13;
   private static final double MIN_FALL_SPEED_TO_BE_CONSIDERED_SLIDING = 0.08;
   private static final double THROTTLE_SLIDE_SPEED_TO = 0.05;
   private static final int SLIDE_ADVANCEMENT_CHECK_INTERVAL = 20;
   private static final VoxelShape SHAPE = Block.column(14.0, 0.0, 15.0);

   public HoneyBlock(final BlockBehaviour.Properties properties) {
      super(properties);
   }

   private static boolean doesEntityDoHoneyBlockSlideEffects(final Entity entity) {
      return entity instanceof LivingEntity || entity instanceof AbstractMinecart || entity instanceof PrimedTnt || entity instanceof AbstractBoat;
   }

   @Override
   protected VoxelShape getCollisionShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
      return SHAPE;
   }

   @Override
   public void fallOn(final Level level, final BlockState state, final BlockPos pos, final Entity entity, final double fallDistance) {
      entity.playSound(SoundEvents.HONEY_BLOCK_SLIDE, 1.0F, 1.0F);
      if (!level.isClientSide()) {
         level.broadcastEntityEvent(entity, (byte)54);
      }

      if (entity.causeFallDamage(fallDistance, 0.2F, level.damageSources().fall())) {
         entity.playSound(this.soundType.getFallSound(), this.soundType.getVolume() * 0.5F, this.soundType.getPitch() * 0.75F);
      }
   }

   @Override
   protected void entityInside(
      final BlockState state, final Level level, final BlockPos pos, final Entity entity, final InsideBlockEffectApplier effectApplier, final boolean isPrecise
   ) {
      if (this.isSlidingDown(pos, entity)) {
         this.maybeDoSlideAchievement(entity, pos);
         this.doSlideMovement(entity);
         this.maybeDoSlideEffects(level, entity);
      }

      super.entityInside(state, level, pos, entity, effectApplier, isPrecise);
   }

   private static double getOldDeltaY(final double deltaY) {
      return deltaY / 0.98F + 0.08;
   }

   private static double getNewDeltaY(final double deltaY) {
      return (deltaY - 0.08) * 0.98F;
   }

   private boolean isSlidingDown(final BlockPos pos, final Entity entity) {
      if (entity.onGround()) {
         return false;
      }

      if (entity.getY() > pos.getY() + 0.9375 - 1.0E-7) {
         return false;
      }

      if (getOldDeltaY(entity.getDeltaMovement().y) >= -0.08) {
         return false;
      }

      double dx = Math.abs(pos.getX() + 0.5 - entity.getX());
      double dz = Math.abs(pos.getZ() + 0.5 - entity.getZ());
      double overlapDistance = 0.4375 + entity.getBbWidth() / 2.0F;
      return dx + 1.0E-7 > overlapDistance || dz + 1.0E-7 > overlapDistance;
   }

   private void maybeDoSlideAchievement(final Entity entity, final BlockPos pos) {
      if (entity instanceof ServerPlayer serverPlayer && entity.level().getGameTime() % 20L == 0L) {
         CriteriaTriggers.HONEY_BLOCK_SLIDE.trigger(serverPlayer, entity.level().getBlockState(pos));
      }
   }

   private void doSlideMovement(final Entity entity) {
      Vec3 deltaMovement = entity.getDeltaMovement();
      if (getOldDeltaY(entity.getDeltaMovement().y) < -0.13) {
         double horizontalReductionFactor = -0.05 / getOldDeltaY(entity.getDeltaMovement().y);
         entity.setDeltaMovement(new Vec3(deltaMovement.x * horizontalReductionFactor, getNewDeltaY(-0.05), deltaMovement.z * horizontalReductionFactor));
      } else {
         entity.setDeltaMovement(new Vec3(deltaMovement.x, getNewDeltaY(-0.05), deltaMovement.z));
      }

      entity.resetFallDistance();
   }

   private void maybeDoSlideEffects(final Level level, final Entity entity) {
      if (doesEntityDoHoneyBlockSlideEffects(entity)) {
         RandomSource random = level.getRandom();
         if (random.nextInt(5) == 0) {
            entity.playSound(SoundEvents.HONEY_BLOCK_SLIDE, 1.0F, 1.0F);
         }

         if (!level.isClientSide() && random.nextInt(5) == 0) {
            level.broadcastEntityEvent(entity, (byte)53);
         }
      }
   }

   public static void showSlideParticles(final Entity entity) {
      showParticles(entity, 5);
   }

   public static void showJumpParticles(final Entity entity) {
      showParticles(entity, 10);
   }

   private static void showParticles(final Entity entity, final int count) {
      if (entity.level().isClientSide()) {
         BlockState blockState = Blocks.HONEY_BLOCK.defaultBlockState();

         for (int i = 0; i < count; i++) {
            entity.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, blockState), entity.getX(), entity.getY(), entity.getZ(), 0.0, 0.0, 0.0);
         }
      }
   }
}
