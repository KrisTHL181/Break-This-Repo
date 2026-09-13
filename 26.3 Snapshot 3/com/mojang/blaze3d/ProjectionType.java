package com.mojang.blaze3d;

import com.mojang.blaze3d.vertex.VertexSorting;
import org.joml.Matrix4f;

public enum ProjectionType {
   PERSPECTIVE(VertexSorting.DISTANCE_TO_ORIGIN, (matrix, bias) -> matrix.scale(1.0F - bias / 4096.0F)),
   ORTHOGRAPHIC(VertexSorting.ORTHOGRAPHIC_Z, (matrix, bias) -> matrix.translate(0.0F, 0.0F, bias / 512.0F));

   private final VertexSorting vertexSorting;
   private final ProjectionType.LayeringTransform layeringTransform;

   ProjectionType(final VertexSorting vertexSorting, final ProjectionType.LayeringTransform layeringTransform) {
      this.vertexSorting = vertexSorting;
      this.layeringTransform = layeringTransform;
   }

   public VertexSorting vertexSorting() {
      return this.vertexSorting;
   }

   public void applyLayeringTransform(final Matrix4f matrix, final float bias) {
      this.layeringTransform.apply(matrix, bias);
   }

   @FunctionalInterface
   private interface LayeringTransform {
      void apply(Matrix4f matrix, float bias);
   }
}
