package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

@OnlyIn(Dist.CLIENT)
public class LeashFeatureRenderer extends RenderTypeFeatureRenderer<LeashFeatureRenderer.Submit> {
    public static final FeatureRendererType<LeashFeatureRenderer.Submit> TYPE = FeatureRendererType.create("Leash");
    private static final int LEASH_RENDER_STEPS = 24;
    private static final float LEASH_WIDTH = 0.05F;

    @Override
    protected void buildGroup(final FeatureFrameContext context, final List<LeashFeatureRenderer.Submit> submits) {
        for (LeashFeatureRenderer.Submit submit : submits) {
            this.prepare(submit);
        }
    }

    private void prepare(final LeashFeatureRenderer.Submit submit) {
        VertexConsumer builder = this.getVertexBuilder(RenderTypes.leash());
        Matrix4f pose = submit.pose();
        EntityRenderState.LeashState leashState = submit.leashState();
        float dx = (float)(leashState.end.x - leashState.start.x);
        float dy = (float)(leashState.end.y - leashState.start.y);
        float dz = (float)(leashState.end.z - leashState.start.z);
        float offsetFactor = Mth.invSqrt(dx * dx + dz * dz) * 0.05F / 2.0F;
        float dxOff = dz * offsetFactor;
        float dzOff = dx * offsetFactor;
        pose.translate((float)leashState.offset.x, (float)leashState.offset.y, (float)leashState.offset.z);

        for (int k = 0; k <= 24; k++) {
            addVertexPair(builder, pose, dx, dy, dz, 0.05F, dxOff, dzOff, k, false, leashState);
        }

        for (int k = 24; k >= 0; k--) {
            addVertexPair(builder, pose, dx, dy, dz, 0.0F, dxOff, dzOff, k, true, leashState);
        }
    }

    private static void addVertexPair(
        final VertexConsumer builder,
        final Matrix4fc pose,
        final float dx,
        final float dy,
        final float dz,
        final float fudge,
        final float dxOff,
        final float dzOff,
        final int k,
        final boolean backwards,
        final EntityRenderState.LeashState state
    ) {
        float progress = k / 24.0F;
        int block = (int)Mth.lerp(progress, state.startBlockLight, state.endBlockLight);
        int sky = (int)Mth.lerp(progress, state.startSkyLight, state.endSkyLight);
        int lightCoords = LightCoordsUtil.pack(block, sky);
        float colorModifier = k % 2 == (backwards ? 1 : 0) ? 0.7F : 1.0F;
        float r = 0.5F * colorModifier;
        float g = 0.4F * colorModifier;
        float b = 0.3F * colorModifier;
        float x = dx * progress;
        float y;
        if (state.slack) {
            y = dy > 0.0F ? dy * progress * progress : dy - dy * (1.0F - progress) * (1.0F - progress);
        } else {
            y = dy * progress;
        }

        float z = dz * progress;
        builder.addVertex(pose, x - dxOff, y + fudge, z + dzOff).setColor(r, g, b, 1.0F).setLight(lightCoords);
        builder.addVertex(pose, x + dxOff, y + 0.05F - fudge, z - dzOff).setColor(r, g, b, 1.0F).setLight(lightCoords);
    }

    @OnlyIn(Dist.CLIENT)
    public record Submit(Matrix4f pose, EntityRenderState.LeashState leashState) implements SubmitNode {
        @Override
        public FeatureRendererType<LeashFeatureRenderer.Submit> featureType() {
            return LeashFeatureRenderer.TYPE;
        }
    }
}