package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.feature.submit.BatchableSubmit;
import net.minecraft.client.renderer.feature.submit.TranslucentSubmit;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.UvMapping;
import org.jspecify.annotations.Nullable;

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
      } else if (submit.uvMapping() != null) {
         buffer = submit.uvMapping().wrap(buffer);
      }

      Model<? super S> model = submit.model();
      model.setupAnim(submit.state());
      model.renderToBuffer(this.poseStack, buffer, submit.lightCoords(), submit.overlayCoords(), submit.tintedColor());
   }

   public record CrumblingOverlay(int progress, PoseStack.Pose cameraPose) {
   }

   public record Submit<S>(
      RenderType renderType,
      PoseStack.Pose pose,
      Model<? super S> model,
      S state,
      int lightCoords,
      int overlayCoords,
      int tintedColor,
      @Nullable UvMapping uvMapping,
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
         return (FeatureRendererType<ModelFeatureRenderer.Submit<S>>)ModelFeatureRenderer.TYPE;
      }
   }
}
