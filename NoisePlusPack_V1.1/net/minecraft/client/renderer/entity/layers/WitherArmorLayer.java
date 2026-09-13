package net.minecraft.client.renderer.entity.layers;

import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.wither.WitherBossModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.WitherRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WitherArmorLayer extends EnergySwirlLayer<WitherRenderState, WitherBossModel> {
   private static final Identifier WITHER_ARMOR_LOCATION = Identifier.withDefaultNamespace("textures/entity/wither/wither_armor.png");
   private final WitherBossModel model;

   public WitherArmorLayer(RenderLayerParent<WitherRenderState, WitherBossModel> p_174554_, EntityModelSet p_174555_) {
      super(p_174554_);
      this.model = new WitherBossModel(p_174555_.bakeLayer(ModelLayers.WITHER_ARMOR));
   }

   protected boolean isPowered(WitherRenderState p_361038_) {
      return p_361038_.isPowered;
   }

   @Override
   protected float xOffset(float p_117702_) {
      return Mth.cos(p_117702_ * 0.02F) * 3.0F;
   }

   @Override
   protected Identifier getTextureLocation() {
      return WITHER_ARMOR_LOCATION;
   }

   protected WitherBossModel model() {
      return this.model;
   }
}
