package com.mojang.blaze3d.vertex;

import com.google.common.primitives.Floats;
import it.unimi.dsi.fastutil.ints.IntArrays;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public interface VertexSorting {
   VertexSorting DISTANCE_TO_ORIGIN = byDistance(0.0F, 0.0F, 0.0F);
   VertexSorting ORTHOGRAPHIC_Z = byDistance(point -> -point.z());

   static VertexSorting byDistance(final float x, final float y, final float z) {
      return byDistance(new Vector3f(x, y, z));
   }

   static VertexSorting byDistance(final Vector3fc origin) {
      return byDistance(origin::distanceSquared);
   }

   static VertexSorting byDistance(final VertexSorting.DistanceFunction function) {
      return values -> {
         Vector3f scratch = new Vector3f();
         float[] keys = new float[values.size()];
         int[] indices = new int[values.size()];

         for (int i = 0; i < values.size(); indices[i] = i++) {
            keys[i] = function.apply(values.get(i, scratch));
         }

         IntArrays.mergeSort(indices, (o1, o2) -> Floats.compare(keys[o2], keys[o1]));
         return indices;
      };
   }

   int[] sort(CompactVectorArray points);

   @FunctionalInterface
   interface DistanceFunction {
      float apply(Vector3f value);
   }
}
