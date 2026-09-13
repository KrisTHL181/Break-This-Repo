package com.mojang.blaze3d.vertex;

import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.ARGB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3x2fc;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public interface VertexConsumer {
   VertexConsumer addVertex(float var1, float var2, float var3);

   VertexConsumer setColor(int var1, int var2, int var3, int var4);

   VertexConsumer setColor(int var1);

   VertexConsumer setUv(float var1, float var2);

   VertexConsumer setUv1(int var1, int var2);

   VertexConsumer setUv2(int var1, int var2);

   VertexConsumer setNormal(float var1, float var2, float var3);

   VertexConsumer setLineWidth(float var1);

   default void addVertex(
      float p_342335_,
      float p_342594_,
      float p_342395_,
      int p_344436_,
      float p_344317_,
      float p_344558_,
      int p_344862_,
      int p_343109_,
      float p_343232_,
      float p_342995_,
      float p_343739_
   ) {
      this.addVertex(p_342335_, p_342594_, p_342395_);
      this.setColor(p_344436_);
      this.setUv(p_344317_, p_344558_);
      this.setOverlay(p_344862_);
      this.setLight(p_343109_);
      this.setNormal(p_343232_, p_342995_, p_343739_);
   }

   default VertexConsumer setColor(float p_345344_, float p_343040_, float p_343668_, float p_342740_) {
      return this.setColor((int)(p_345344_ * 255.0F), (int)(p_343040_ * 255.0F), (int)(p_343668_ * 255.0F), (int)(p_342740_ * 255.0F));
   }

   default VertexConsumer setLight(int p_342385_) {
      return this.setUv2(p_342385_ & 65535, p_342385_ >> 16 & 65535);
   }

   default VertexConsumer setOverlay(int p_345433_) {
      return this.setUv1(p_345433_ & 65535, p_345433_ >> 16 & 65535);
   }

   default void putBulkData(
      PoseStack.Pose p_85996_, BakedQuad p_85997_, float p_85999_, float p_86000_, float p_86001_, float p_330684_, int p_86003_, int p_332867_
   ) {
      this.putBulkData(
         p_85996_,
         p_85997_,
         new float[]{1.0F, 1.0F, 1.0F, 1.0F},
         p_85999_,
         p_86000_,
         p_86001_,
         p_330684_,
         new int[]{p_86003_, p_86003_, p_86003_, p_86003_},
         p_332867_
      );
   }

   default void putBulkData(
      PoseStack.Pose p_85988_,
      BakedQuad p_85989_,
      float[] p_331915_,
      float p_85990_,
      float p_85991_,
      float p_85992_,
      float p_335371_,
      int[] p_331444_,
      int p_85993_
   ) {
      Vector3fc vector3fc = p_85989_.direction().getUnitVec3f();
      Matrix4f matrix4f = p_85988_.pose();
      Vector3f vector3f = p_85988_.transformNormal(vector3fc, new Vector3f());
      int i = p_85989_.lightEmission();

      for (int j = 0; j < 4; j++) {
         Vector3fc vector3fc1 = p_85989_.position(j);
         long k = p_85989_.packedUV(j);
         float f = p_331915_[j];
         int l = ARGB.colorFromFloat(p_335371_, f * p_85990_, f * p_85991_, f * p_85992_);
         int i1 = LightTexture.lightCoordsWithEmission(p_331444_[j], i);
         Vector3f vector3f1 = matrix4f.transformPosition(vector3fc1, new Vector3f());
         float f1 = UVPair.unpackU(k);
         float f2 = UVPair.unpackV(k);
         this.addVertex(vector3f1.x(), vector3f1.y(), vector3f1.z(), l, f1, f2, p_85993_, i1, vector3f.x(), vector3f.y(), vector3f.z());
      }
   }

   default VertexConsumer addVertex(Vector3fc p_451019_) {
      return this.addVertex(p_451019_.x(), p_451019_.y(), p_451019_.z());
   }

   default VertexConsumer addVertex(PoseStack.Pose p_343718_, Vector3f p_344795_) {
      return this.addVertex(p_343718_, p_344795_.x(), p_344795_.y(), p_344795_.z());
   }

   default VertexConsumer addVertex(PoseStack.Pose p_343203_, float p_343315_, float p_342573_, float p_344986_) {
      return this.addVertex(p_343203_.pose(), p_343315_, p_342573_, p_344986_);
   }

   default VertexConsumer addVertex(Matrix4fc p_460886_, float p_342636_, float p_342677_, float p_343814_) {
      Vector3f vector3f = p_460886_.transformPosition(p_342636_, p_342677_, p_343814_, new Vector3f());
      return this.addVertex(vector3f.x(), vector3f.y(), vector3f.z());
   }

   default VertexConsumer addVertexWith2DPose(Matrix3x2fc p_460275_, float p_406462_, float p_406232_) {
      Vector2f vector2f = p_460275_.transformPosition(p_406462_, p_406232_, new Vector2f());
      return this.addVertex(vector2f.x(), vector2f.y(), 0.0F);
   }

   default VertexConsumer setNormal(PoseStack.Pose p_343706_, float p_345121_, float p_344892_, float p_344341_) {
      Vector3f vector3f = p_343706_.transformNormal(p_345121_, p_344892_, p_344341_, new Vector3f());
      return this.setNormal(vector3f.x(), vector3f.y(), vector3f.z());
   }

   default VertexConsumer setNormal(PoseStack.Pose p_369767_, Vector3f p_366727_) {
      return this.setNormal(p_369767_, p_366727_.x(), p_366727_.y(), p_366727_.z());
   }
}
