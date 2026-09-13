package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.animal.golem.IronGolemModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.IronGolemRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class IronGolemFlowerLayer extends RenderLayer<IronGolemRenderState, IronGolemModel> {
   public IronGolemFlowerLayer(RenderLayerParent<IronGolemRenderState, IronGolemModel> p_234842_) {
      super(p_234842_);
   }

   public void submit(PoseStack p_429714_, SubmitNodeCollector p_425431_, int p_431529_, IronGolemRenderState p_428533_, float p_427596_, float p_425489_) {
      if (p_428533_.offerFlowerTick != 0) {
         p_429714_.pushPose();
         ModelPart modelpart = this.getParentModel().getFlowerHoldingArm();
         modelpart.translateAndRotate(p_429714_);
         p_429714_.translate(-1.1875F, 1.0625F, -0.9375F);
         p_429714_.translate(0.5F, 0.5F, 0.5F);
         float f = 0.5F;
         p_429714_.scale(0.5F, 0.5F, 0.5F);
         p_429714_.mulPose(Axis.XP.rotationDegrees(-90.0F));
         p_429714_.translate(-0.5F, -0.5F, -0.5F);
         p_425431_.submitBlock(p_429714_, Blocks.POPPY.defaultBlockState(), p_431529_, OverlayTexture.NO_OVERLAY, p_428533_.outlineColor);
         p_429714_.popPose();
      }
   }
}
