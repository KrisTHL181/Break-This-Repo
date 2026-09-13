package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemFrameRenderer<T extends ItemFrame> extends EntityRenderer<T, ItemFrameRenderState> {
    public static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT = BlockDisplayContext.create();
    public static final int GLOW_FRAME_BRIGHTNESS = 5;
    public static final int BRIGHT_MAP_LIGHT_ADJUSTMENT = 30;
    private final BlockModelResolver blockModelResolver;
    private final ItemModelResolver itemModelResolver;
    private final MapRenderer mapRenderer;

    public ItemFrameRenderer(final EntityRendererProvider.Context context) {
        super(context);
        this.blockModelResolver = context.getBlockModelResolver();
        this.itemModelResolver = context.getItemModelResolver();
        this.mapRenderer = context.getMapRenderer();
    }

    protected int getBlockLightLevel(final T entity, final BlockPos blockPos) {
        return entity.is(EntityTypes.GLOW_ITEM_FRAME) ? Math.max(5, super.getBlockLightLevel(entity, blockPos)) : super.getBlockLightLevel(entity, blockPos);
    }

    public void submit(
        final ItemFrameRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera
    ) {
        super.submit(state, poseStack, submitNodeCollector, camera);
        poseStack.pushPose();
        Direction direction = state.direction;
        Vec3 renderOffset = this.getRenderOffset(state);
        poseStack.translate(-renderOffset.x(), -renderOffset.y(), -renderOffset.z());
        double offs = 0.46875;
        poseStack.translate(direction.getStepX() * 0.46875, direction.getStepY() * 0.46875, direction.getStepZ() * 0.46875);
        float xRot;
        float yRot;
        if (direction.getAxis().isHorizontal()) {
            xRot = 0.0F;
            yRot = 180.0F - direction.toYRot();
        } else {
            xRot = -90 * direction.getAxisDirection().getStep();
            yRot = 180.0F;
        }

        poseStack.mulPose(Axis.XP.rotationDegrees(xRot));
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        if (!state.frameModel.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(-0.5F, -0.5F, -0.5F);
            state.frameModel.submitWithZOffset(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
        }

        if (state.isInvisible) {
            poseStack.translate(0.0F, 0.0F, 0.5F);
        } else {
            poseStack.translate(0.0F, 0.0F, 0.4375F);
        }

        if (state.mapId != null) {
            int rotation = state.rotation % 4 * 2;
            poseStack.mulPose(Axis.ZP.rotationDegrees(rotation * 360.0F / 8.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            float s = 0.0078125F;
            poseStack.scale(0.0078125F, 0.0078125F, 0.0078125F);
            poseStack.translate(-64.0F, -64.0F, 0.0F);
            poseStack.translate(0.0F, 0.0F, -1.0F);
            int lightCoords = this.getLightCoords(state.isGlowFrame, 15728850, state.lightCoords);
            this.mapRenderer.render(state.mapRenderState, poseStack, submitNodeCollector, true, lightCoords);
        } else if (!state.item.isEmpty()) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(state.rotation * 360.0F / 8.0F));
            int lightVal = this.getLightCoords(state.isGlowFrame, 15728880, state.lightCoords);
            poseStack.scale(0.5F, 0.5F, 0.5F);
            state.item.submit(poseStack, submitNodeCollector, lightVal, OverlayTexture.NO_OVERLAY, state.outlineColor);
        }

        poseStack.popPose();
    }

    private int getLightCoords(final boolean isGlowFrame, final int glowLightCoords, final int originalLightCoords) {
        return isGlowFrame ? glowLightCoords : originalLightCoords;
    }

    public Vec3 getRenderOffset(final ItemFrameRenderState state) {
        return new Vec3(state.direction.getStepX() * 0.3F, -0.25, state.direction.getStepZ() * 0.3F);
    }

    protected boolean shouldShowName(final T entity, final double distanceToCameraSq) {
        return !Minecraft.getInstance().gui.hud.isHidden()
            && this.entityRenderDispatcher.crosshairPickEntity == entity
            && entity.getItem().getCustomName() != null;
    }

    protected Component getNameTag(final T entity) {
        return entity.getItem().getHoverName();
    }

    public ItemFrameRenderState createRenderState() {
        return new ItemFrameRenderState();
    }

    public void extractRenderState(final T entity, final ItemFrameRenderState state, final float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.direction = entity.getDirection();
        ItemStack itemStack = entity.getItem();
        this.itemModelResolver.updateForNonLiving(state.item, itemStack, ItemDisplayContext.FIXED, entity);
        state.rotation = entity.getRotation();
        state.isGlowFrame = entity.is(EntityTypes.GLOW_ITEM_FRAME);
        state.mapId = null;
        if (!itemStack.isEmpty()) {
            MapId framedMapId = entity.getFramedMapId(itemStack);
            if (framedMapId != null) {
                MapItemSavedData mapData = entity.level().getMapData(framedMapId);
                if (mapData != null) {
                    this.mapRenderer.extractRenderState(framedMapId, mapData, state.mapRenderState);
                    state.mapId = framedMapId;
                }
            }
        }

        if (!state.isInvisible) {
            this.blockModelResolver.updateForItemFrame(state.frameModel, state.isGlowFrame, state.mapId != null);
        } else {
            state.frameModel.clear();
        }
    }
}