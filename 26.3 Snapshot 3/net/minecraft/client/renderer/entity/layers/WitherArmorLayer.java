package net.minecraft.client.renderer.entity.layers;

import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.wither.WitherBossModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.WitherRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class WitherArmorLayer extends EnergySwirlLayer<WitherRenderState, WitherBossModel> {
   private static final Identifier WITHER_ARMOR_LOCATION = Identifier.withDefaultNamespace("textures/entity/wither/wither_armor.png");
   private final WitherBossModel model;

   public WitherArmorLayer(final RenderLayerParent<WitherRenderState, WitherBossModel> renderer, final EntityModelSet modelSet) {
      super(renderer);
      this.model = new WitherBossModel(modelSet.bakeLayer(ModelLayers.WITHER_ARMOR));
   }

   protected boolean isPowered(final WitherRenderState state) {
      return state.isPowered;
   }

   @Override
   protected float xOffset(final float t) {
      return Mth.cos(t * 0.02F) * 3.0F;
   }

   @Override
   protected Identifier getTextureLocation() {
      return WITHER_ARMOR_LOCATION;
   }

   protected WitherBossModel model() {
      return this.model;
   }
}
