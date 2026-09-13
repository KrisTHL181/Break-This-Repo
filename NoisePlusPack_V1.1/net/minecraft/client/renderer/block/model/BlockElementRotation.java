package net.minecraft.client.renderer.block.model;

import com.mojang.math.MatrixUtil;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public record BlockElementRotation(Vector3fc origin, BlockElementRotation.RotationValue value, boolean rescale, Matrix4fc transform) {
   public BlockElementRotation(Vector3fc p_453182_, BlockElementRotation.RotationValue p_452495_, boolean p_254029_) {
      this(p_453182_, p_452495_, p_254029_, computeTransform(p_452495_, p_254029_));
   }

   private static Matrix4f computeTransform(BlockElementRotation.RotationValue p_458124_, boolean p_450744_) {
      Matrix4f matrix4f = p_458124_.transformation();
      if (p_450744_ && !MatrixUtil.isIdentity(matrix4f)) {
         Vector3fc vector3fc = computeRescale(matrix4f);
         matrix4f.scale(vector3fc);
      }

      return matrix4f;
   }

   private static Vector3fc computeRescale(Matrix4fc p_450916_) {
      Vector3f vector3f = new Vector3f();
      float f = scaleFactorForAxis(p_450916_, Direction.Axis.X, vector3f);
      float f1 = scaleFactorForAxis(p_450916_, Direction.Axis.Y, vector3f);
      float f2 = scaleFactorForAxis(p_450916_, Direction.Axis.Z, vector3f);
      return vector3f.set(f, f1, f2);
   }

   private static float scaleFactorForAxis(Matrix4fc p_456637_, Direction.Axis p_456089_, Vector3f p_453554_) {
      Vector3f vector3f = p_453554_.set(p_456089_.getPositive().getUnitVec3f());
      Vector3f vector3f1 = p_456637_.transformDirection(vector3f);
      float f = Math.abs(vector3f1.x);
      float f1 = Math.abs(vector3f1.y);
      float f2 = Math.abs(vector3f1.z);
      float f3 = Math.max(Math.max(f, f1), f2);
      return 1.0F / f3;
   }

   @OnlyIn(Dist.CLIENT)
   public record EulerXYZRotation(float x, float y, float z) implements BlockElementRotation.RotationValue {
      @Override
      public Matrix4f transformation() {
         return new Matrix4f()
            .rotationZYX(
               this.z * (float) (java.lang.Math.PI / 180.0), this.y * (float) (java.lang.Math.PI / 180.0), this.x * (float) (java.lang.Math.PI / 180.0)
            );
      }
   }

   @OnlyIn(Dist.CLIENT)
   public interface RotationValue {
      Matrix4f transformation();
   }

   @OnlyIn(Dist.CLIENT)
   public record SingleAxisRotation(Direction.Axis axis, float angle) implements BlockElementRotation.RotationValue {
      @Override
      public Matrix4f transformation() {
         Matrix4f matrix4f = new Matrix4f();
         if (this.angle == 0.0F) {
            return matrix4f;
         }

         Vector3fc vector3fc = this.axis.getPositive().getUnitVec3f();
         matrix4f.rotation(this.angle * (float) (java.lang.Math.PI / 180.0), vector3fc);
         return matrix4f;
      }
   }
}
