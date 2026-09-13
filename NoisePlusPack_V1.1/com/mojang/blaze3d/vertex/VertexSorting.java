package com.mojang.blaze3d.vertex;

import com.google.common.primitives.Floats;
import it.unimi.dsi.fastutil.ints.IntArrays;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public interface VertexSorting {
   VertexSorting DISTANCE_TO_ORIGIN = byDistance(0.0F, 0.0F, 0.0F);
   VertexSorting ORTHOGRAPHIC_Z = byDistance(p_277433_ -> -p_277433_.z());

   static VertexSorting byDistance(float p_277642_, float p_277654_, float p_278092_) {
      return byDistance(new Vector3f(p_277642_, p_277654_, p_278092_));
   }

   static VertexSorting byDistance(Vector3fc p_426625_) {
      return byDistance(p_426625_::distanceSquared);
   }

   static VertexSorting byDistance(VertexSorting.DistanceFunction p_277530_) {
      return p_420581_ -> {
         Vector3f vector3f = new Vector3f();
         float[] afloat = new float[p_420581_.size()];
         int[] aint = new int[p_420581_.size()];

         for (int i = 0; i < p_420581_.size(); aint[i] = i++) {
            afloat[i] = p_277530_.apply(p_420581_.get(i, vector3f));
         }

         IntArrays.mergeSort(aint, (p_277443_, p_277864_) -> Floats.compare(afloat[p_277864_], afloat[p_277443_]));
         return aint;
      };
   }

   int[] sort(CompactVectorArray var1);

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   interface DistanceFunction {
      float apply(Vector3f var1);
   }
}
