package net.minecraft.client.model.geom;

public record PartPose(float x, float y, float z, float xRot, float yRot, float zRot, float xScale, float yScale, float zScale) {
   public static final PartPose ZERO = offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

   public static PartPose offset(final float x, final float y, final float z) {
      return offsetAndRotation(x, y, z, 0.0F, 0.0F, 0.0F);
   }

   public static PartPose rotation(final float x, final float y, final float z) {
      return offsetAndRotation(0.0F, 0.0F, 0.0F, x, y, z);
   }

   public static PartPose offsetAndRotation(
      final float offsetX, final float offsetY, final float offsetZ, final float rotationX, final float rotationY, final float rotationZ
   ) {
      return new PartPose(offsetX, offsetY, offsetZ, rotationX, rotationY, rotationZ, 1.0F, 1.0F, 1.0F);
   }

   public PartPose translated(final float x, final float y, final float z) {
      return new PartPose(this.x + x, this.y + y, this.z + z, this.xRot, this.yRot, this.zRot, this.xScale, this.yScale, this.zScale);
   }

   public PartPose withScale(final float scale) {
      return new PartPose(this.x, this.y, this.z, this.xRot, this.yRot, this.zRot, scale, scale, scale);
   }

   public PartPose scaled(final float factor) {
      return factor == 1.0F ? this : this.scaled(factor, factor, factor);
   }

   public PartPose scaled(final float scaleX, final float scaleY, final float scaleZ) {
      return new PartPose(
         this.x * scaleX, this.y * scaleY, this.z * scaleZ, this.xRot, this.yRot, this.zRot, this.xScale * scaleX, this.yScale * scaleY, this.zScale * scaleZ
      );
   }
}
