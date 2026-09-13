package net.minecraft.client.renderer;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4f;

public class Projection {
   private ProjectionType projectionType = ProjectionType.PERSPECTIVE;
   private float zNear;
   private float zFar;
   private float perspectiveFov;
   private float width;
   private float height;
   private boolean orthoInvertY;
   private boolean isMatrixDirty;
   private final Matrix4f matrix = new Matrix4f();
   private long matrixVersion = -1L;

   public void setupPerspective(final float zNear, final float zFar, final float fov, final float width, final float height) {
      if (this.projectionType != ProjectionType.PERSPECTIVE
         || this.zNear != zNear
         || this.zFar != zFar
         || this.perspectiveFov != fov
         || this.width != width
         || this.height != height) {
         this.isMatrixDirty = true;
         this.projectionType = ProjectionType.PERSPECTIVE;
         this.zNear = zNear;
         this.zFar = zFar;
         this.perspectiveFov = fov;
         this.width = width;
         this.height = height;
      }
   }

   public void setupOrtho(final float zNear, final float zFar, final float width, final float height, final boolean invertY) {
      if (this.projectionType != ProjectionType.ORTHOGRAPHIC
         || this.zNear != zNear
         || this.zFar != zFar
         || this.width != width
         || this.height != height
         || this.orthoInvertY != invertY) {
         this.isMatrixDirty = true;
         this.projectionType = ProjectionType.ORTHOGRAPHIC;
         this.zNear = zNear;
         this.zFar = zFar;
         this.perspectiveFov = 0.0F;
         this.width = width;
         this.height = height;
         this.orthoInvertY = invertY;
      }
   }

   public void setSize(final float width, final float height) {
      this.isMatrixDirty = true;
      this.width = width;
      this.height = height;
   }

   public Matrix4f getMatrix(final Matrix4f dest) {
      if (!this.isMatrixDirty) {
         return dest.set(this.matrix);
      }

      this.isMatrixDirty = false;
      this.matrixVersion++;
      float near = this.zFar;
      float far = this.zNear;
      boolean zZeroToOne = RenderSystem.getDevice().getDeviceInfo().isZZeroToOne();
      return this.projectionType == ProjectionType.PERSPECTIVE
         ? dest.set(this.matrix.setPerspective(this.perspectiveFov * (float) (Math.PI / 180.0), this.width / this.height, near, far, zZeroToOne))
         : dest.set(
            this.matrix.setOrtho(0.0F, this.width, this.orthoInvertY ? this.height : 0.0F, this.orthoInvertY ? 0.0F : this.height, near, far, zZeroToOne)
         );
   }

   public long getMatrixVersion() {
      return this.isMatrixDirty ? this.matrixVersion + 1L : this.matrixVersion;
   }

   public float zNear() {
      return this.zNear;
   }

   public float zFar() {
      return this.zFar;
   }

   public float width() {
      return this.width;
   }

   public float height() {
      return this.height;
   }

   public float fov() {
      return this.perspectiveFov;
   }

   public boolean invertY() {
      return this.orthoInvertY;
   }
}
