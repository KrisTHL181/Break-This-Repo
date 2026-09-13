package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.dolphin.DolphinModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.DolphinCarryingItemLayer;
import net.minecraft.client.renderer.entity.state.DolphinRenderState;
import net.minecraft.client.renderer.entity.state.HoldingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DolphinRenderer extends AgeableMobRenderer<Dolphin, DolphinRenderState, DolphinModel> {
   private static final Identifier DOLPHIN_LOCATION = Identifier.withDefaultNamespace("textures/entity/dolphin.png");

   public DolphinRenderer(EntityRendererProvider.Context p_173960_) {
      super(p_173960_, new DolphinModel(p_173960_.bakeLayer(ModelLayers.DOLPHIN)), new DolphinModel(p_173960_.bakeLayer(ModelLayers.DOLPHIN_BABY)), 0.7F);
      this.addLayer(new DolphinCarryingItemLayer(this));
   }

   public Identifier getTextureLocation(DolphinRenderState p_457859_) {
      return DOLPHIN_LOCATION;
   }

   public DolphinRenderState createRenderState() {
      return new DolphinRenderState();
   }

   public void extractRenderState(Dolphin p_456322_, DolphinRenderState p_370009_, float p_361573_) {
      super.extractRenderState(p_456322_, p_370009_, p_361573_);
      HoldingEntityRenderState.extractHoldingEntityRenderState(p_456322_, p_370009_, this.itemModelResolver);
      p_370009_.isMoving = p_456322_.getDeltaMovement().horizontalDistanceSqr() > 1.0E-7;
   }
}
