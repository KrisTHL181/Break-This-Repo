package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ModelPartFeatureRenderer {
   private final PoseStack poseStack = new PoseStack();

   public void render(
      SubmitNodeCollection p_428472_, MultiBufferSource.BufferSource p_426941_, OutlineBufferSource p_431895_, MultiBufferSource.BufferSource p_428809_
   ) {
      ModelPartFeatureRenderer.Storage modelpartfeaturerenderer$storage = p_428472_.getModelPartSubmits();

      for (Entry<RenderType, List<SubmitNodeStorage.ModelPartSubmit>> entry : modelpartfeaturerenderer$storage.modelPartSubmits.entrySet()) {
         RenderType rendertype = entry.getKey();
         List<SubmitNodeStorage.ModelPartSubmit> list = entry.getValue();
         VertexConsumer vertexconsumer = p_426941_.getBuffer(rendertype);

         for (SubmitNodeStorage.ModelPartSubmit submitnodestorage$modelpartsubmit : list) {
            VertexConsumer vertexconsumer1;
            if (submitnodestorage$modelpartsubmit.sprite() != null) {
               if (submitnodestorage$modelpartsubmit.hasFoil()) {
                  vertexconsumer1 = submitnodestorage$modelpartsubmit.sprite()
                     .wrap(ItemRenderer.getFoilBuffer(p_426941_, rendertype, submitnodestorage$modelpartsubmit.sheeted(), true));
               } else {
                  vertexconsumer1 = submitnodestorage$modelpartsubmit.sprite().wrap(vertexconsumer);
               }
            } else if (submitnodestorage$modelpartsubmit.hasFoil()) {
               vertexconsumer1 = ItemRenderer.getFoilBuffer(p_426941_, rendertype, submitnodestorage$modelpartsubmit.sheeted(), true);
            } else {
               vertexconsumer1 = vertexconsumer;
            }

            this.poseStack.last().set(submitnodestorage$modelpartsubmit.pose());
            submitnodestorage$modelpartsubmit.modelPart()
               .render(
                  this.poseStack,
                  vertexconsumer1,
                  submitnodestorage$modelpartsubmit.lightCoords(),
                  submitnodestorage$modelpartsubmit.overlayCoords(),
                  submitnodestorage$modelpartsubmit.tintedColor()
               );
            if (submitnodestorage$modelpartsubmit.outlineColor() != 0 && (rendertype.outline().isPresent() || rendertype.isOutline())) {
               p_431895_.setColor(submitnodestorage$modelpartsubmit.outlineColor());
               VertexConsumer vertexconsumer2 = p_431895_.getBuffer(rendertype);
               submitnodestorage$modelpartsubmit.modelPart()
                  .render(
                     this.poseStack,
                     submitnodestorage$modelpartsubmit.sprite() == null ? vertexconsumer2 : submitnodestorage$modelpartsubmit.sprite().wrap(vertexconsumer2),
                     submitnodestorage$modelpartsubmit.lightCoords(),
                     submitnodestorage$modelpartsubmit.overlayCoords(),
                     submitnodestorage$modelpartsubmit.tintedColor()
                  );
            }

            if (submitnodestorage$modelpartsubmit.crumblingOverlay() != null) {
               VertexConsumer vertexconsumer3 = new SheetedDecalTextureGenerator(
                  p_428809_.getBuffer(ModelBakery.DESTROY_TYPES.get(submitnodestorage$modelpartsubmit.crumblingOverlay().progress())),
                  submitnodestorage$modelpartsubmit.crumblingOverlay().cameraPose(),
                  1.0F
               );
               submitnodestorage$modelpartsubmit.modelPart()
                  .render(
                     this.poseStack,
                     vertexconsumer3,
                     submitnodestorage$modelpartsubmit.lightCoords(),
                     submitnodestorage$modelpartsubmit.overlayCoords(),
                     submitnodestorage$modelpartsubmit.tintedColor()
                  );
            }
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class Storage {
      final Map<RenderType, List<SubmitNodeStorage.ModelPartSubmit>> modelPartSubmits = new HashMap<>();
      private final Set<RenderType> modelPartSubmitsUsage = new ObjectOpenHashSet();

      public void add(RenderType p_453839_, SubmitNodeStorage.ModelPartSubmit p_428026_) {
         this.modelPartSubmits.computeIfAbsent(p_453839_, p_452439_ -> new ArrayList<>()).add(p_428026_);
      }

      public void clear() {
         for (Entry<RenderType, List<SubmitNodeStorage.ModelPartSubmit>> entry : this.modelPartSubmits.entrySet()) {
            if (!entry.getValue().isEmpty()) {
               this.modelPartSubmitsUsage.add(entry.getKey());
               entry.getValue().clear();
            }
         }
      }

      public void endFrame() {
         this.modelPartSubmits.keySet().removeIf(p_453514_ -> !this.modelPartSubmitsUsage.contains(p_453514_));
         this.modelPartSubmitsUsage.clear();
      }
   }
}
