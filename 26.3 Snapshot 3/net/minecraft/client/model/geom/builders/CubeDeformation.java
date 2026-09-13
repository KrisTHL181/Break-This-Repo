package net.minecraft.client.model.geom.builders;

public class CubeDeformation {
   public static final CubeDeformation NONE = new CubeDeformation(0.0F);
   final float growX;
   final float growY;
   final float growZ;

   public CubeDeformation(final float growX, final float growY, final float growZ) {
      this.growX = growX;
      this.growY = growY;
      this.growZ = growZ;
   }

   public CubeDeformation(final float grow) {
      this(grow, grow, grow);
   }

   public CubeDeformation extend(final float factor) {
      return new CubeDeformation(this.growX + factor, this.growY + factor, this.growZ + factor);
   }

   public CubeDeformation extend(final float factorX, final float factorY, final float factorZ) {
      return new CubeDeformation(this.growX + factorX, this.growY + factorY, this.growZ + factorZ);
   }
}
