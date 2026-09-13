package net.minecraft.client;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Arrays;
import net.minecraft.client.entity.ClientAvatarState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttributeProbe;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.waypoints.TrackedWaypoint;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class Camera implements TrackedWaypoint.Camera {
    public static final float PROJECTION_Z_NEAR = 0.05F;
    private static final float DEFAULT_CAMERA_DISTANCE = 4.0F;
    private static final Vector3fc FORWARDS = new Vector3f(0.0F, 0.0F, -1.0F);
    private static final Vector3fc UP = new Vector3f(0.0F, 1.0F, 0.0F);
    private static final Vector3fc LEFT = new Vector3f(-1.0F, 0.0F, 0.0F);
    public static final float BASE_HUD_FOV = 70.0F;
    private boolean initialized;
    private @Nullable Level level;
    private @Nullable Entity entity;
    private Vec3 position = Vec3.ZERO;
    private final BlockPos.MutableBlockPos blockPosition = new BlockPos.MutableBlockPos();
    private final Vector3f forwards = new Vector3f(FORWARDS);
    private final Vector3f panoramicForwards = new Vector3f(FORWARDS);
    private final Vector3f up = new Vector3f(UP);
    private final Vector3f left = new Vector3f(LEFT);
    private float xRot;
    private float yRot;
    private final Quaternionf rotation = new Quaternionf();
    private boolean detached;
    private float eyeHeight;
    private float eyeHeightOld;
    private final Projection projection = new Projection();
    private Frustum cullFrustum = new Frustum(new Matrix4f(), new Matrix4f());
    private @Nullable Frustum capturedFrustum;
    private boolean captureFrustum;
    private final Matrix4f cachedViewRotMatrix = new Matrix4f();
    private final Matrix4f cachedViewRotProjMatrix = new Matrix4f();
    private long lastProjectionVersion = -1L;
    private int matrixPropertiesDirty = -1;
    private static final int DIRTY_VIEW_ROT = 1;
    private static final int DIRTY_VIEW_ROT_PROJ = 2;
    private float fovModifier;
    private float oldFovModifier;
    private float fov;
    private float hudFov;
    private float depthFar;
    private boolean isPanoramicMode;
    private final EnvironmentAttributeProbe attributeProbe = new EnvironmentAttributeProbe();
    private final Minecraft minecraft = Minecraft.getInstance();

    public void tick() {
        if (this.level != null && this.entity != null) {
            this.eyeHeightOld = this.eyeHeight;
            this.eyeHeight = this.eyeHeight + (this.entity.getEyeHeight() - this.eyeHeight) * 0.5F;
            this.attributeProbe.tick(this.level, this.position);
        }

        this.tickFov();
    }

    public void update(final DeltaTracker deltaTracker) {
        float renderDistance = this.minecraft.options.getEffectiveRenderDistance() * 16;
        this.depthFar = Math.max(renderDistance * 4.0F, this.minecraft.options.cloudRange().get() * 16);
        LocalPlayer player = this.minecraft.player;
        if (player != null && this.level != null) {
            if (this.entity == null) {
                this.setEntity(player);
            }

            float partialTicks = this.getCameraEntityPartialTicks(deltaTracker);
            this.alignWithEntity(partialTicks);
            this.fov = this.calculateFov(partialTicks);
            this.hudFov = this.calculateHudFov(partialTicks);
            this.prepareCullFrustum(this.getViewRotationMatrix(this.cachedViewRotMatrix), this.createProjectionMatrixForCulling(), this.position);
            float windowWidth = this.minecraft.getWindow().getWidth();
            float windowHeight = this.minecraft.getWindow().getHeight();
            this.setupPerspective(0.05F, this.depthFar, this.fov, windowWidth, windowHeight);
            this.initialized = true;
        }
    }

    public float getCameraEntityPartialTicks(final DeltaTracker deltaTracker) {
        return this.level.tickRateManager().isEntityFrozen(this.entity) ? 1.0F : deltaTracker.getGameTimeDeltaPartialTick(true);
    }

    public void extractRenderState(final CameraRenderState cameraState, final float cameraEntityPartialTicks) {
        cameraState.initialized = this.isInitialized();
        cameraState.isPanoramicMode = this.isPanoramicMode;
        cameraState.isFrustumCaptured = this.capturedFrustum != null;
        cameraState.smartCull = this.minecraft.smartCull;
        if (this.minecraft.player.isSpectator() && this.level.getBlockState(this.blockPosition).isSolidRender()) {
            cameraState.smartCull = false;
        }

        cameraState.pos = this.position();
        cameraState.xRot = this.xRot;
        cameraState.yRot = this.yRot;
        cameraState.blockPos = this.blockPosition();
        cameraState.orientation.set(this.rotation());
        cameraState.cullFrustum.set(this.cullFrustum);
        cameraState.depthFar = this.depthFar;
        this.projection.getMatrix(cameraState.projectionMatrix);
        this.getViewRotationMatrix(cameraState.viewRotationMatrix);
        if (this.entity instanceof LivingEntity livingEntity) {
            cameraState.entityRenderState.isLiving = true;
            cameraState.entityRenderState.isSleeping = livingEntity.isSleeping();
            cameraState.entityRenderState.doesMobEffectBlockSky = livingEntity.hasEffect(MobEffects.BLINDNESS) || livingEntity.hasEffect(MobEffects.DARKNESS);
            cameraState.entityRenderState.isDeadOrDying = livingEntity.isDeadOrDying();
            cameraState.entityRenderState.hurtDir = livingEntity.getHurtDir();
            cameraState.entityRenderState.hurtTime = livingEntity.hurtTime - cameraEntityPartialTicks;
            cameraState.entityRenderState.deathTime = livingEntity.deathTime + cameraEntityPartialTicks;
            cameraState.entityRenderState.hurtDuration = livingEntity.hurtDuration;
        } else {
            cameraState.entityRenderState.isLiving = false;
            cameraState.entityRenderState.isSleeping = false;
            cameraState.entityRenderState.doesMobEffectBlockSky = false;
        }

        if (this.entity instanceof AbstractClientPlayer player) {
            cameraState.entityRenderState.isPlayer = true;
            ClientAvatarState avatarState = player.avatarState();
            cameraState.entityRenderState.backwardsInterpolatedWalkDistance = avatarState.getBackwardsInterpolatedWalkDistance(cameraEntityPartialTicks);
            cameraState.entityRenderState.bob = avatarState.getInterpolatedBob(cameraEntityPartialTicks);
        } else {
            cameraState.entityRenderState.isPlayer = false;
        }

        cameraState.hudFov = this.hudFov;
    }

    private void tickFov() {
        float targetFovModifier;
        if (this.minecraft.getCameraEntity() instanceof AbstractClientPlayer player) {
            Options options = this.minecraft.options;
            boolean firstPerson = options.getCameraType().isFirstPerson();
            float effectScale = options.fovEffectScale().get().floatValue();
            targetFovModifier = player.getFieldOfViewModifier(firstPerson, effectScale);
        } else {
            targetFovModifier = 1.0F;
        }

        this.oldFovModifier = this.fovModifier;
        this.fovModifier = this.fovModifier + (targetFovModifier - this.fovModifier) * 0.5F;
        this.fovModifier = Mth.clamp(this.fovModifier, 0.1F, 1.5F);
    }

    private Matrix4f createProjectionMatrixForCulling() {
        float fovForCulling = Math.max(this.fov, this.minecraft.options.fov().get().intValue());
        Matrix4f projection = new Matrix4f();
        return projection.perspective(
            fovForCulling * (float) (Math.PI / 180.0),
            (float)this.minecraft.getWindow().getWidth() / this.minecraft.getWindow().getHeight(),
            0.05F,
            this.depthFar,
            RenderSystem.getDevice().getDeviceInfo().isZZeroToOne()
        );
    }

    public Frustum getCullFrustum() {
        return this.cullFrustum;
    }

    private void prepareCullFrustum(final Matrix4fc modelViewMatrix, final Matrix4f projectionMatrixForCulling, final Vec3 cameraPos) {
        if (this.capturedFrustum != null && !this.captureFrustum) {
            this.cullFrustum = this.capturedFrustum;
        } else {
            this.cullFrustum = new Frustum(modelViewMatrix, projectionMatrixForCulling);
            this.cullFrustum.prepare(cameraPos.x(), cameraPos.y(), cameraPos.z());
        }

        if (this.captureFrustum) {
            this.capturedFrustum = this.cullFrustum;
            this.captureFrustum = false;
        }
    }

    public @Nullable Frustum getCapturedFrustum() {
        return this.capturedFrustum;
    }

    public void captureFrustum() {
        this.captureFrustum = true;
    }

    public void killFrustum() {
        this.capturedFrustum = null;
    }

    private float calculateFov(final float partialTicks) {
        if (this.isPanoramicMode) {
            return 90.0F;
        }

        float fov = this.minecraft.options.fov().get().intValue() * Mth.lerp(partialTicks, this.oldFovModifier, this.fovModifier);
        return this.modifyFovBasedOnDeathOrFluid(partialTicks, fov);
    }

    private float calculateHudFov(final float partialTicks) {
        return this.modifyFovBasedOnDeathOrFluid(partialTicks, 70.0F);
    }

    private float modifyFovBasedOnDeathOrFluid(final float partialTicks, float fov) {
        if (this.entity instanceof LivingEntity cameraEntity && cameraEntity.isDeadOrDying()) {
            float duration = Math.min(cameraEntity.deathTime + partialTicks, 20.0F);
            fov /= (1.0F - 500.0F / (duration + 500.0F)) * 2.0F + 1.0F;
        }

        FogType state = this.getFluidInCamera();
        if (state == FogType.LAVA || state == FogType.WATER) {
            float effectScale = this.minecraft.options.fovEffectScale().get().floatValue();
            fov *= Mth.lerp(effectScale, 1.0F, 0.85714287F);
        }

        return fov;
    }

    private void alignWithEntity(final float partialTicks) {
        if (this.entity.isPassenger()
            && this.entity.getVehicle() instanceof Minecart minecart
            && minecart.getBehavior() instanceof NewMinecartBehavior behavior
            && behavior.cartHasPosRotLerp()) {
            Vec3 positionOffset = minecart.getPassengerRidingPosition(this.entity)
                .subtract(minecart.position())
                .subtract(this.entity.getVehicleAttachmentPoint(minecart))
                .add(new Vec3(0.0, Mth.lerp(partialTicks, this.eyeHeightOld, this.eyeHeight), 0.0));
            this.setRotation(this.entity.getViewYRot(partialTicks), this.entity.getViewXRot(partialTicks));
            this.setPosition(behavior.getCartLerpPosition(partialTicks).add(positionOffset));
        } else {
            this.setRotation(this.entity.getViewYRot(partialTicks), this.entity.getViewXRot(partialTicks));
            this.setPosition(
                Mth.lerp(partialTicks, this.entity.xo, this.entity.getX()),
                Mth.lerp(partialTicks, this.entity.yo, this.entity.getY()) + Mth.lerp(partialTicks, this.eyeHeightOld, this.eyeHeight),
                Mth.lerp(partialTicks, this.entity.zo, this.entity.getZ())
            );
        }

        this.detached = !this.minecraft.options.getCameraType().isFirstPerson();
        if (this.detached) {
            if (this.minecraft.options.getCameraType().isMirrored()) {
                this.setRotation(this.yRot + 180.0F, -this.xRot);
            }

            float cameraDistance = 4.0F;
            float cameraScale = 1.0F;
            if (this.entity instanceof LivingEntity living) {
                cameraScale = living.getScale();
                cameraDistance = (float)living.getAttributeValue(Attributes.CAMERA_DISTANCE);
            }

            float mountScale = cameraScale;
            float mountDistance = cameraDistance;
            if (this.entity.isPassenger() && this.entity.getVehicle() instanceof LivingEntity mount) {
                mountScale = mount.getScale();
                mountDistance = (float)mount.getAttributeValue(Attributes.CAMERA_DISTANCE);
            }

            this.move(-this.getMaxZoom(Math.max(cameraScale * cameraDistance, mountScale * mountDistance)), 0.0F, 0.0F);
        } else if (this.entity instanceof LivingEntity && ((LivingEntity)this.entity).isSleeping()) {
            Direction bedOrientation = ((LivingEntity)this.entity).getBedOrientation();
            this.setRotation(bedOrientation != null ? bedOrientation.toYRot() - 180.0F : 0.0F, 0.0F);
            this.move(0.0F, 0.15F, 0.0F);
        }
    }

    private float getMaxZoom(float cameraDist) {
        float jitterScale = 0.1F;

        for (int i = 0; i < 8; i++) {
            float offsetX = (i & 1) * 2 - 1;
            float offsetY = (i >> 1 & 1) * 2 - 1;
            float offsetZ = (i >> 2 & 1) * 2 - 1;
            Vec3 from = this.position.add(offsetX * 0.1F, offsetY * 0.1F, offsetZ * 0.1F);
            Vec3 to = from.add(new Vec3(this.forwards).scale(-cameraDist));
            HitResult hitResult = this.level.clip(new ClipContext(from, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, this.entity));
            if (hitResult.getType() != HitResult.Type.MISS) {
                float distSq = (float)hitResult.getLocation().distanceToSqr(this.position);
                if (distSq < Mth.square(cameraDist)) {
                    cameraDist = Mth.sqrt(distSq);
                }
            }
        }

        return cameraDist;
    }

    public boolean isPanoramicMode() {
        return this.isPanoramicMode;
    }

    public float getFov() {
        return this.fov;
    }

    private void setupPerspective(final float zNear, final float zFar, final float fov, final float width, final float height) {
        this.projection.setupPerspective(zNear, zFar, fov, width, height);
    }

    private void setupOrtho(final float zNear, final float zFar, final float width, final float height, final boolean invertY) {
        this.projection.setupOrtho(zNear, zFar, width, height, invertY);
    }

    protected void move(final float forwards, final float up, final float right) {
        Vector3f offset = new Vector3f(right, up, -forwards).rotate(this.rotation);
        this.setPosition(new Vec3(this.position.x + offset.x, this.position.y + offset.y, this.position.z + offset.z));
    }

    protected void setRotation(final float yRot, final float xRot) {
        this.xRot = xRot;
        this.yRot = yRot;
        this.rotation.rotationYXZ((float) Math.PI - yRot * (float) (Math.PI / 180.0), -xRot * (float) (Math.PI / 180.0), 0.0F);
        FORWARDS.rotate(this.rotation, this.forwards);
        UP.rotate(this.rotation, this.up);
        LEFT.rotate(this.rotation, this.left);
        this.matrixPropertiesDirty |= 3;
    }

    protected void setPosition(final double x, final double y, final double z) {
        this.setPosition(new Vec3(x, y, z));
    }

    protected void setPosition(final Vec3 position) {
        this.position = position;
        this.blockPosition.set(position.x, position.y, position.z);
    }

    @Override
    public Vec3 position() {
        return this.position;
    }

    public BlockPos blockPosition() {
        return this.blockPosition;
    }

    public float xRot() {
        return this.xRot;
    }

    public float yRot() {
        return this.yRot;
    }

    @Override
    public float yaw() {
        return Mth.wrapDegrees(this.yRot());
    }

    public Quaternionf rotation() {
        return this.rotation;
    }

    public Matrix4f getViewRotationMatrix(final Matrix4f dest) {
        if ((this.matrixPropertiesDirty & 1) != 0) {
            Quaternionf inverseRotation = this.rotation().conjugate(new Quaternionf());
            this.cachedViewRotMatrix.rotation(inverseRotation);
            this.matrixPropertiesDirty &= -2;
        }

        return dest.set(this.cachedViewRotMatrix);
    }

    public Matrix4f getViewRotationProjectionMatrix(final Matrix4f dest) {
        long projectionVersion = this.projection.getMatrixVersion();
        if ((this.matrixPropertiesDirty & 2) != 0 || this.lastProjectionVersion != this.projection.getMatrixVersion()) {
            this.getViewRotationMatrix(this.cachedViewRotMatrix);
            this.projection.getMatrix(this.cachedViewRotProjMatrix);
            this.cachedViewRotProjMatrix.mul(this.cachedViewRotMatrix);
            this.matrixPropertiesDirty &= -3;
            this.lastProjectionVersion = projectionVersion;
        }

        return dest.set(this.cachedViewRotProjMatrix);
    }

    public @Nullable Entity entity() {
        return this.entity;
    }

    public void setEntity(final Entity entity) {
        this.entity = entity;
    }

    public boolean isInitialized() {
        return this.initialized;
    }

    public boolean isDetached() {
        return this.detached;
    }

    public EnvironmentAttributeProbe attributeProbe() {
        return this.attributeProbe;
    }

    public Camera.NearPlane getNearPlane(final float fov) {
        double aspectRatio = (double)this.projection.width() / this.projection.height();
        double planeHeight = Math.tan(fov * (float) (Math.PI / 180.0) / 2.0) * this.projection.zNear();
        double planeWidth = planeHeight * aspectRatio;
        Vec3 forwardsVec3 = new Vec3(this.forwards).scale(this.projection.zNear());
        Vec3 leftVec3 = new Vec3(this.left).scale(planeWidth);
        Vec3 upVec3 = new Vec3(this.up).scale(planeHeight);
        return new Camera.NearPlane(forwardsVec3, leftVec3, upVec3);
    }

    public FogType getFluidInCamera() {
        if (!this.initialized) {
            return FogType.NONE;
        }

        FluidState fluidState1 = this.level.getFluidState(this.blockPosition);
        if (fluidState1.is(FluidTags.WATER) && this.position.y < this.blockPosition.getY() + fluidState1.getHeight(this.level, this.blockPosition)) {
            return FogType.WATER;
        }

        Camera.NearPlane plane = this.getNearPlane(this.minecraft.options.fov().get().intValue());

        for (Vec3 point : Arrays.asList(plane.forward, plane.getTopLeft(), plane.getTopRight(), plane.getBottomLeft(), plane.getBottomRight())) {
            Vec3 offsetPos = this.position.add(point);
            BlockPos checkPos = BlockPos.containing(offsetPos);
            FluidState fluidState = this.level.getFluidState(checkPos);
            if (fluidState.is(FluidTags.LAVA)) {
                if (offsetPos.y <= fluidState.getHeight(this.level, checkPos) + checkPos.getY()) {
                    return FogType.LAVA;
                }
            } else {
                BlockState state = this.level.getBlockState(checkPos);
                if (state.is(Blocks.POWDER_SNOW)) {
                    return FogType.POWDER_SNOW;
                }
            }
        }

        return FogType.NONE;
    }

    public Vector3fc forwardVector() {
        return this.forwards;
    }

    public Vector3fc panoramicForwards() {
        return this.panoramicForwards;
    }

    public Vector3fc upVector() {
        return this.up;
    }

    public Vector3fc leftVector() {
        return this.left;
    }

    public void reset() {
        this.level = null;
        this.entity = null;
        this.attributeProbe.reset();
        this.initialized = false;
    }

    public void setLevel(final @Nullable ClientLevel level) {
        this.level = level;
    }

    public void enablePanoramicMode() {
        this.isPanoramicMode = true;
        this.panoramicForwards.set(this.forwards);
    }

    public void disablePanoramicMode() {
        this.isPanoramicMode = false;
    }

    @OnlyIn(Dist.CLIENT)
    public static class NearPlane {
        private final Vec3 forward;
        private final Vec3 left;
        private final Vec3 up;

        private NearPlane(final Vec3 forward, final Vec3 left, final Vec3 up) {
            this.forward = forward;
            this.left = left;
            this.up = up;
        }

        public Vec3 getTopLeft() {
            return this.forward.add(this.up).add(this.left);
        }

        public Vec3 getTopRight() {
            return this.forward.add(this.up).subtract(this.left);
        }

        public Vec3 getBottomLeft() {
            return this.forward.subtract(this.up).add(this.left);
        }

        public Vec3 getBottomRight() {
            return this.forward.subtract(this.up).subtract(this.left);
        }

        public Vec3 getPointOnPlane(final float x, final float y) {
            return this.forward.add(this.up.scale(y)).subtract(this.left.scale(x));
        }
    }
}