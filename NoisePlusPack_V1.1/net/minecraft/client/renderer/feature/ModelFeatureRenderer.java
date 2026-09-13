package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.SharedConstants;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class ModelFeatureRenderer {
   private final PoseStack poseStack = new PoseStack();

   public void render(
      SubmitNodeCollection p_424122_, MultiBufferSource.BufferSource p_425574_, OutlineBufferSource p_424002_, MultiBufferSource.BufferSource p_428766_
   ) {
      ModelFeatureRenderer.Storage modelfeaturerenderer$storage = p_424122_.getModelSubmits();
      this.renderBatch(p_425574_, p_424002_, modelfeaturerenderer$storage.opaqueModelSubmits, p_428766_);
      modelfeaturerenderer$storage.translucentModelSubmits.sort(Comparator.comparingDouble(p_426282_ -> -p_426282_.position().lengthSquared()));
      this.renderTranslucents(p_425574_, p_424002_, modelfeaturerenderer$storage.translucentModelSubmits, p_428766_);
   }

   private void renderTranslucents(
      MultiBufferSource.BufferSource p_425035_,
      OutlineBufferSource p_424488_,
      List<SubmitNodeStorage.TranslucentModelSubmit<?>> p_423495_,
      MultiBufferSource.BufferSource p_430495_
   ) {
      for (SubmitNodeStorage.TranslucentModelSubmit<?> translucentmodelsubmit : p_423495_) {
         this.renderModel(
            translucentmodelsubmit.modelSubmit(),
            translucentmodelsubmit.renderType(),
            p_425035_.getBuffer(translucentmodelsubmit.renderType()),
            p_424488_,
            p_430495_
         );
      }
   }

   private void renderBatch(
      MultiBufferSource.BufferSource p_429497_,
      OutlineBufferSource p_425494_,
      Map<RenderType, List<SubmitNodeStorage.ModelSubmit<?>>> p_425382_,
      MultiBufferSource.BufferSource p_430884_
   ) {
      Iterable<Entry<RenderType, List<SubmitNodeStorage.ModelSubmit<?>>>> iterable;
      if (SharedConstants.DEBUG_SHUFFLE_MODELS) {
         List<Entry<RenderType, List<SubmitNodeStorage.ModelSubmit<?>>>> list = new ArrayList<>(p_425382_.entrySet());
         Collections.shuffle(list);
         iterable = list;
      } else {
         iterable = p_425382_.entrySet();
      }

      for (Entry<RenderType, List<SubmitNodeStorage.ModelSubmit<?>>> entry : iterable) {
         VertexConsumer vertexconsumer = p_429497_.getBuffer(entry.getKey());

         for (SubmitNodeStorage.ModelSubmit<?> modelsubmit : entry.getValue()) {
            this.renderModel(modelsubmit, entry.getKey(), vertexconsumer, p_425494_, p_430884_);
         }
      }
   }

   private <S> void renderModel(
      SubmitNodeStorage.ModelSubmit<S> p_426187_,
      RenderType p_450242_,
      VertexConsumer p_428121_,
      OutlineBufferSource p_422455_,
      MultiBufferSource.BufferSource p_428832_
   ) {
      this.poseStack.pushPose();
      this.poseStack.last().set(p_426187_.pose());
      Model<? super S> model = p_426187_.model();
      VertexConsumer vertexconsumer = p_426187_.sprite() == null ? p_428121_ : p_426187_.sprite().wrap(p_428121_);
      model.setupAnim(p_426187_.state());
      model.renderToBuffer(this.poseStack, vertexconsumer, p_426187_.lightCoords(), p_426187_.overlayCoords(), p_426187_.tintedColor());
      if (p_426187_.outlineColor() != 0 && (p_450242_.outline().isPresent() || p_450242_.isOutline())) {
         p_422455_.setColor(p_426187_.outlineColor());
         VertexConsumer vertexconsumer1 = p_422455_.getBuffer(p_450242_);
         model.renderToBuffer(
            this.poseStack,
            p_426187_.sprite() == null ? vertexconsumer1 : p_426187_.sprite().wrap(vertexconsumer1),
            p_426187_.lightCoords(),
            p_426187_.overlayCoords(),
            p_426187_.tintedColor()
         );
      }

      if (p_426187_.crumblingOverlay() != null && p_450242_.affectsCrumbling()) {
         VertexConsumer vertexconsumer2 = new SheetedDecalTextureGenerator(
            p_428832_.getBuffer(ModelBakery.DESTROY_TYPES.get(p_426187_.crumblingOverlay().progress())), p_426187_.crumblingOverlay().cameraPose(), 1.0F
         );
         model.renderToBuffer(
            this.poseStack,
            p_426187_.sprite() == null ? vertexconsumer2 : p_426187_.sprite().wrap(vertexconsumer2),
            p_426187_.lightCoords(),
            p_426187_.overlayCoords(),
            p_426187_.tintedColor()
         );
      }

      this.poseStack.popPose();
   }

   @OnlyIn(Dist.CLIENT)
   public record CrumblingOverlay(int progress, PoseStack.Pose cameraPose) {
   }

   @OnlyIn(Dist.CLIENT)
   public static class Storage {
      final Map<RenderType, List<SubmitNodeStorage.ModelSubmit<?>>> opaqueModelSubmits = new HashMap<>();
      final List<SubmitNodeStorage.TranslucentModelSubmit<?>> translucentModelSubmits = new ArrayList<>();
      private final Set<RenderType> usedModelSubmitBuckets = new ObjectOpenHashSet();

      public void add(RenderType p_457687_, SubmitNodeStorage.ModelSubmit<?> p_428367_) {
         if (p_457687_.pipeline().getBlendFunction().isEmpty()) {
            this.opaqueModelSubmits.computeIfAbsent(p_457687_, p_454696_ -> new ArrayList<>()).add(p_428367_);
         } else {
            Vector3f vector3f = p_428367_.pose().pose().transformPosition(new Vector3f());
            this.translucentModelSubmits.add(new SubmitNodeStorage.TranslucentModelSubmit<>(p_428367_, p_457687_, vector3f));
         }
      }

      public void clear() {
         this.translucentModelSubmits.clear();

         for (Entry<RenderType, List<SubmitNodeStorage.ModelSubmit<?>>> entry : this.opaqueModelSubmits.entrySet()) {
            List<SubmitNodeStorage.ModelSubmit<?>> list = entry.getValue();
            if (!list.isEmpty()) {
               this.usedModelSubmitBuckets.add(entry.getKey());
               list.clear();
            }
         }
      }

      public void endFrame() {
         this.opaqueModelSubmits.keySet().removeIf(p_452283_ -> !this.usedModelSubmitBuckets.contains(p_452283_));
         this.usedModelSubmitBuckets.clear();
      }
   }
}
