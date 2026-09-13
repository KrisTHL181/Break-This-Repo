package net.minecraft.client.animation;

import org.joml.Vector3fc;

public record Keyframe(float timestamp, Vector3fc preTarget, Vector3fc postTarget, AnimationChannel.Interpolation interpolation) {
   public Keyframe(final float timestamp, final Vector3fc postTarget, final AnimationChannel.Interpolation interpolation) {
      this(timestamp, postTarget, postTarget, interpolation);
   }
}
