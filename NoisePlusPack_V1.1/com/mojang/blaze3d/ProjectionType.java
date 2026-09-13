package com.mojang.blaze3d;

import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public enum ProjectionType {
   PERSPECTIVE(VertexSorting.DISTANCE_TO_ORIGIN, (p_363734_, p_365662_) -> p_363734_.scale(1.0F - p_365662_ / 4096.0F)),
   ORTHOGRAPHIC(VertexSorting.ORTHOGRAPHIC_Z, (p_361478_, p_367053_) -> p_361478_.translate(0.0F, 0.0F, p_367053_ / 512.0F));

   private final VertexSorting vertexSorting;
   private final ProjectionType.LayeringTransform layeringTransform;

   ProjectionType(final VertexSorting p_370067_, final ProjectionType.LayeringTransform p_367962_) {
      this.vertexSorting = p_370067_;
      this.layeringTransform = p_367962_;
   }

   public VertexSorting vertexSorting() {
      return this.vertexSorting;
   }

   public void applyLayeringTransform(Matrix4f p_364350_, float p_368134_) {
      this.layeringTransform.apply(p_364350_, p_368134_);
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   interface LayeringTransform {
      void apply(Matrix4f var1, float var2);
   }
}
