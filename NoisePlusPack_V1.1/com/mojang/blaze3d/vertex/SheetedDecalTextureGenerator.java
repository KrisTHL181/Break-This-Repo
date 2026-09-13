package com.mojang.blaze3d.vertex;

import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class SheetedDecalTextureGenerator implements VertexConsumer {
   private final VertexConsumer delegate;
   private final Matrix4f cameraInversePose;
   private final Matrix3f normalInversePose;
   private final float textureScale;
   private final Vector3f worldPos = new Vector3f();
   private final Vector3f normal = new Vector3f();
   private float x;
   private float y;
   private float z;

   public SheetedDecalTextureGenerator(VertexConsumer p_260211_, PoseStack.Pose p_332899_, float p_259312_) {
      this.delegate = p_260211_;
      this.cameraInversePose = new Matrix4f(p_332899_.pose()).invert();
      this.normalInversePose = new Matrix3f(p_332899_.normal()).invert();
      this.textureScale = p_259312_;
   }

   @Override
   public VertexConsumer addVertex(float p_345104_, float p_342988_, float p_342152_) {
      this.x = p_345104_;
      this.y = p_342988_;
      this.z = p_342152_;
      this.delegate.addVertex(p_345104_, p_342988_, p_342152_);
      return this;
   }

   @Override
   public VertexConsumer setColor(int p_344386_, int p_345260_, int p_344616_, int p_345057_) {
      this.delegate.setColor(-1);
      return this;
   }

   @Override
   public VertexConsumer setColor(int p_455856_) {
      this.delegate.setColor(-1);
      return this;
   }

   @Override
   public VertexConsumer setUv(float p_343310_, float p_343059_) {
      return this;
   }

   @Override
   public VertexConsumer setUv1(int p_344277_, int p_343886_) {
      this.delegate.setUv1(p_344277_, p_343886_);
      return this;
   }

   @Override
   public VertexConsumer setUv2(int p_342602_, int p_345062_) {
      this.delegate.setUv2(p_342602_, p_345062_);
      return this;
   }

   @Override
   public VertexConsumer setNormal(float p_344306_, float p_342091_, float p_344579_) {
      this.delegate.setNormal(p_344306_, p_342091_, p_344579_);
      Vector3f vector3f = this.normalInversePose.transform(p_344306_, p_342091_, p_344579_, this.normal);
      Direction direction = Direction.getApproximateNearest(vector3f.x(), vector3f.y(), vector3f.z());
      Vector3f vector3f1 = this.cameraInversePose.transformPosition(this.x, this.y, this.z, this.worldPos);
      vector3f1.rotateY((float) Math.PI);
      vector3f1.rotateX((float) (-Math.PI / 2));
      vector3f1.rotate(direction.getRotation());
      this.delegate.setUv(-vector3f1.x() * this.textureScale, -vector3f1.y() * this.textureScale);
      return this;
   }

   @Override
   public VertexConsumer setLineWidth(float p_461060_) {
      this.delegate.setLineWidth(p_461060_);
      return this;
   }
}
