package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.TntRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TntRenderer extends EntityRenderer<PrimedTnt, TntRenderState> {
   public TntRenderer(EntityRendererProvider.Context p_174426_) {
      super(p_174426_);
      this.shadowRadius = 0.5F;
   }

   public void submit(TntRenderState p_424445_, PoseStack p_422760_, SubmitNodeCollector p_423241_, CameraRenderState p_430379_) {
      p_422760_.pushPose();
      p_422760_.translate(0.0F, 0.5F, 0.0F);
      float f = p_424445_.fuseRemainingInTicks;
      if (p_424445_.fuseRemainingInTicks < 10.0F) {
         float f1 = 1.0F - p_424445_.fuseRemainingInTicks / 10.0F;
         f1 = Mth.clamp(f1, 0.0F, 1.0F);
         f1 *= f1;
         f1 *= f1;
         float f2 = 1.0F + f1 * 0.3F;
         p_422760_.scale(f2, f2, f2);
      }

      p_422760_.mulPose(Axis.YP.rotationDegrees(-90.0F));
      p_422760_.translate(-0.5F, -0.5F, 0.5F);
      p_422760_.mulPose(Axis.YP.rotationDegrees(90.0F));
      if (p_424445_.blockState != null) {
         TntMinecartRenderer.submitWhiteSolidBlock(
            p_424445_.blockState, p_422760_, p_423241_, p_424445_.lightCoords, (int)f / 5 % 2 == 0, p_424445_.outlineColor
         );
      }

      p_422760_.popPose();
      super.submit(p_424445_, p_422760_, p_423241_, p_430379_);
   }

   public TntRenderState createRenderState() {
      return new TntRenderState();
   }

   public void extractRenderState(PrimedTnt p_366432_, TntRenderState p_365560_, float p_367967_) {
      super.extractRenderState(p_366432_, p_365560_, p_367967_);
      p_365560_.fuseRemainingInTicks = p_366432_.getFuse() - p_367967_ + 1.0F;
      p_365560_.blockState = p_366432_.getBlockState();
   }
}
