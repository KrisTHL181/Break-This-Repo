package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.breeze.BreezeModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.BreezeRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BreezeWindLayer extends RenderLayer<BreezeRenderState, BreezeModel> {
   private static final Identifier TEXTURE_LOCATION = Identifier.withDefaultNamespace("textures/entity/breeze/breeze_wind.png");
   private final BreezeModel model;

   public BreezeWindLayer(RenderLayerParent<BreezeRenderState, BreezeModel> p_312719_, EntityModelSet p_424586_) {
      super(p_312719_);
      this.model = new BreezeModel(p_424586_.bakeLayer(ModelLayers.BREEZE_WIND));
   }

   public void submit(PoseStack p_423827_, SubmitNodeCollector p_423334_, int p_426201_, BreezeRenderState p_424120_, float p_428800_, float p_429670_) {
      RenderType rendertype = RenderTypes.breezeWind(TEXTURE_LOCATION, this.xOffset(p_424120_.ageInTicks) % 1.0F, 0.0F);
      p_423334_.order(1)
         .submitModel(this.model, p_424120_, p_423827_, rendertype, p_426201_, OverlayTexture.NO_OVERLAY, -1, null, p_424120_.outlineColor, null);
   }

   private float xOffset(float p_310525_) {
      return p_310525_ * 0.02F;
   }
}
