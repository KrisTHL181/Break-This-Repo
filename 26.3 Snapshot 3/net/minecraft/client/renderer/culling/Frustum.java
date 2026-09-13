package net.minecraft.client.renderer.culling;

import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

public class Frustum {
   public static final int OFFSET_STEP = 4;
   private final FrustumIntersection intersection = new FrustumIntersection();
   private final Matrix4f matrix = new Matrix4f();
   private Vector4f viewVector;
   private double camX;
   private double camY;
   private double camZ;

   public Frustum(final Matrix4fc modelView, final Matrix4f projection) {
      this.calculateFrustum(modelView, projection);
   }

   public Frustum(final Frustum frustum) {
      this.set(frustum);
   }

   public void set(final Frustum frustum) {
      this.intersection.set(frustum.matrix);
      this.matrix.set(frustum.matrix);
      this.camX = frustum.camX;
      this.camY = frustum.camY;
      this.camZ = frustum.camZ;
      this.viewVector = frustum.viewVector;
   }

   public Frustum offset(final float offset) {
      this.camX = this.camX + this.viewVector.x * offset;
      this.camY = this.camY + this.viewVector.y * offset;
      this.camZ = this.camZ + this.viewVector.z * offset;
      return this;
   }

   public Frustum offsetToFullyIncludeCameraCube(final int cubeSize) {
      double camX1 = Math.floor(this.camX / cubeSize) * cubeSize;
      double camY1 = Math.floor(this.camY / cubeSize) * cubeSize;
      double camZ1 = Math.floor(this.camZ / cubeSize) * cubeSize;
      double camX2 = Math.ceil(this.camX / cubeSize) * cubeSize;
      double camY2 = Math.ceil(this.camY / cubeSize) * cubeSize;

      for (double camZ2 = Math.ceil(this.camZ / cubeSize) * cubeSize;
         this.intersection
               .intersectAab(
                  (float)(camX1 - this.camX),
                  (float)(camY1 - this.camY),
                  (float)(camZ1 - this.camZ),
                  (float)(camX2 - this.camX),
                  (float)(camY2 - this.camY),
                  (float)(camZ2 - this.camZ)
               )
            != -2;
         this.camZ = this.camZ - this.viewVector.z() * 4.0F
      ) {
         this.camX = this.camX - this.viewVector.x() * 4.0F;
         this.camY = this.camY - this.viewVector.y() * 4.0F;
      }

      return this;
   }

   public void prepare(final double camX, final double camY, final double camZ) {
      this.camX = camX;
      this.camY = camY;
      this.camZ = camZ;
   }

   private void calculateFrustum(final Matrix4fc modelView, final Matrix4f projection) {
      projection.mul(modelView, this.matrix);
      this.intersection.set(this.matrix);
      this.viewVector = this.matrix.transformTranspose(new Vector4f(0.0F, 0.0F, 1.0F, 0.0F));
   }

   public boolean isVisible(final AABB bb) {
      int intersectionResult = this.cubeInFrustum(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
      return intersectionResult == -2 || intersectionResult == -1;
   }

   public int cubeInFrustum(final BoundingBox bb) {
      return this.cubeInFrustum(bb.minX(), bb.minY(), bb.minZ(), bb.maxX() + 1, bb.maxY() + 1, bb.maxZ() + 1);
   }

   private int cubeInFrustum(final double minX, final double minY, final double minZ, final double maxX, final double maxY, final double maxZ) {
      float x1 = (float)(minX - this.camX);
      float y1 = (float)(minY - this.camY);
      float z1 = (float)(minZ - this.camZ);
      float x2 = (float)(maxX - this.camX);
      float y2 = (float)(maxY - this.camY);
      float z2 = (float)(maxZ - this.camZ);
      return this.intersection.intersectAab(x1, y1, z1, x2, y2, z2);
   }

   public boolean pointInFrustum(final double x, final double y, final double z) {
      return this.intersection.testPoint((float)(x - this.camX), (float)(y - this.camY), (float)(z - this.camZ));
   }

   public Vector4f[] getFrustumPoints() {
      Vector4f[] frustumPoints = new Vector4f[]{
         new Vector4f(-1.0F, -1.0F, -1.0F, 1.0F),
         new Vector4f(1.0F, -1.0F, -1.0F, 1.0F),
         new Vector4f(1.0F, 1.0F, -1.0F, 1.0F),
         new Vector4f(-1.0F, 1.0F, -1.0F, 1.0F),
         new Vector4f(-1.0F, -1.0F, 1.0F, 1.0F),
         new Vector4f(1.0F, -1.0F, 1.0F, 1.0F),
         new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
         new Vector4f(-1.0F, 1.0F, 1.0F, 1.0F)
      };
      Matrix4f clipToWorldMatrix = this.matrix.invert(new Matrix4f());

      for (int i = 0; i < 8; i++) {
         clipToWorldMatrix.transform(frustumPoints[i]);
         frustumPoints[i].div(frustumPoints[i].w());
      }

      return frustumPoints;
   }

   public double getCamX() {
      return this.camX;
   }

   public double getCamY() {
      return this.camY;
   }

   public double getCamZ() {
      return this.camZ;
   }
}
