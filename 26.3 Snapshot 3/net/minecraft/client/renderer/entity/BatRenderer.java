package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.ambient.BatModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.BatRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ambient.Bat;

public class BatRenderer extends MobRenderer<Bat, BatRenderState, BatModel> {
   private static final Identifier BAT_LOCATION = Identifier.withDefaultNamespace("textures/entity/bat/bat.png");

   public BatRenderer(final EntityRendererProvider.Context context) {
      super(context, new BatModel(context.bakeLayer(ModelLayers.BAT)), 0.25F);
   }

   public Identifier getTextureLocation(final BatRenderState state) {
      return BAT_LOCATION;
   }

   public BatRenderState createRenderState() {
      return new BatRenderState();
   }

   public void extractRenderState(final Bat entity, final BatRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.isResting = entity.isResting();
      state.flyAnimationState.copyFrom(entity.flyAnimationState);
      state.restAnimationState.copyFrom(entity.restAnimationState);
   }
}
