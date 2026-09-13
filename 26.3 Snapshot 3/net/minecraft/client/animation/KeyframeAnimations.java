package net.minecraft.client.animation;

import org.joml.Vector3f;

public class KeyframeAnimations {
   public static Vector3f posVec(final float x, final float y, final float z) {
      return new Vector3f(x, -y, z);
   }

   public static Vector3f degreeVec(final float x, final float y, final float z) {
      return new Vector3f(x * (float) (Math.PI / 180.0), y * (float) (Math.PI / 180.0), z * (float) (Math.PI / 180.0));
   }

   public static Vector3f scaleVec(final double x, final double y, final double z) {
      return new Vector3f((float)(x - 1.0), (float)(y - 1.0), (float)(z - 1.0));
   }
}
