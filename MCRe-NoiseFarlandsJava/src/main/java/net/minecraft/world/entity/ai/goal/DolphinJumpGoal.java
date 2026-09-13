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

    public DolphinJumpGoal(final Dolphin dolphin, final int interval) {
        this.dolphin = dolphin;
        this.interval = reducedTickDelay(interval);
    }

    @Override
    public boolean canUse() {
        if (this.dolphin.getRandom().nextInt(this.interval) != 0) {
            return false;
        }

        Direction motion = this.dolphin.getMotionDirection();
        int stepX = motion.getStepX();
        int stepZ = motion.getStepZ();
        BlockPos dolphinPos = this.dolphin.blockPosition();

        for (int i : STEPS_TO_CHECK) {
            if (!this.waterIsClear(dolphinPos, stepX, stepZ, i) || !this.surfaceIsClear(dolphinPos, stepX, stepZ, i)) {
                return false;
            }
        }

        return true;
    }

    private boolean waterIsClear(final BlockPos dolphinPos, final int stepX, final int stepZ, final int currentStep) {
        BlockPos nextPos = dolphinPos.offset(stepX * currentStep, 0, stepZ * currentStep);
        return this.dolphin.level().getFluidState(nextPos).is(FluidTags.WATER) && !this.dolphin.level().getBlockState(nextPos).blocksMotion();
    }

    private boolean surfaceIsClear(final BlockPos dolphinPos, final int stepX, final int stepZ, final int currentStep) {
        return this.dolphin.level().getBlockState(dolphinPos.offset(stepX * currentStep, 1, stepZ * currentStep)).isAir()
            && this.dolphin.level().getBlockState(dolphinPos.offset(stepX * currentStep, 2, stepZ * currentStep)).isAir();
    }

    @Override
    public boolean canContinueToUse() {
        double yd = this.dolphin.getDeltaMovement().y;
        return (!(yd * yd < 0.03F) || this.dolphin.getXRot() == 0.0F || !(Math.abs(this.dolphin.getXRot()) < 10.0F) || !this.dolphin.isInWater())
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
        boolean alreadyBreached = this.breached;
        if (!alreadyBreached) {
            FluidState fluidState = this.dolphin.level().getFluidState(this.dolphin.blockPosition());
            this.breached = fluidState.is(FluidTags.WATER);
        }

        if (this.breached && !alreadyBreached) {
            this.dolphin.playSound(SoundEvents.DOLPHIN_JUMP, 1.0F, 1.0F);
        }

        Vec3 movement = this.dolphin.getDeltaMovement();
        if (movement.y * movement.y < 0.03F && this.dolphin.getXRot() != 0.0F) {
            this.dolphin.setXRot(Mth.rotLerp(0.2F, this.dolphin.getXRot(), 0.0F));
        } else if (movement.length() > 1.0E-5F) {
            double horizontalDistance = movement.horizontalDistance();
            double rotation = Math.atan2(-movement.y, horizontalDistance) * 180.0F / (float)Math.PI;
            this.dolphin.setXRot((float)rotation);
        }
    }
}