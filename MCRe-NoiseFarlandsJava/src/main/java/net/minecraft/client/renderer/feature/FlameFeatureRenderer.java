package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.util.LightCoordsUtil;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

@OnlyIn(Dist.CLIENT)
public class FlameFeatureRenderer extends RenderTypeFeatureRenderer<FlameFeatureRenderer.Submit> {
    public static final FeatureRendererType<FlameFeatureRenderer.Submit> TYPE = FeatureRendererType.create("Flame");

    @Override
    protected void buildGroup(final FeatureFrameContext context, final List<FlameFeatureRenderer.Submit> submits) {
        VertexConsumer builder = this.getVertexBuilder(RenderTypes.entityCutoutCull(TextureAtlas.LOCATION_BLOCKS));
        TextureAtlasSprite fire1 = context.atlasManager().get(ModelBakery.FIRE_0);
        TextureAtlasSprite fire2 = context.atlasManager().get(ModelBakery.FIRE_1);

        for (FlameFeatureRenderer.Submit submit : submits) {
            this.prepare(submit, builder, fire1, fire2);
        }
    }

    private void prepare(final FlameFeatureRenderer.Submit submit, final VertexConsumer buffer, final TextureAtlasSprite fire1, final TextureAtlasSprite fire2) {
        PoseStack.Pose pose = submit.pose();
        EntityRenderState state = submit.entityRenderState();
        float s = state.boundingBoxWidth * 1.4F;
        pose.scale(s, s, s);
        float r = 0.5F;
        float xo = 0.0F;
        float h = state.boundingBoxHeight / s;
        float yo = 0.0F;
        pose.rotate(submit.rotation());
        pose.translate(0.0F, 0.0F, 0.3F - (int)h * 0.02F);
        float zo = 0.0F;
        int ss = 0;
        int lightCoords = LightCoordsUtil.withBlock(state.lightCoords, 15);

        while (h > 0.0F) {
            TextureAtlasSprite tex = ss % 2 == 0 ? fire1 : fire2;
            float u0 = tex.getU0();
            float v0 = tex.getV0();
            float u1 = tex.getU1();
            float v1 = tex.getV1();
            if (ss / 2 % 2 == 0) {
                float tmp = u1;
                u1 = u0;
                u0 = tmp;
            }

            fireVertex(pose, buffer, -r - 0.0F, 0.0F - yo, zo, u1, v1, lightCoords);
            fireVertex(pose, buffer, r - 0.0F, 0.0F - yo, zo, u0, v1, lightCoords);
            fireVertex(pose, buffer, r - 0.0F, 1.4F - yo, zo, u0, v0, lightCoords);
            fireVertex(pose, buffer, -r - 0.0F, 1.4F - yo, zo, u1, v0, lightCoords);
            h -= 0.45F;
            yo -= 0.45F;
            r *= 0.9F;
            zo -= 0.03F;
            ss++;
        }
    }

    private static void fireVertex(
        final PoseStack.Pose pose,
        final VertexConsumer buffer,
        final float x,
        final float y,
        final float z,
        final float u,
        final float v,
        final int lightCoords
    ) {
        buffer.addVertex(pose, x, y, z).setColor(-1).setUv(u, v).setUv1(0, 10).setLight(lightCoords).setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    @OnlyIn(Dist.CLIENT)
    public record Submit(PoseStack.Pose pose, EntityRenderState entityRenderState, Quaternionf rotation) implements SubmitNode {
        @Override
        public FeatureRendererType<FlameFeatureRenderer.Submit> featureType() {
            return FlameFeatureRenderer.TYPE;
        }
    }
}