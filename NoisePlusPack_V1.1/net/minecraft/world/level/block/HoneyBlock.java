package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
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
   public static final MapCodec<HoneyBlock> CODEC = simpleCodec(HoneyBlock::new);
   private static final double SLIDE_STARTS_WHEN_VERTICAL_SPEED_IS_AT_LEAST = 0.13;
   private static final double MIN_FALL_SPEED_TO_BE_CONSIDERED_SLIDING = 0.08;
   private static final double THROTTLE_SLIDE_SPEED_TO = 0.05;
   private static final int SLIDE_ADVANCEMENT_CHECK_INTERVAL = 20;
   private static final VoxelShape SHAPE = Block.column(14.0, 0.0, 15.0);

   @Override
   public MapCodec<HoneyBlock> codec() {
      return CODEC;
   }

   public HoneyBlock(BlockBehaviour.Properties p_53985_) {
      super(p_53985_);
   }

   private static boolean doesEntityDoHoneyBlockSlideEffects(Entity p_54013_) {
      return p_54013_ instanceof LivingEntity || p_54013_ instanceof AbstractMinecart || p_54013_ instanceof PrimedTnt || p_54013_ instanceof AbstractBoat;
   }

   @Override
   protected VoxelShape getCollisionShape(BlockState p_54015_, BlockGetter p_54016_, BlockPos p_54017_, CollisionContext p_54018_) {
      return SHAPE;
   }

   @Override
   public void fallOn(Level p_153372_, BlockState p_153373_, BlockPos p_153374_, Entity p_153375_, double p_394083_) {
      p_153375_.playSound(SoundEvents.HONEY_BLOCK_SLIDE, 1.0F, 1.0F);
      if (!p_153372_.isClientSide()) {
         p_153372_.broadcastEntityEvent(p_153375_, (byte)54);
      }

      if (p_153375_.causeFallDamage(p_394083_, 0.2F, p_153372_.damageSources().fall())) {
         p_153375_.playSound(this.soundType.getFallSound(), this.soundType.getVolume() * 0.5F, this.soundType.getPitch() * 0.75F);
      }
   }

   @Override
   protected void entityInside(BlockState p_54003_, Level p_54004_, BlockPos p_54005_, Entity p_54006_, InsideBlockEffectApplier p_395723_, boolean p_432034_) {
      if (this.isSlidingDown(p_54005_, p_54006_)) {
         this.maybeDoSlideAchievement(p_54006_, p_54005_);
         this.doSlideMovement(p_54006_);
         this.maybeDoSlideEffects(p_54004_, p_54006_);
      }

      super.entityInside(p_54003_, p_54004_, p_54005_, p_54006_, p_395723_, p_432034_);
   }

   private static double getOldDeltaY(double p_368591_) {
      return p_368591_ / 0.98F + 0.08;
   }

   private static double getNewDeltaY(double p_369047_) {
      return (p_369047_ - 0.08) * 0.98F;
   }

   private boolean isSlidingDown(BlockPos p_54008_, Entity p_54009_) {
      if (p_54009_.onGround()) {
         return false;
      }

      if (p_54009_.getY() > p_54008_.getY() + 0.9375 - 1.0E-7) {
         return false;
      }

      if (getOldDeltaY(p_54009_.getDeltaMovement().y) >= -0.08) {
         return false;
      }

      double d0 = Math.abs(p_54008_.getX() + 0.5 - p_54009_.getX());
      double d1 = Math.abs(p_54008_.getZ() + 0.5 - p_54009_.getZ());
      double d2 = 0.4375 + p_54009_.getBbWidth() / 2.0F;
      return d0 + 1.0E-7 > d2 || d1 + 1.0E-7 > d2;
   }

   private void maybeDoSlideAchievement(Entity p_53992_, BlockPos p_53993_) {
      if (p_53992_ instanceof ServerPlayer && p_53992_.level().getGameTime() % 20L == 0L) {
         CriteriaTriggers.HONEY_BLOCK_SLIDE.trigger((ServerPlayer)p_53992_, p_53992_.level().getBlockState(p_53993_));
      }
   }

   private void doSlideMovement(Entity p_54020_) {
      Vec3 vec3 = p_54020_.getDeltaMovement();
      if (getOldDeltaY(p_54020_.getDeltaMovement().y) < -0.13) {
         double d0 = -0.05 / getOldDeltaY(p_54020_.getDeltaMovement().y);
         p_54020_.setDeltaMovement(new Vec3(vec3.x * d0, getNewDeltaY(-0.05), vec3.z * d0));
      } else {
         p_54020_.setDeltaMovement(new Vec3(vec3.x, getNewDeltaY(-0.05), vec3.z));
      }

      p_54020_.resetFallDistance();
   }

   private void maybeDoSlideEffects(Level p_53995_, Entity p_53996_) {
      if (doesEntityDoHoneyBlockSlideEffects(p_53996_)) {
         if (p_53995_.random.nextInt(5) == 0) {
            p_53996_.playSound(SoundEvents.HONEY_BLOCK_SLIDE, 1.0F, 1.0F);
         }

         if (!p_53995_.isClientSide() && p_53995_.random.nextInt(5) == 0) {
            p_53995_.broadcastEntityEvent(p_53996_, (byte)53);
         }
      }
   }

   public static void showSlideParticles(Entity p_53987_) {
      showParticles(p_53987_, 5);
   }

   public static void showJumpParticles(Entity p_54011_) {
      showParticles(p_54011_, 10);
   }

   private static void showParticles(Entity p_53989_, int p_53990_) {
      if (p_53989_.level().isClientSide()) {
         BlockState blockstate = Blocks.HONEY_BLOCK.defaultBlockState();

         for (int i = 0; i < p_53990_; i++) {
            p_53989_.level()
               .addParticle(new BlockParticleOption(ParticleTypes.BLOCK, blockstate), p_53989_.getX(), p_53989_.getY(), p_53989_.getZ(), 0.0, 0.0, 0.0);
         }
      }
   }
}
