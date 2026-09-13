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
public class BreezeEyesLayer extends RenderLayer<BreezeRenderState, BreezeModel> {
   private static final RenderType BREEZE_EYES = RenderTypes.breezeEyes(Identifier.withDefaultNamespace("textures/entity/breeze/breeze_eyes.png"));
   private final BreezeModel model;

   public BreezeEyesLayer(RenderLayerParent<BreezeRenderState, BreezeModel> p_310165_, EntityModelSet p_422843_) {
      super(p_310165_);
      this.model = new BreezeModel(p_422843_.bakeLayer(ModelLayers.BREEZE_EYES));
   }

   public void submit(PoseStack p_425332_, SubmitNodeCollector p_427127_, int p_422356_, BreezeRenderState p_425741_, float p_428393_, float p_424774_) {
      p_427127_.order(1)
         .submitModel(this.model, p_425741_, p_425332_, BREEZE_EYES, p_422356_, OverlayTexture.NO_OVERLAY, -1, null, p_425741_.outlineColor, null);
   }
}
