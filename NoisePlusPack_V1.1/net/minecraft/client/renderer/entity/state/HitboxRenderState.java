package net.minecraft.client.renderer.entity.state;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record HitboxRenderState(
   double x0, double y0, double z0, double x1, double y1, double z1, float offsetX, float offsetY, float offsetZ, float red, float green, float blue
) {
   public HitboxRenderState(
      double p_393222_,
      double p_394687_,
      double p_397408_,
      double p_392101_,
      double p_394320_,
      double p_391299_,
      float p_395263_,
      float p_397203_,
      float p_391960_
   ) {
      this(p_393222_, p_394687_, p_397408_, p_392101_, p_394320_, p_391299_, 0.0F, 0.0F, 0.0F, p_395263_, p_397203_, p_391960_);
   }
}
