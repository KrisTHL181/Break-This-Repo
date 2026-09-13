package net.minecraft.client.renderer.feature.submit;

import net.minecraft.client.renderer.feature.FeatureRendererType;
import net.minecraft.util.Mth;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

public interface TranslucentSubmit extends SubmitNode {
   float distanceToCameraSq();

   @Override
   FeatureRendererType<? extends TranslucentSubmit> featureType();

   static float computeDistanceToCameraSq(final Matrix4fc pose) {
      return Mth.lengthSquared(pose.m30(), pose.m31(), pose.m32());
   }

   static float computeDistanceToCameraSq(final Matrix4fc pose, final float originX, final float originY, final float originZ) {
      return pose.transformPosition(originX, originY, originZ, new Vector3f()).lengthSquared();
   }
}
