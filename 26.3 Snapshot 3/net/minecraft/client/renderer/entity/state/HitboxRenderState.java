package net.minecraft.client.renderer.entity.state;

public record HitboxRenderState(
   double x0, double y0, double z0, double x1, double y1, double z1, float offsetX, float offsetY, float offsetZ, float red, float green, float blue
) {
   public HitboxRenderState(
      final double x0,
      final double y0,
      final double z0,
      final double x1,
      final double y1,
      final double z1,
      final float red,
      final float green,
      final float blue
   ) {
      this(x0, y0, z0, x1, y1, z1, 0.0F, 0.0F, 0.0F, red, green, blue);
   }
}
