package net.minecraft.world.entity.ai.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

public class DolphinJumpGoal extends JumpGoal {
   private static final int[] STEPS_TO_CHECK = new int[]{0, 1, 4, 5, 6, 7};
   private final Dolphin dolphin;
   private final int interval;
   private boolean breached;

   public DolphinJumpGoal(Dolphin p_456558_, int p_25169_) {
      this.dolphin = p_456558_;
      this.interval = reducedTickDelay(p_25169_);
   }

   @Override
   public boolean canUse() {
      if (this.dolphin.getRandom().nextInt(this.interval) != 0) {
         return false;
      }

      Direction direction = this.dolphin.getMotionDirection();
      int i = direction.getStepX();
      int j = direction.getStepZ();
      BlockPos blockpos = this.dolphin.blockPosition();

      for (int k : STEPS_TO_CHECK) {
         if (!this.waterIsClear(blockpos, i, j, k) || !this.surfaceIsClear(blockpos, i, j, k)) {
            return false;
         }
      }

      return true;
   }

   private boolean waterIsClear(BlockPos p_25173_, int p_25174_, int p_25175_, int p_25176_) {
      BlockPos blockpos = p_25173_.offset(p_25174_ * p_25176_, 0, p_25175_ * p_25176_);
      return this.dolphin.level().getFluidState(blockpos).is(FluidTags.WATER) && !this.dolphin.level().getBlockState(blockpos).blocksMotion();
   }

   private boolean surfaceIsClear(BlockPos p_25179_, int p_25180_, int p_25181_, int p_25182_) {
      return this.dolphin.level().getBlockState(p_25179_.offset(p_25180_ * p_25182_, 1, p_25181_ * p_25182_)).isAir()
         && this.dolphin.level().getBlockState(p_25179_.offset(p_25180_ * p_25182_, 2, p_25181_ * p_25182_)).isAir();
   }

   @Override
   public boolean canContinueToUse() {
      double d0 = this.dolphin.getDeltaMovement().y;
      return (!(d0 * d0 < 0.03F) || this.dolphin.getXRot() == 0.0F || !(Math.abs(this.dolphin.getXRot()) < 10.0F) || !this.dolphin.isInWater())
         && !this.dolphin.onGround();
   }

   @Override
   public boolean isInterruptable() {
      return false;
   }

   @Override
   public void start() {
      Direction direction = this.dolphin.getMotionDirection();
      this.dolphin.setDeltaMovement(this.dolphin.getDeltaMovement().add(direction.getStepX() * 0.6, 0.7, direction.getStepZ() * 0.6));
      this.dolphin.getNavigation().stop();
   }

   @Override
   public void stop() {
      this.dolphin.setXRot(0.0F);
   }

   @Override
   public void tick() {
      boolean flag = this.breached;
      if (!flag) {
         FluidState fluidstate = this.dolphin.level().getFluidState(this.dolphin.blockPosition());
         this.breached = fluidstate.is(FluidTags.WATER);
      }

      if (this.breached && !flag) {
         this.dolphin.playSound(SoundEvents.DOLPHIN_JUMP, 1.0F, 1.0F);
      }

      Vec3 vec3 = this.dolphin.getDeltaMovement();
      if (vec3.y * vec3.y < 0.03F && this.dolphin.getXRot() != 0.0F) {
         this.dolphin.setXRot(Mth.rotLerp(0.2F, this.dolphin.getXRot(), 0.0F));
      } else if (vec3.length() > 1.0E-5F) {
         double d0 = vec3.horizontalDistance();
         double d1 = Math.atan2(-vec3.y, d0) * 180.0 / (float) Math.PI;
         this.dolphin.setXRot((float)d1);
      }
   }
}
