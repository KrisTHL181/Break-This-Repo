package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.object.boat.RaftModel;
import net.minecraft.client.renderer.entity.state.BoatRenderState;

public class RaftRenderer extends AbstractBoatRenderer {
   private final EntityModel<BoatRenderState> model;

   public RaftRenderer(final EntityRendererProvider.Context context, final ModelLayerLocation modelId) {
      super(context, modelId.model().withPath(p -> "textures/entity/" + p + ".png"));
      this.model = new RaftModel(context.bakeLayer(modelId));
   }

   @Override
   protected EntityModel<BoatRenderState> model() {
      return this.model;
   }
}
