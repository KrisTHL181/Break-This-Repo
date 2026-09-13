package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public abstract class RenderLayer<S extends EntityRenderState, M extends EntityModel<? super S>> {
   private final RenderLayerParent<S, M> renderer;

   public RenderLayer(final RenderLayerParent<S, M> renderer) {
      this.renderer = renderer;
   }

   protected static <S extends LivingEntityRenderState> void coloredCutoutModelCopyLayerRender(
      final Model<? super S> model,
      final Identifier texture,
      final PoseStack poseStack,
      final SubmitNodeCollector submitNodeCollector,
      final int lightCoords,
      final S state,
      final int color,
      final int order
   ) {
      if (!state.isInvisible) {
         renderColoredCutoutModel(model, texture, poseStack, submitNodeCollector, lightCoords, state, color, order);
      }
   }

   protected static <S extends LivingEntityRenderState> void renderColoredCutoutModel(
      final Model<? super S> model,
      final Identifier texture,
      final PoseStack poseStack,
      final SubmitNodeCollector submitNodeCollector,
      final int lightCoords,
      final S state,
      final int color,
      final int order
   ) {
      submitNodeCollector.order(order)
         .submitModel(
            model,
            state,
            poseStack,
            RenderTypes.entityCutout(texture),
            lightCoords,
            LivingEntityRenderer.getOverlayCoords(state, 0.0F),
            color,
            null,
            state.outlineColor
         );
   }

   public M getParentModel() {
      return this.renderer.getModel();
   }

   public abstract void submit(final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final int lightCoords, S state, float yRot, float xRot);
}
