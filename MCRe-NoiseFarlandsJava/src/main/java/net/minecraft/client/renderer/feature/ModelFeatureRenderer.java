package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.feature.submit.BatchableSubmit;
import net.minecraft.client.renderer.feature.submit.TranslucentSubmit;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ModelFeatureRenderer extends RenderTypeFeatureRenderer<ModelFeatureRenderer.Submit<?>> {
    public static final FeatureRendererType<ModelFeatureRenderer.Submit<?>> TYPE = FeatureRendererType.create("Entity Model");
    private final PoseStack poseStack = new PoseStack();

    @Override
    protected void buildGroup(final FeatureFrameContext context, final List<ModelFeatureRenderer.Submit<?>> submits) {
        for (ModelFeatureRenderer.Submit<?> submit : submits) {
            this.prepareModel(submit);
        }
    }

    private <S> void prepareModel(final ModelFeatureRenderer.Submit<S> submit) {
        this.poseStack.last().set(submit.pose());
        VertexConsumer buffer = this.getVertexBuilder(submit.renderType());
        if (submit.sheetedDecalPose() != null) {
            buffer = new SheetedDecalTextureGenerator(buffer, submit.sheetedDecalPose(), 1.0F);
        } else if (submit.sprite() != null) {
            buffer = submit.sprite().wrap(buffer);
        }

        Model<? super S> model = submit.model();
        model.setupAnim(submit.state());
        model.renderToBuffer(this.poseStack, buffer, submit.lightCoords(), submit.overlayCoords(), submit.tintedColor());
    }

    @OnlyIn(Dist.CLIENT)
    public record CrumblingOverlay(int progress, PoseStack.Pose cameraPose) {
    }

    @OnlyIn(Dist.CLIENT)
    public record Submit<S>(
        RenderType renderType,
        PoseStack.Pose pose,
        Model<? super S> model,
        S state,
        int lightCoords,
        int overlayCoords,
        int tintedColor,
        @Nullable TextureAtlasSprite sprite,
        PoseStack.@Nullable Pose sheetedDecalPose
    ) implements BatchableSubmit, TranslucentSubmit {
        @Override
        public Object batchKey() {
            return this.renderType;
        }

        @Override
        public float distanceToCameraSq() {
            return TranslucentSubmit.computeDistanceToCameraSq(this.pose.pose());
        }

        @Override
        public FeatureRendererType<ModelFeatureRenderer.Submit<S>> featureType() {
            return (FeatureRendererType<ModelFeatureRenderer.Submit<S>>)(FeatureRendererType)ModelFeatureRenderer.TYPE;
        }
    }
}
