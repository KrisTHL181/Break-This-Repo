package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DragonFireballRenderer extends EntityRenderer<DragonFireball, EntityRenderState> {
    private static final Identifier TEXTURE_LOCATION = Identifier.withDefaultNamespace("textures/entity/enderdragon/dragon_fireball.png");
    private static final RenderType RENDER_TYPE = RenderTypes.entityCutout(TEXTURE_LOCATION);

    public DragonFireballRenderer(final EntityRendererProvider.Context context) {
        super(context);
    }

    protected int getBlockLightLevel(final DragonFireball entity, final BlockPos blockPos) {
        return 15;
    }

    @Override
    public void submit(final EntityRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.scale(2.0F, 2.0F, 2.0F);
        poseStack.mulPose(camera.orientation);
        submitNodeCollector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, buffer) -> buildQuad(state, pose, buffer, -1));
        if (state.outlineColor != 0 && RENDER_TYPE.outline().isPresent()) {
            submitNodeCollector.submitCustomGeometry(
                poseStack, RENDER_TYPE.outline().get(), (pose, buffer) -> buildQuad(state, pose, buffer, state.outlineColor)
            );
        }

        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    private static void buildQuad(final EntityRenderState state, final PoseStack.Pose pose, final VertexConsumer buffer, final int color) {
        vertex(buffer, pose, state.lightCoords, 0.0F, 0, 0, 1, color);
        vertex(buffer, pose, state.lightCoords, 1.0F, 0, 1, 1, color);
        vertex(buffer, pose, state.lightCoords, 1.0F, 1, 1, 0, color);
        vertex(buffer, pose, state.lightCoords, 0.0F, 1, 0, 0, color);
    }

    private static void vertex(
        final VertexConsumer builder, final PoseStack.Pose pose, final int lightCoords, final float x, final int y, final int u, final int v, final int color
    ) {
        builder.addVertex(pose, x - 0.5F, y - 0.25F, 0.0F)
            .setColor(color)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(lightCoords)
            .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }
}