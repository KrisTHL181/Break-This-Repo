package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Objects;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.object.cart.MinecartModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.state.MinecartRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;
import net.minecraft.world.entity.vehicle.minecart.OldMinecartBehavior;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractMinecartRenderer<T extends AbstractMinecart, S extends MinecartRenderState> extends EntityRenderer<T, S> {
    private static final Identifier MINECART_LOCATION = Identifier.withDefaultNamespace("textures/entity/minecart/minecart.png");
    private static final float DISPLAY_BLOCK_SCALE = 0.75F;
    public static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT = BlockDisplayContext.create();
    protected final MinecartModel model;
    private final BlockModelResolver blockModelResolver;

    public AbstractMinecartRenderer(final EntityRendererProvider.Context context, final ModelLayerLocation model) {
        super(context);
        this.shadowRadius = 0.7F;
        this.model = new MinecartModel(context.bakeLayer(model));
        this.blockModelResolver = context.getBlockModelResolver();
    }

    public void submit(final S state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera) {
        super.submit(state, poseStack, submitNodeCollector, camera);
        poseStack.pushPose();
        long seed = state.offsetSeed;
        float offsetX = (((float)(seed >> 16 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        float offsetY = (((float)(seed >> 20 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        float offsetZ = (((float)(seed >> 24 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        poseStack.translate(offsetX, offsetY, offsetZ);
        if (state.isNewRender) {
            newRender(state, poseStack);
        } else {
            oldRender(state, poseStack);
        }

        float hurt = state.hurtTime;
        if (hurt > 0.0F) {
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(hurt) * hurt * state.damageTime / 10.0F * state.hurtDir));
        }

        BlockModelRenderState displayBlockModel = state.displayBlockModel;
        if (!displayBlockModel.isEmpty()) {
            poseStack.pushPose();
            poseStack.scale(0.75F, 0.75F, 0.75F);
            poseStack.translate(-0.5F, (state.displayOffset - 8) / 16.0F, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            this.submitMinecartContents(state, displayBlockModel, poseStack, submitNodeCollector, state.lightCoords);
            poseStack.popPose();
        }

        poseStack.scale(-1.0F, -1.0F, 1.0F);
        submitNodeCollector.submitModel(this.model, state, poseStack, MINECART_LOCATION, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
        poseStack.popPose();
    }

    private static <S extends MinecartRenderState> void newRender(final S state, final PoseStack poseStack) {
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yRot));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-state.xRot));
        poseStack.translate(0.0F, 0.375F, 0.0F);
    }

    private static <S extends MinecartRenderState> void oldRender(final S state, final PoseStack poseStack) {
        double entityX = state.x;
        double entityY = state.y;
        double entityZ = state.z;
        float xRot = state.xRot;
        float rotation = state.yRot;
        if (state.posOnRail != null && state.frontPos != null && state.backPos != null) {
            Vec3 frontPos = state.frontPos;
            Vec3 backPos = state.backPos;
            poseStack.translate(state.posOnRail.x - entityX, (frontPos.y + backPos.y) / 2.0 - entityY, state.posOnRail.z - entityZ);
            Vec3 direction = backPos.add(-frontPos.x, -frontPos.y, -frontPos.z);
            if (direction.length() != 0.0) {
                direction = direction.normalize();
                rotation = (float)(Math.atan2(direction.z, direction.x) * 180.0 / Math.PI);
                xRot = (float)(Math.atan(direction.y) * 73.0);
            }
        }

        poseStack.translate(0.0F, 0.375F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - rotation));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-xRot));
    }

    public void extractRenderState(final T entity, final S state, final float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        if (entity.getBehavior() instanceof NewMinecartBehavior behavior) {
            newExtractState(entity, behavior, state, partialTicks);
            state.isNewRender = true;
        } else if (entity.getBehavior() instanceof OldMinecartBehavior behavior) {
            oldExtractState(entity, behavior, state, partialTicks);
            state.isNewRender = false;
        }

        long seed = entity.getId() * 493286711L;
        state.offsetSeed = seed * seed * 4392167121L + seed * 98761L;
        state.hurtTime = entity.getHurtTime() - partialTicks;
        state.hurtDir = entity.getHurtDir();
        state.damageTime = Math.max(entity.getDamage() - partialTicks, 0.0F);
        state.displayOffset = entity.getDisplayOffset();
        this.blockModelResolver.update(state.displayBlockModel, entity.getDisplayBlockState(), BLOCK_DISPLAY_CONTEXT);
    }

    private static <T extends AbstractMinecart, S extends MinecartRenderState> void newExtractState(
        final T entity, final NewMinecartBehavior behavior, final S state, final float partialTicks
    ) {
        if (behavior.cartHasPosRotLerp()) {
            state.renderPos = behavior.getCartLerpPosition(partialTicks);
            state.xRot = behavior.getCartLerpXRot(partialTicks);
            state.yRot = behavior.getCartLerpYRot(partialTicks);
        } else {
            state.renderPos = null;
            state.xRot = entity.getXRot();
            state.yRot = entity.getYRot();
        }
    }

    private static <T extends AbstractMinecart, S extends MinecartRenderState> void oldExtractState(
        final T entity, final OldMinecartBehavior behavior, final S state, final float partialTicks
    ) {
        float HALF_LENGTH = 0.3F;
        state.xRot = entity.getXRot(partialTicks);
        state.yRot = entity.getYRot(partialTicks);
        double entityX = state.x;
        double entityY = state.y;
        double entityZ = state.z;
        Vec3 pos = behavior.getPos(entityX, entityY, entityZ);
        if (pos != null) {
            state.posOnRail = pos;
            Vec3 p0 = behavior.getPosOffs(entityX, entityY, entityZ, 0.3F);
            Vec3 p1 = behavior.getPosOffs(entityX, entityY, entityZ, -0.3F);
            state.frontPos = Objects.requireNonNullElse(p0, pos);
            state.backPos = Objects.requireNonNullElse(p1, pos);
        } else {
            state.posOnRail = null;
            state.frontPos = null;
            state.backPos = null;
        }
    }

    protected void submitMinecartContents(
        final S state, final BlockModelRenderState blockModel, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final int lightCoords
    ) {
        blockModel.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
    }

    protected AABB getBoundingBoxForCulling(final T entity) {
        AABB aabb = super.getBoundingBoxForCulling(entity);
        return !entity.getDisplayBlockState().isAir() ? aabb.expandTowards(0.0, entity.getDisplayOffset() * 0.75F / 16.0F, 0.0) : aabb;
    }

    public Vec3 getRenderOffset(final S state) {
        Vec3 offset = super.getRenderOffset(state);
        return state.isNewRender && state.renderPos != null
            ? offset.add(state.renderPos.x - state.x, state.renderPos.y - state.y, state.renderPos.z - state.z)
            : offset;
    }
}