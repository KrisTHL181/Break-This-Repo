package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.ExperienceOrbRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ExperienceOrbRenderer extends EntityRenderer<ExperienceOrb, ExperienceOrbRenderState> {
    private static final Identifier EXPERIENCE_ORB_LOCATION = Identifier.withDefaultNamespace("textures/entity/experience/experience_orb.png");
    private static final RenderType RENDER_TYPE = RenderTypes.entityTranslucentCullItemTarget(EXPERIENCE_ORB_LOCATION);

    public ExperienceOrbRenderer(final EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.15F;
        this.shadowStrength = 0.75F;
    }

    protected int getBlockLightLevel(final ExperienceOrb entity, final BlockPos blockPos) {
        return Mth.clamp(super.getBlockLightLevel(entity, blockPos) + 7, 0, 15);
    }

    public void submit(
        final ExperienceOrbRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera
    ) {
        poseStack.pushPose();
        int icon = state.icon;
        float u0 = (icon % 4 * 16 + 0) / 64.0F;
        float u1 = (icon % 4 * 16 + 16) / 64.0F;
        float v0 = (icon / 4 * 16 + 0) / 64.0F;
        float v1 = (icon / 4 * 16 + 16) / 64.0F;
        float r = 1.0F;
        float xo = 0.5F;
        float yo = 0.25F;
        float br = 255.0F;
        float rr = state.ageInTicks / 2.0F;
        int rc = (int)((Mth.sin(rr + 0.0F) + 1.0F) * 0.5F * 255.0F);
        int gc = 255;
        int bc = (int)((Mth.sin(rr + (float) (Math.PI * 4.0 / 3.0)) + 1.0F) * 0.1F * 255.0F);
        poseStack.translate(0.0F, 0.1F, 0.0F);
        poseStack.mulPose(camera.orientation);
        float s = 0.3F;
        poseStack.scale(0.3F, 0.3F, 0.3F);
        submitNodeCollector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, buffer) -> {
            vertex(buffer, pose, -0.5F, -0.25F, rc, 255, bc, u0, v1, state.lightCoords);
            vertex(buffer, pose, 0.5F, -0.25F, rc, 255, bc, u1, v1, state.lightCoords);
            vertex(buffer, pose, 0.5F, 0.75F, rc, 255, bc, u1, v0, state.lightCoords);
            vertex(buffer, pose, -0.5F, 0.75F, rc, 255, bc, u0, v0, state.lightCoords);
        });
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    private static void vertex(
        final VertexConsumer buffer,
        final PoseStack.Pose pose,
        final float x,
        final float y,
        final int r,
        final int g,
        final int b,
        final float u,
        final float v,
        final int lightCoords
    ) {
        buffer.addVertex(pose, x, y, 0.0F)
            .setColor(r, g, b, 128)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(lightCoords)
            .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    public ExperienceOrbRenderState createRenderState() {
        return new ExperienceOrbRenderState();
    }

    public void extractRenderState(final ExperienceOrb entity, final ExperienceOrbRenderState state, final float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.icon = entity.getIcon();
    }
}