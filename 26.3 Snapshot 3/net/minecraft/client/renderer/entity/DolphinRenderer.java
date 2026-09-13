package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.dolphin.DolphinModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.DolphinCarryingItemLayer;
import net.minecraft.client.renderer.entity.state.DolphinRenderState;
import net.minecraft.client.renderer.entity.state.HoldingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.dolphin.Dolphin;

public class DolphinRenderer extends AgeableMobRenderer<Dolphin, DolphinRenderState, DolphinModel> {
   private static final Identifier DOLPHIN_LOCATION = Identifier.withDefaultNamespace("textures/entity/dolphin/dolphin.png");
   private static final Identifier DOLPHIN_BABY_LOCATION = Identifier.withDefaultNamespace("textures/entity/dolphin/dolphin_baby.png");

   public DolphinRenderer(final EntityRendererProvider.Context context) {
      super(context, new DolphinModel(context.bakeLayer(ModelLayers.DOLPHIN)), new DolphinModel(context.bakeLayer(ModelLayers.DOLPHIN_BABY)), 0.7F);
      this.addLayer(new DolphinCarryingItemLayer(this));
   }

   public Identifier getTextureLocation(final DolphinRenderState state) {
      return state.isBaby ? DOLPHIN_BABY_LOCATION : DOLPHIN_LOCATION;
   }

   public DolphinRenderState createRenderState() {
      return new DolphinRenderState();
   }

   public void extractRenderState(final Dolphin entity, final DolphinRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      HoldingEntityRenderState.extractHoldingEntityRenderState(entity, state, this.itemModelResolver);
      state.isMoving = entity.getDeltaMovement().horizontalDistanceSqr() > 1.0E-7;
   }
}
