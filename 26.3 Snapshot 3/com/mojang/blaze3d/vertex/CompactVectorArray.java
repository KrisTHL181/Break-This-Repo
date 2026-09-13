package com.mojang.blaze3d.vertex;

import org.joml.Vector3f;
import org.joml.Vector3fc;

public class CompactVectorArray {
   private static final int VECTOR_SIZE = 3;
   private static final int OFFSET_X = 0;
   private static final int OFFSET_Y = 1;
   private static final int OFFSET_Z = 2;
   private final float[] contents;

   public CompactVectorArray(final int count) {
      this.contents = new float[3 * count];
   }

   public int size() {
      return this.contents.length / 3;
   }

   public void set(final int index, final Vector3fc v) {
      this.set(index, v.x(), v.y(), v.z());
   }

   public void set(final int index, final float x, final float y, final float z) {
      this.contents[3 * index + 0] = x;
      this.contents[3 * index + 1] = y;
      this.contents[3 * index + 2] = z;
   }

   public Vector3f get(final int index, final Vector3f output) {
      return output.set(this.contents[3 * index + 0], this.contents[3 * index + 1], this.contents[3 * index + 2]);
   }

   public float getX(final int index) {
      return this.contents[3 * index + 0];
   }

   public float getY(final int index) {
      return this.contents[3 * index + 1];
   }

   public float getZ(final int index) {
      return this.contents[3 * index + 2];
   }
}
