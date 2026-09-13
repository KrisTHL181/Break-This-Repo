package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.animal.dolphin.DolphinModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.DolphinRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DolphinCarryingItemLayer extends RenderLayer<DolphinRenderState, DolphinModel> {
   public DolphinCarryingItemLayer(RenderLayerParent<DolphinRenderState, DolphinModel> p_234834_) {
      super(p_234834_);
   }

   public void submit(PoseStack p_431502_, SubmitNodeCollector p_429965_, int p_426157_, DolphinRenderState p_422604_, float p_422393_, float p_430009_) {
      ItemStackRenderState itemstackrenderstate = p_422604_.heldItem;
      if (!itemstackrenderstate.isEmpty()) {
         p_431502_.pushPose();
         float f = 1.0F;
         float f1 = -1.0F;
         float f2 = Mth.abs(p_422604_.xRot) / 60.0F;
         if (p_422604_.xRot < 0.0F) {
            p_431502_.translate(0.0F, 1.0F - f2 * 0.5F, -1.0F + f2 * 0.5F);
         } else {
            p_431502_.translate(0.0F, 1.0F + f2 * 0.8F, -1.0F + f2 * 0.2F);
         }

         itemstackrenderstate.submit(p_431502_, p_429965_, p_426157_, OverlayTexture.NO_OVERLAY, p_422604_.outlineColor);
         p_431502_.popPose();
      }
   }
}
