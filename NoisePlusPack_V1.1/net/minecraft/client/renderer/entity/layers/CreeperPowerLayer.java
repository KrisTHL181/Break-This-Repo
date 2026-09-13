package net.minecraft.client.renderer.entity.layers;

import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.creeper.CreeperModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.CreeperRenderState;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CreeperPowerLayer extends EnergySwirlLayer<CreeperRenderState, CreeperModel> {
   private static final Identifier POWER_LOCATION = Identifier.withDefaultNamespace("textures/entity/creeper/creeper_armor.png");
   private final CreeperModel model;

   public CreeperPowerLayer(RenderLayerParent<CreeperRenderState, CreeperModel> p_174471_, EntityModelSet p_174472_) {
      super(p_174471_);
      this.model = new CreeperModel(p_174472_.bakeLayer(ModelLayers.CREEPER_ARMOR));
   }

   protected boolean isPowered(CreeperRenderState p_367950_) {
      return p_367950_.isPowered;
   }

   @Override
   protected float xOffset(float p_116683_) {
      return p_116683_ * 0.01F;
   }

   @Override
   protected Identifier getTextureLocation() {
      return POWER_LOCATION;
   }

   protected CreeperModel model() {
      return this.model;
   }
}
