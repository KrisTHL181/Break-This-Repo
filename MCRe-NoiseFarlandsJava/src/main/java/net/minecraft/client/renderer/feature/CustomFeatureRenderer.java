package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.submit.BatchableSubmit;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CustomFeatureRenderer extends RenderTypeFeatureRenderer<CustomFeatureRenderer.Submit> {
    public static final FeatureRendererType<CustomFeatureRenderer.Submit> TYPE = FeatureRendererType.create("Custom");

    @Override
    protected void buildGroup(final FeatureFrameContext context, final List<CustomFeatureRenderer.Submit> submits) {
        for (CustomFeatureRenderer.Submit submit : submits) {
            VertexConsumer builder = this.getVertexBuilder(submit.renderType());
            submit.customGeometryRenderer().render(submit.pose(), builder);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record Submit(PoseStack.Pose pose, RenderType renderType, SubmitNodeCollector.CustomGeometryRenderer customGeometryRenderer)
        implements BatchableSubmit {
        @Override
        public Object batchKey() {
            return this.renderType;
        }

        @Override
        public FeatureRendererType<CustomFeatureRenderer.Submit> featureType() {
            return CustomFeatureRenderer.TYPE;
        }
    }
}