package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.monster.witch.WitchModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.WitchRenderState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WitchItemLayer extends CrossedArmsItemLayer<WitchRenderState, WitchModel> {
   public WitchItemLayer(RenderLayerParent<WitchRenderState, WitchModel> p_234926_) {
      super(p_234926_);
   }

   protected void applyTranslation(WitchRenderState p_367958_, PoseStack p_362411_) {
      if (p_367958_.isHoldingPotion) {
         this.getParentModel().root().translateAndRotate(p_362411_);
         this.getParentModel().translateToHead(p_362411_);
         this.getParentModel().getNose().translateAndRotate(p_362411_);
         p_362411_.translate(0.0625F, 0.25F, 0.0F);
         p_362411_.mulPose(Axis.ZP.rotationDegrees(180.0F));
         p_362411_.mulPose(Axis.XP.rotationDegrees(140.0F));
         p_362411_.mulPose(Axis.ZP.rotationDegrees(10.0F));
         p_362411_.mulPose(Axis.XP.rotationDegrees(180.0F));
      } else {
         super.applyTranslation(p_367958_, p_362411_);
      }
   }
}
