package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.animal.fox.FoxModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.FoxRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FoxHeldItemLayer extends RenderLayer<FoxRenderState, FoxModel> {
   public FoxHeldItemLayer(RenderLayerParent<FoxRenderState, FoxModel> p_234838_) {
      super(p_234838_);
   }

   public void submit(PoseStack p_422941_, SubmitNodeCollector p_427153_, int p_430512_, FoxRenderState p_431073_, float p_431669_, float p_423890_) {
      ItemStackRenderState itemstackrenderstate = p_431073_.heldItem;
      if (!itemstackrenderstate.isEmpty()) {
         boolean flag = p_431073_.isSleeping;
         boolean flag1 = p_431073_.isBaby;
         p_422941_.pushPose();
         p_422941_.translate(this.getParentModel().head.x / 16.0F, this.getParentModel().head.y / 16.0F, this.getParentModel().head.z / 16.0F);
         if (flag1) {
            float f = 0.75F;
            p_422941_.scale(0.75F, 0.75F, 0.75F);
         }

         p_422941_.mulPose(Axis.ZP.rotation(p_431073_.headRollAngle));
         p_422941_.mulPose(Axis.YP.rotationDegrees(p_431669_));
         p_422941_.mulPose(Axis.XP.rotationDegrees(p_423890_));
         if (p_431073_.isBaby) {
            if (flag) {
               p_422941_.translate(0.4F, 0.26F, 0.15F);
            } else {
               p_422941_.translate(0.06F, 0.26F, -0.5F);
            }
         } else if (flag) {
            p_422941_.translate(0.46F, 0.26F, 0.22F);
         } else {
            p_422941_.translate(0.06F, 0.27F, -0.5F);
         }

         p_422941_.mulPose(Axis.XP.rotationDegrees(90.0F));
         if (flag) {
            p_422941_.mulPose(Axis.ZP.rotationDegrees(90.0F));
         }

         itemstackrenderstate.submit(p_422941_, p_427153_, p_430512_, OverlayTexture.NO_OVERLAY, p_431073_.outlineColor);
         p_422941_.popPose();
      }
   }
}
