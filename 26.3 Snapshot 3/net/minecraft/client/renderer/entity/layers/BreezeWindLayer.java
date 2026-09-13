package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.breeze.BreezeModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.BreezeRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public class BreezeWindLayer extends RenderLayer<BreezeRenderState, BreezeModel> {
   private static final Identifier TEXTURE_LOCATION = Identifier.withDefaultNamespace("textures/entity/breeze/breeze_wind.png");
   private final BreezeModel model;

   public BreezeWindLayer(final RenderLayerParent<BreezeRenderState, BreezeModel> renderer, final EntityModelSet modelSet) {
      super(renderer);
      this.model = new BreezeModel(modelSet.bakeLayer(ModelLayers.BREEZE_WIND));
   }

   public void submit(
      final PoseStack poseStack,
      final SubmitNodeCollector submitNodeCollector,
      final int lightCoords,
      final BreezeRenderState state,
      final float yRot,
      final float xRot
   ) {
      RenderType renderType = RenderTypes.breezeWind(TEXTURE_LOCATION, this.xOffset(state.ageInTicks) % 1.0F, 0.0F);
      submitNodeCollector.order(1).submitModel(this.model, state, poseStack, renderType, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
   }

   private float xOffset(final float t) {
      return t * 0.02F;
   }
}
