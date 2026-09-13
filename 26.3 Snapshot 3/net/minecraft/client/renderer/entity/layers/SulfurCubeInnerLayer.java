package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.slime.SulfurCubeModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.TntRenderer;
import net.minecraft.client.renderer.entity.state.SulfurCubeRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public class SulfurCubeInnerLayer extends RenderLayer<SulfurCubeRenderState, SulfurCubeModel> {
   private static final Identifier SULFUR_CUBE_INNER_LOCATION = Identifier.withDefaultNamespace("textures/entity/sulfur_cube/sulfur_cube_inner.png");
   private static final Identifier SULFUR_CUBE_SMALL_INNER_LOCATION = Identifier.withDefaultNamespace("textures/entity/sulfur_cube/sulfur_cube_inner_small.png");
   private final SulfurCubeModel normalModel;
   private final SulfurCubeModel smallModel;

   public SulfurCubeInnerLayer(final RenderLayerParent<SulfurCubeRenderState, SulfurCubeModel> renderer, final EntityModelSet modelSet) {
      super(renderer);
      this.normalModel = new SulfurCubeModel(modelSet.bakeLayer(ModelLayers.SULFUR_CUBE_INNER));
      this.smallModel = new SulfurCubeModel(modelSet.bakeLayer(ModelLayers.SULFUR_CUBE_SMALL_INNER));
   }

   public void submit(
      final PoseStack poseStack,
      final SubmitNodeCollector submitNodeCollector,
      final int lightCoords,
      final SulfurCubeRenderState state,
      final float yRot,
      final float xRot
   ) {
      if (state.wornHeadType == null) {
         int overlayCoords = state.fuseRemainingTicks > 0.0F && TntRenderer.isLit(state.fuseRemainingTicks)
            ? OverlayTexture.pack(OverlayTexture.u(1.0F), 10)
            : LivingEntityRenderer.getOverlayCoords(state, 0.0F);
         if (!state.containedBlock.isEmpty()) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            if (state.isBaby) {
               poseStack.scale(0.5F, 0.5F, 0.5F);
            }

            poseStack.translate(-0.5F, -0.518F, -0.5F);
            state.containedBlock.submit(poseStack, submitNodeCollector, state.lightCoords, overlayCoords, state.outlineColor);
            poseStack.popPose();
         } else if (!state.isInvisible) {
            Identifier location = state.isBaby ? SULFUR_CUBE_SMALL_INNER_LOCATION : SULFUR_CUBE_INNER_LOCATION;
            SulfurCubeModel model = state.isBaby ? this.smallModel : this.normalModel;
            RenderType renderType = RenderTypes.entityTranslucent(location);
            submitNodeCollector.order(-1).submitModel(model, state, poseStack, renderType, lightCoords, overlayCoords, -1, null, state.outlineColor);
         }
      }
   }
}
