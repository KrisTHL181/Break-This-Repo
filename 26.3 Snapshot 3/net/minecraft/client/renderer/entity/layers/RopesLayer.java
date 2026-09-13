package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.animal.ghast.HappyGhastModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.HappyGhastRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;

public class RopesLayer<M extends HappyGhastModel> extends RenderLayer<HappyGhastRenderState, M> {
   private final Identifier ropesTexture;
   private final HappyGhastModel adultModel;
   private final HappyGhastModel babyModel;

   public RopesLayer(final RenderLayerParent<HappyGhastRenderState, M> renderer, final EntityModelSet modelSet, final Identifier ropesTexture) {
      super(renderer);
      this.ropesTexture = ropesTexture;
      this.adultModel = new HappyGhastModel(modelSet.bakeLayer(ModelLayers.HAPPY_GHAST_ROPES));
      this.babyModel = new HappyGhastModel(modelSet.bakeLayer(ModelLayers.HAPPY_GHAST_BABY_ROPES));
   }

   public void submit(
      final PoseStack poseStack,
      final SubmitNodeCollector submitNodeCollector,
      final int lightCoords,
      final HappyGhastRenderState state,
      final float yRot,
      final float xRot
   ) {
      if (state.isLeashHolder && state.bodyItem.is(ItemTags.HARNESSES)) {
         HappyGhastModel model = state.isBaby ? this.babyModel : this.adultModel;
         submitNodeCollector.submitModel(model, state, poseStack, this.ropesTexture, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
      }
   }
}
