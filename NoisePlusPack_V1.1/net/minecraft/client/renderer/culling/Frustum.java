package net.minecraft.client.renderer.culling;

import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector4f;

@OnlyIn(Dist.CLIENT)
public class Frustum {
   public static final int OFFSET_STEP = 4;
   private final FrustumIntersection intersection = new FrustumIntersection();
   private final Matrix4f matrix = new Matrix4f();
   private Vector4f viewVector;
   private double camX;
   private double camY;
   private double camZ;

   public Frustum(Matrix4f p_254207_, Matrix4f p_254535_) {
      this.calculateFrustum(p_254207_, p_254535_);
   }

   public Frustum(Frustum p_194440_) {
      this.intersection.set(p_194440_.matrix);
      this.matrix.set(p_194440_.matrix);
      this.camX = p_194440_.camX;
      this.camY = p_194440_.camY;
      this.camZ = p_194440_.camZ;
      this.viewVector = p_194440_.viewVector;
   }

   public Frustum offset(float p_429345_) {
      this.camX = this.camX + this.viewVector.x * p_429345_;
      this.camY = this.camY + this.viewVector.y * p_429345_;
      this.camZ = this.camZ + this.viewVector.z * p_429345_;
      return this;
   }

   public Frustum offsetToFullyIncludeCameraCube(int p_194442_) {
      double d0 = Math.floor(this.camX / p_194442_) * p_194442_;
      double d1 = Math.floor(this.camY / p_194442_) * p_194442_;
      double d2 = Math.floor(this.camZ / p_194442_) * p_194442_;
      double d3 = Math.ceil(this.camX / p_194442_) * p_194442_;
      double d4 = Math.ceil(this.camY / p_194442_) * p_194442_;

      for (double d5 = Math.ceil(this.camZ / p_194442_) * p_194442_;
         this.intersection
               .intersectAab(
                  (float)(d0 - this.camX),
                  (float)(d1 - this.camY),
                  (float)(d2 - this.camZ),
                  (float)(d3 - this.camX),
                  (float)(d4 - this.camY),
                  (float)(d5 - this.camZ)
               )
            != -2;
         this.camZ = this.camZ - this.viewVector.z() * 4.0F
      ) {
         this.camX = this.camX - this.viewVector.x() * 4.0F;
         this.camY = this.camY - this.viewVector.y() * 4.0F;
      }

      return this;
   }

   public void prepare(double p_113003_, double p_113004_, double p_113005_) {
      this.camX = p_113003_;
      this.camY = p_113004_;
      this.camZ = p_113005_;
   }

   private void calculateFrustum(Matrix4f p_253909_, Matrix4f p_254521_) {
      p_254521_.mul(p_253909_, this.matrix);
      this.intersection.set(this.matrix);
      this.viewVector = this.matrix.transformTranspose(new Vector4f(0.0F, 0.0F, 1.0F, 0.0F));
   }

   public boolean isVisible(AABB p_113030_) {
      int i = this.cubeInFrustum(p_113030_.minX, p_113030_.minY, p_113030_.minZ, p_113030_.maxX, p_113030_.maxY, p_113030_.maxZ);
      return i == -2 || i == -1;
   }

   public int cubeInFrustum(BoundingBox p_366028_) {
      return this.cubeInFrustum(p_366028_.minX(), p_366028_.minY(), p_366028_.minZ(), p_366028_.maxX() + 1, p_366028_.maxY() + 1, p_366028_.maxZ() + 1);
   }

   private int cubeInFrustum(double p_362451_, double p_367560_, double p_367158_, double p_368539_, double p_363499_, double p_365163_) {
      float f = (float)(p_362451_ - this.camX);
      float f1 = (float)(p_367560_ - this.camY);
      float f2 = (float)(p_367158_ - this.camZ);
      float f3 = (float)(p_368539_ - this.camX);
      float f4 = (float)(p_363499_ - this.camY);
      float f5 = (float)(p_365163_ - this.camZ);
      return this.intersection.intersectAab(f, f1, f2, f3, f4, f5);
   }

   public boolean pointInFrustum(double p_422977_, double p_427667_, double p_423831_) {
      return this.intersection.testPoint((float)(p_422977_ - this.camX), (float)(p_427667_ - this.camY), (float)(p_423831_ - this.camZ));
   }

   public Vector4f[] getFrustumPoints() {
      Vector4f[] avector4f = new Vector4f[]{
         new Vector4f(-1.0F, -1.0F, -1.0F, 1.0F),
         new Vector4f(1.0F, -1.0F, -1.0F, 1.0F),
         new Vector4f(1.0F, 1.0F, -1.0F, 1.0F),
         new Vector4f(-1.0F, 1.0F, -1.0F, 1.0F),
         new Vector4f(-1.0F, -1.0F, 1.0F, 1.0F),
         new Vector4f(1.0F, -1.0F, 1.0F, 1.0F),
         new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
         new Vector4f(-1.0F, 1.0F, 1.0F, 1.0F)
      };
      Matrix4f matrix4f = this.matrix.invert(new Matrix4f());

      for (int i = 0; i < 8; i++) {
         matrix4f.transform(avector4f[i]);
         avector4f[i].div(avector4f[i].w());
      }

      return avector4f;
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
