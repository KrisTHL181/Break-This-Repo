package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.animal.panda.PandaModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.PandaRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PandaHoldsItemLayer extends RenderLayer<PandaRenderState, PandaModel> {
   public PandaHoldsItemLayer(RenderLayerParent<PandaRenderState, PandaModel> p_234862_) {
      super(p_234862_);
   }

   public void submit(PoseStack p_430679_, SubmitNodeCollector p_423909_, int p_426601_, PandaRenderState p_431260_, float p_429369_, float p_429860_) {
      ItemStackRenderState itemstackrenderstate = p_431260_.heldItem;
      if (!itemstackrenderstate.isEmpty() && p_431260_.isSitting && !p_431260_.isScared) {
         float f = -0.6F;
         float f1 = 1.4F;
         if (p_431260_.isEating) {
            f -= 0.2F * Mth.sin(p_431260_.ageInTicks * 0.6F) + 0.2F;
            f1 -= 0.09F * Mth.sin(p_431260_.ageInTicks * 0.6F);
         }

         p_430679_.pushPose();
         p_430679_.translate(0.1F, f1, f);
         itemstackrenderstate.submit(p_430679_, p_423909_, p_426601_, OverlayTexture.NO_OVERLAY, p_431260_.outlineColor);
         p_430679_.popPose();
      }
   }
}
