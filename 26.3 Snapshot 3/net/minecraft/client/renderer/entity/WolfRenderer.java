package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.wolf.AdultWolfModel;
import net.minecraft.client.model.animal.wolf.BabyWolfModel;
import net.minecraft.client.model.animal.wolf.WolfModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.WolfArmorLayer;
import net.minecraft.client.renderer.entity.layers.WolfCollarLayer;
import net.minecraft.client.renderer.entity.state.WolfRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.animal.wolf.Wolf;

public class WolfRenderer extends AgeableMobRenderer<Wolf, WolfRenderState, WolfModel> {
   public WolfRenderer(final EntityRendererProvider.Context context) {
      super(context, new AdultWolfModel(context.bakeLayer(ModelLayers.WOLF)), new BabyWolfModel(context.bakeLayer(ModelLayers.WOLF_BABY)), 0.5F);
      this.addLayer(new WolfArmorLayer(this, context.getModelSet(), context.getEquipmentRenderer()));
      this.addLayer(new WolfCollarLayer(this));
   }

   protected int getModelTint(final WolfRenderState state) {
      float wetShade = state.wetShade;
      return wetShade == 1.0F ? -1 : ARGB.colorFromFloat(1.0F, wetShade, wetShade, wetShade);
   }

   public Identifier getTextureLocation(final WolfRenderState state) {
      return state.texture;
   }

   public WolfRenderState createRenderState() {
      return new WolfRenderState();
   }

   public void extractRenderState(final Wolf entity, final WolfRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.isAngry = entity.isAngry();
      state.isSitting = entity.isInSittingPose();
      state.tailAngle = entity.getTailAngle();
      state.headRollAngle = entity.getHeadRollAngle(partialTicks);
      state.shakeAnim = entity.getShakeAnim(partialTicks);
      state.texture = entity.getTexture();
      state.wetShade = entity.getWetShade(partialTicks);
      state.collarColor = entity.isTame() ? entity.getCollarColor() : null;
      state.bodyArmorItem = entity.getBodyArmorItem().copy();
   }
}
