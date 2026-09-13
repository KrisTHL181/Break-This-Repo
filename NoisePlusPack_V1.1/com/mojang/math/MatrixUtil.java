package com.mojang.math;

import org.apache.commons.lang3.tuple.Triple;
import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class MatrixUtil {
   private static final float G = 3.0F + 2.0F * Math.sqrt(2.0F);
   private static final GivensParameters PI_4 = GivensParameters.fromPositiveAngle((float) (java.lang.Math.PI / 4));

   private MatrixUtil() {
   }

   public static Matrix4f mulComponentWise(Matrix4f p_254173_, float p_253864_) {
      return p_254173_.set(
         p_254173_.m00() * p_253864_,
         p_254173_.m01() * p_253864_,
         p_254173_.m02() * p_253864_,
         p_254173_.m03() * p_253864_,
         p_254173_.m10() * p_253864_,
         p_254173_.m11() * p_253864_,
         p_254173_.m12() * p_253864_,
         p_254173_.m13() * p_253864_,
         p_254173_.m20() * p_253864_,
         p_254173_.m21() * p_253864_,
         p_254173_.m22() * p_253864_,
         p_254173_.m23() * p_253864_,
         p_254173_.m30() * p_253864_,
         p_254173_.m31() * p_253864_,
         p_254173_.m32() * p_253864_,
         p_254173_.m33() * p_253864_
      );
   }

   private static GivensParameters approxGivensQuat(float p_276275_, float p_276276_, float p_276282_) {
      float f = 2.0F * (p_276275_ - p_276282_);
      return G * p_276276_ * p_276276_ < f * f ? GivensParameters.fromUnnormalized(p_276276_, f) : PI_4;
   }

   private static GivensParameters qrGivensQuat(float p_253897_, float p_254413_) {
      float f = (float)java.lang.Math.hypot(p_253897_, p_254413_);
      float f1 = f > 1.0E-6F ? p_254413_ : 0.0F;
      float f2 = Math.abs(p_253897_) + Math.max(f, 1.0E-6F);
      if (p_253897_ < 0.0F) {
         float f3 = f1;
         f1 = f2;
         f2 = f3;
      }

      return GivensParameters.fromUnnormalized(f1, f2);
   }

   private static void similarityTransform(Matrix3f p_276319_, Matrix3f p_276263_) {
      p_276319_.mul(p_276263_);
      p_276263_.transpose();
      p_276263_.mul(p_276319_);
      p_276319_.set(p_276263_);
   }

   private static void stepJacobi(Matrix3f p_276262_, Matrix3f p_276279_, Quaternionf p_276314_, Quaternionf p_276299_) {
      if (p_276262_.m01 * p_276262_.m01 + p_276262_.m10 * p_276262_.m10 > 1.0E-6F) {
         GivensParameters givensparameters = approxGivensQuat(p_276262_.m00, 0.5F * (p_276262_.m01 + p_276262_.m10), p_276262_.m11);
         Quaternionf quaternionf = givensparameters.aroundZ(p_276314_);
         p_276299_.mul(quaternionf);
         givensparameters.aroundZ(p_276279_);
         similarityTransform(p_276262_, p_276279_);
      }

      if (p_276262_.m02 * p_276262_.m02 + p_276262_.m20 * p_276262_.m20 > 1.0E-6F) {
         GivensParameters givensparameters1 = approxGivensQuat(p_276262_.m00, 0.5F * (p_276262_.m02 + p_276262_.m20), p_276262_.m22).inverse();
         Quaternionf quaternionf1 = givensparameters1.aroundY(p_276314_);
         p_276299_.mul(quaternionf1);
         givensparameters1.aroundY(p_276279_);
         similarityTransform(p_276262_, p_276279_);
      }

      if (p_276262_.m12 * p_276262_.m12 + p_276262_.m21 * p_276262_.m21 > 1.0E-6F) {
         GivensParameters givensparameters2 = approxGivensQuat(p_276262_.m11, 0.5F * (p_276262_.m12 + p_276262_.m21), p_276262_.m22);
         Quaternionf quaternionf2 = givensparameters2.aroundX(p_276314_);
         p_276299_.mul(quaternionf2);
         givensparameters2.aroundX(p_276279_);
         similarityTransform(p_276262_, p_276279_);
      }
   }

   public static Quaternionf eigenvalueJacobi(Matrix3f p_276278_, int p_276269_) {
      Quaternionf quaternionf = new Quaternionf();
      Matrix3f matrix3f = new Matrix3f();
      Quaternionf quaternionf1 = new Quaternionf();

      for (int i = 0; i < p_276269_; i++) {
         stepJacobi(p_276278_, matrix3f, quaternionf1, quaternionf);
      }

      quaternionf.normalize();
      return quaternionf;
   }

   public static Triple<Quaternionf, Vector3f, Quaternionf> svdDecompose(Matrix3f p_253947_) {
      Matrix3f matrix3f = new Matrix3f(p_253947_);
      matrix3f.transpose();
      matrix3f.mul(p_253947_);
      Quaternionf quaternionf = eigenvalueJacobi(matrix3f, 5);
      float f = matrix3f.m00;
      float f1 = matrix3f.m11;
      boolean flag = f < 1.0E-6;
      boolean flag1 = f1 < 1.0E-6;
      Matrix3f matrix3f1 = p_253947_.rotate(quaternionf);
      Quaternionf quaternionf1 = new Quaternionf();
      Quaternionf quaternionf2 = new Quaternionf();
      GivensParameters givensparameters;
      if (flag) {
         givensparameters = qrGivensQuat(matrix3f1.m11, -matrix3f1.m10);
      } else {
         givensparameters = qrGivensQuat(matrix3f1.m00, matrix3f1.m01);
      }

      Quaternionf quaternionf3 = givensparameters.aroundZ(quaternionf2);
      Matrix3f matrix3f2 = givensparameters.aroundZ(matrix3f);
      quaternionf1.mul(quaternionf3);
      matrix3f2.transpose().mul(matrix3f1);
      if (flag) {
         givensparameters = qrGivensQuat(matrix3f2.m22, -matrix3f2.m20);
      } else {
         givensparameters = qrGivensQuat(matrix3f2.m00, matrix3f2.m02);
      }

      givensparameters = givensparameters.inverse();
      Quaternionf quaternionf4 = givensparameters.aroundY(quaternionf2);
      Matrix3f matrix3f3 = givensparameters.aroundY(matrix3f1);
      quaternionf1.mul(quaternionf4);
      matrix3f3.transpose().mul(matrix3f2);
      if (flag1) {
         givensparameters = qrGivensQuat(matrix3f3.m22, -matrix3f3.m21);
      } else {
         givensparameters = qrGivensQuat(matrix3f3.m11, matrix3f3.m12);
      }

      Quaternionf quaternionf5 = givensparameters.aroundX(quaternionf2);
      Matrix3f matrix3f4 = givensparameters.aroundX(matrix3f2);
      quaternionf1.mul(quaternionf5);
      matrix3f4.transpose().mul(matrix3f3);
      Vector3f vector3f = new Vector3f(matrix3f4.m00, matrix3f4.m11, matrix3f4.m22);
      return Triple.of(quaternionf1, vector3f, quaternionf.conjugate());
   }

   private static boolean checkPropertyRaw(Matrix4fc p_397336_, int p_395006_) {
      return (p_397336_.properties() & p_395006_) != 0;
   }

   public static boolean checkProperty(Matrix4fc p_393322_, int p_393960_) {
      if (checkPropertyRaw(p_393322_, p_393960_)) {
         return true;
      } else if (p_393322_ instanceof Matrix4f matrix4f) {
         matrix4f.determineProperties();
         return checkPropertyRaw(p_393322_, p_393960_);
      } else {
         return false;
      }
   }

   public static boolean isIdentity(Matrix4fc p_397909_) {
      return checkProperty(p_397909_, 4);
   }

   public static boolean isPureTranslation(Matrix4fc p_391561_) {
      return checkProperty(p_391561_, 8);
   }

   public static boolean isOrthonormal(Matrix4fc p_394875_) {
      return checkProperty(p_394875_, 16);
   }
}
