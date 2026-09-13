package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.monster.enderman.EndermanModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.EndermanRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CarriedBlockLayer extends RenderLayer<EndermanRenderState, EndermanModel<EndermanRenderState>> {
   public CarriedBlockLayer(RenderLayerParent<EndermanRenderState, EndermanModel<EndermanRenderState>> p_234814_) {
      super(p_234814_);
   }

   public void submit(PoseStack p_425287_, SubmitNodeCollector p_428873_, int p_424689_, EndermanRenderState p_428563_, float p_428624_, float p_426180_) {
      BlockState blockstate = p_428563_.carriedBlock;
      if (blockstate != null) {
         p_425287_.pushPose();
         p_425287_.translate(0.0F, 0.6875F, -0.75F);
         p_425287_.mulPose(Axis.XP.rotationDegrees(20.0F));
         p_425287_.mulPose(Axis.YP.rotationDegrees(45.0F));
         p_425287_.translate(0.25F, 0.1875F, 0.25F);
         float f = 0.5F;
         p_425287_.scale(-0.5F, -0.5F, 0.5F);
         p_425287_.mulPose(Axis.YP.rotationDegrees(90.0F));
         p_428873_.submitBlock(p_425287_, blockstate, p_424689_, OverlayTexture.NO_OVERLAY, p_428563_.outlineColor);
         p_425287_.popPose();
      }
   }
}
