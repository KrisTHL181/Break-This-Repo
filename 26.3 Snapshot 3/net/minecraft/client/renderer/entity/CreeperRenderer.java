package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.creeper.CreeperModel;
import net.minecraft.client.renderer.entity.layers.CreeperPowerLayer;
import net.minecraft.client.renderer.entity.state.CreeperRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Creeper;

public class CreeperRenderer extends MobRenderer<Creeper, CreeperRenderState, CreeperModel> {
   private static final Identifier CREEPER_LOCATION = Identifier.withDefaultNamespace("textures/entity/creeper/creeper.png");

   public CreeperRenderer(final EntityRendererProvider.Context context) {
      super(context, new CreeperModel(context.bakeLayer(ModelLayers.CREEPER)), 0.5F);
      this.addLayer(new CreeperPowerLayer(this, context.getModelSet()));
   }

   protected void scale(final CreeperRenderState state, final PoseStack poseStack) {
      float g = state.swelling;
      float wobble = 1.0F + Mth.sin(g * 100.0F) * g * 0.01F;
      g = Mth.clamp(g, 0.0F, 1.0F);
      g *= g;
      g *= g;
      float s = (1.0F + g * 0.4F) * wobble;
      float hs = (1.0F + g * 0.1F) / wobble;
      poseStack.scale(s, hs, s);
   }

   protected float getWhiteOverlayProgress(final CreeperRenderState state) {
      float step = state.swelling;
      return (int)(step * 10.0F) % 2 == 0 ? 0.0F : Mth.clamp(step, 0.5F, 1.0F);
   }

   public Identifier getTextureLocation(final CreeperRenderState state) {
      return CREEPER_LOCATION;
   }

   public CreeperRenderState createRenderState() {
      return new CreeperRenderState();
   }

   public void extractRenderState(final Creeper entity, final CreeperRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.swelling = entity.getSwelling(partialTicks);
      state.isPowered = entity.isPowered();
   }
}
