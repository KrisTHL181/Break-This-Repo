package net.minecraft.world.entity;

import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class InterpolationHandler {
    public static final int DEFAULT_INTERPOLATION_STEPS = 3;
    private final Entity entity;
    private int interpolationSteps;
    private final InterpolationHandler.InterpolationData interpolationData = new InterpolationHandler.InterpolationData(0, Vec3.ZERO, 0.0F, 0.0F);
    private @Nullable Vec3 previousTickPosition;
    private @Nullable Vec2 previousTickRot;
    private final @Nullable Consumer<InterpolationHandler> onInterpolationStart;

    public InterpolationHandler(final Entity entity) {
        this(entity, 3, null);
    }

    public InterpolationHandler(final Entity entity, final int interpolationSteps) {
        this(entity, interpolationSteps, null);
    }

    public InterpolationHandler(final Entity entity, final @Nullable Consumer<InterpolationHandler> onInterpolationStart) {
        this(entity, 3, onInterpolationStart);
    }

    public InterpolationHandler(final Entity entity, final int interpolationSteps, final @Nullable Consumer<InterpolationHandler> onInterpolationStart) {
        this.interpolationSteps = interpolationSteps;
        this.entity = entity;
        this.onInterpolationStart = onInterpolationStart;
    }

    public Vec3 position() {
        return this.interpolationData.steps > 0 ? this.interpolationData.position : this.entity.position();
    }

    public float yRot() {
        return this.interpolationData.steps > 0 ? this.interpolationData.yRot : this.entity.getYRot();
    }

    public float xRot() {
        return this.interpolationData.steps > 0 ? this.interpolationData.xRot : this.entity.getXRot();
    }

    public void interpolateTo(final Vec3 position, final float yRot, final float xRot) {
        if (this.interpolationSteps == 0) {
            this.entity.snapTo(position, yRot, xRot);
            this.cancel();
        } else if (!this.hasActiveInterpolation()
            || !Objects.equals(this.yRot(), yRot)
            || !Objects.equals(this.xRot(), xRot)
            || !Objects.equals(this.position(), position)) {
            this.interpolationData.steps = this.interpolationSteps;
            this.interpolationData.position = position;
            this.interpolationData.yRot = yRot;
            this.interpolationData.xRot = xRot;
            this.previousTickPosition = this.entity.position();
            this.previousTickRot = new Vec2(this.entity.getXRot(), this.entity.getYRot());
            if (this.onInterpolationStart != null) {
                this.onInterpolationStart.accept(this);
            }
        }
    }

    public boolean hasActiveInterpolation() {
        return this.interpolationData.steps > 0;
    }

    public void setInterpolationLength(final int steps) {
        this.interpolationSteps = steps;
    }

    public void interpolate() {
        if (!this.hasActiveInterpolation()) {
            this.cancel();
        } else {
            double alpha = 1.0 / this.interpolationData.steps;
            if (this.previousTickPosition != null) {
                Vec3 deltaSinceLastInterpolation = this.entity.position().subtract(this.previousTickPosition);
                if (this.entity.level().noCollision(this.entity, this.entity.makeBoundingBox(this.interpolationData.position.add(deltaSinceLastInterpolation)))
                    )
                 {
                    this.interpolationData.addDelta(deltaSinceLastInterpolation);
                }
            }

            if (this.previousTickRot != null) {
                float deltaYRotSinceLastInterpolation = this.entity.getYRot() - this.previousTickRot.y;
                float deltaXRotSinceLastInterpolation = this.entity.getXRot() - this.previousTickRot.x;
                this.interpolationData.addRotation(deltaYRotSinceLastInterpolation, deltaXRotSinceLastInterpolation);
            }

            double x = Mth.lerp(alpha, this.entity.getX(), this.interpolationData.position.x);
            double y = Mth.lerp(alpha, this.entity.getY(), this.interpolationData.position.y);
            double z = Mth.lerp(alpha, this.entity.getZ(), this.interpolationData.position.z);
            Vec3 newPosition = new Vec3(x, y, z);
            float newYRot = (float)Mth.rotLerp(alpha, this.entity.getYRot(), this.interpolationData.yRot);
            float newXRot = (float)Mth.lerp(alpha, this.entity.getXRot(), this.interpolationData.xRot);
            this.entity.setPos(newPosition);
            this.entity.setRot(newYRot, newXRot);
            this.interpolationData.decrease();
            this.previousTickPosition = newPosition;
            this.previousTickRot = new Vec2(this.entity.getXRot(), this.entity.getYRot());
        }
    }

    public void cancel() {
        this.interpolationData.steps = 0;
        this.previousTickPosition = null;
        this.previousTickRot = null;
    }

    private static class InterpolationData {
        protected int steps;
        private Vec3 position;
        private float yRot;
        private float xRot;

        private InterpolationData(final int steps, final Vec3 position, final float yRot, final float xRot) {
            this.steps = steps;
            this.position = position;
            this.yRot = yRot;
            this.xRot = xRot;
        }

        public void decrease() {
            this.steps--;
        }

        public void addDelta(final Vec3 delta) {
            this.position = this.position.add(delta);
        }

        public void addRotation(final float yRot, final float xRot) {
            this.yRot += yRot;
            this.xRot += xRot;
        }
    }
}