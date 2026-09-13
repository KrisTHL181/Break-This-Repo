package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class RenderLayer<S extends EntityRenderState, M extends EntityModel<? super S>> {
   private final RenderLayerParent<S, M> renderer;

   public RenderLayer(RenderLayerParent<S, M> p_117346_) {
      this.renderer = p_117346_;
   }

   protected static <S extends LivingEntityRenderState> void coloredCutoutModelCopyLayerRender(
      Model<? super S> p_431218_,
      Identifier p_460802_,
      PoseStack p_117363_,
      SubmitNodeCollector p_429375_,
      int p_117365_,
      S p_366295_,
      int p_345259_,
      int p_430778_
   ) {
      if (!p_366295_.isInvisible) {
         renderColoredCutoutModel(p_431218_, p_460802_, p_117363_, p_429375_, p_117365_, p_366295_, p_345259_, p_430778_);
      }
   }

   protected static <S extends LivingEntityRenderState> void renderColoredCutoutModel(
      Model<? super S> p_425116_,
      Identifier p_458878_,
      PoseStack p_117379_,
      SubmitNodeCollector p_424185_,
      int p_117381_,
      S p_360714_,
      int p_343754_,
      int p_426846_
   ) {
      p_424185_.order(p_426846_)
         .submitModel(
            p_425116_,
            p_360714_,
            p_117379_,
            RenderTypes.entityCutoutNoCull(p_458878_),
            p_117381_,
            LivingEntityRenderer.getOverlayCoords(p_360714_, 0.0F),
            p_343754_,
            null,
            p_360714_.outlineColor,
            null
         );
   }

   public M getParentModel() {
      return this.renderer.getModel();
   }

   public abstract void submit(PoseStack var1, SubmitNodeCollector var2, int var3, S var4, float var5, float var6);
}
