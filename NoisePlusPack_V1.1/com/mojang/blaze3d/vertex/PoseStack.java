package com.mojang.blaze3d.vertex;

import com.mojang.math.MatrixUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public class PoseStack {
   private final List<PoseStack.Pose> poses = new ArrayList<>(16);
   private int lastIndex;

   public PoseStack() {
      this.poses.add(new PoseStack.Pose());
   }

   public void translate(double p_85838_, double p_85839_, double p_85840_) {
      this.translate((float)p_85838_, (float)p_85839_, (float)p_85840_);
   }

   public void translate(float p_254202_, float p_253782_, float p_254238_) {
      this.last().translate(p_254202_, p_253782_, p_254238_);
   }

   public void translate(Vec3 p_362933_) {
      this.translate(p_362933_.x, p_362933_.y, p_362933_.z);
   }

   public void scale(float p_85842_, float p_85843_, float p_85844_) {
      this.last().scale(p_85842_, p_85843_, p_85844_);
   }

   public void mulPose(Quaternionfc p_397500_) {
      this.last().rotate(p_397500_);
   }

   public void rotateAround(Quaternionfc p_394782_, float p_273581_, float p_272655_, float p_273275_) {
      this.last().rotateAround(p_394782_, p_273581_, p_272655_, p_273275_);
   }

   public void pushPose() {
      PoseStack.Pose posestack$pose = this.last();
      this.lastIndex++;
      if (this.lastIndex >= this.poses.size()) {
         this.poses.add(posestack$pose.copy());
      } else {
         this.poses.get(this.lastIndex).set(posestack$pose);
      }
   }

   public void popPose() {
      if (this.lastIndex == 0) {
         throw new NoSuchElementException();
      }

      this.lastIndex--;
   }

   public PoseStack.Pose last() {
      return this.poses.get(this.lastIndex);
   }

   public boolean isEmpty() {
      return this.lastIndex == 0;
   }

   public void setIdentity() {
      this.last().setIdentity();
   }

   public void mulPose(Matrix4fc p_393889_) {
      this.last().mulPose(p_393889_);
   }

   @OnlyIn(Dist.CLIENT)
   public static final class Pose {
      private final Matrix4f pose = new Matrix4f();
      private final Matrix3f normal = new Matrix3f();
      private boolean trustedNormals = true;

      private void computeNormalMatrix() {
         this.normal.set(this.pose).invert().transpose();
         this.trustedNormals = false;
      }

      public void set(PoseStack.Pose p_395588_) {
         this.pose.set(p_395588_.pose);
         this.normal.set(p_395588_.normal);
         this.trustedNormals = p_395588_.trustedNormals;
      }

      public Matrix4f pose() {
         return this.pose;
      }

      public Matrix3f normal() {
         return this.normal;
      }

      public Vector3f transformNormal(Vector3fc p_392702_, Vector3f p_332767_) {
         return this.transformNormal(p_392702_.x(), p_392702_.y(), p_392702_.z(), p_332767_);
      }

      public Vector3f transformNormal(float p_333912_, float p_334796_, float p_329732_, Vector3f p_328781_) {
         Vector3f vector3f = this.normal.transform(p_333912_, p_334796_, p_329732_, p_328781_);
         return this.trustedNormals ? vector3f : vector3f.normalize();
      }

      public Matrix4f translate(float p_396834_, float p_393738_, float p_391176_) {
         return this.pose.translate(p_396834_, p_393738_, p_391176_);
      }

      public void scale(float p_396924_, float p_391266_, float p_393160_) {
         this.pose.scale(p_396924_, p_391266_, p_393160_);
         if (Math.abs(p_396924_) != Math.abs(p_391266_) || Math.abs(p_391266_) != Math.abs(p_393160_)) {
            this.normal.scale(1.0F / p_396924_, 1.0F / p_391266_, 1.0F / p_393160_);
            this.trustedNormals = false;
         } else if (p_396924_ < 0.0F || p_391266_ < 0.0F || p_393160_ < 0.0F) {
            this.normal.scale(Math.signum(p_396924_), Math.signum(p_391266_), Math.signum(p_393160_));
         }
      }

      public void rotate(Quaternionfc p_394066_) {
         this.pose.rotate(p_394066_);
         this.normal.rotate(p_394066_);
      }

      public void rotateAround(Quaternionfc p_396539_, float p_392991_, float p_392162_, float p_391807_) {
         this.pose.rotateAround(p_396539_, p_392991_, p_392162_, p_391807_);
         this.normal.rotate(p_396539_);
      }

      public void setIdentity() {
         this.pose.identity();
         this.normal.identity();
         this.trustedNormals = true;
      }

      public void mulPose(Matrix4fc p_397709_) {
         this.pose.mul(p_397709_);
         if (!MatrixUtil.isPureTranslation(p_397709_)) {
            if (MatrixUtil.isOrthonormal(p_397709_)) {
               this.normal.mul(new Matrix3f(p_397709_));
            } else {
               this.computeNormalMatrix();
            }
         }
      }

      public PoseStack.Pose copy() {
         PoseStack.Pose posestack$pose = new PoseStack.Pose();
         posestack$pose.set(this);
         return posestack$pose;
      }
   }
}
