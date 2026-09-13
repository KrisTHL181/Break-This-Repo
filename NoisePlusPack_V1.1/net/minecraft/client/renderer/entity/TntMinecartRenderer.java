package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.MinecartTntRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.minecart.MinecartTNT;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TntMinecartRenderer extends AbstractMinecartRenderer<MinecartTNT, MinecartTntRenderState> {
   public TntMinecartRenderer(EntityRendererProvider.Context p_174424_) {
      super(p_174424_, ModelLayers.TNT_MINECART);
   }

   protected void submitMinecartContents(
      MinecartTntRenderState p_425284_, BlockState p_428788_, PoseStack p_429442_, SubmitNodeCollector p_430697_, int p_431248_
   ) {
      float f = p_425284_.fuseRemainingInTicks;
      if (f > -1.0F && f < 10.0F) {
         float f1 = 1.0F - f / 10.0F;
         f1 = Mth.clamp(f1, 0.0F, 1.0F);
         f1 *= f1;
         f1 *= f1;
         float f2 = 1.0F + f1 * 0.3F;
         p_429442_.scale(f2, f2, f2);
      }

      submitWhiteSolidBlock(p_428788_, p_429442_, p_430697_, p_431248_, f > -1.0F && (int)f / 5 % 2 == 0, p_425284_.outlineColor);
   }

   public static void submitWhiteSolidBlock(
      BlockState p_427868_, PoseStack p_430542_, SubmitNodeCollector p_423773_, int p_429870_, boolean p_427055_, int p_428351_
   ) {
      int i;
      if (p_427055_) {
         i = OverlayTexture.pack(OverlayTexture.u(1.0F), 10);
      } else {
         i = OverlayTexture.NO_OVERLAY;
      }

      p_423773_.submitBlock(p_430542_, p_427868_, p_429870_, i, p_428351_);
   }

   public MinecartTntRenderState createRenderState() {
      return new MinecartTntRenderState();
   }

   public void extractRenderState(MinecartTNT p_455651_, MinecartTntRenderState p_362573_, float p_365468_) {
      super.extractRenderState(p_455651_, p_362573_, p_365468_);
      p_362573_.fuseRemainingInTicks = p_455651_.getFuse() > -1 ? p_455651_.getFuse() - p_365468_ + 1.0F : -1.0F;
   }
}
