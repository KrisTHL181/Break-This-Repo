package net.minecraft.client.animation;

import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;

public record AnimationDefinition(float lengthInSeconds, boolean looping, Map<String, List<AnimationChannel>> boneAnimations) {
   public KeyframeAnimation bake(final ModelPart root) {
      return KeyframeAnimation.bake(root, this);
   }

   public static class Builder {
      private final float length;
      private final Map<String, List<AnimationChannel>> animationByBone = Maps.newHashMap();
      private boolean looping;

      public static AnimationDefinition.Builder withLength(final float lengthInSeconds) {
         return new AnimationDefinition.Builder(lengthInSeconds);
      }

      private Builder(final float length) {
         this.length = length;
      }

      public AnimationDefinition.Builder looping() {
         this.looping = true;
         return this;
      }

      public AnimationDefinition.Builder addAnimation(final String boneName, final AnimationChannel animation) {
         this.animationByBone.computeIfAbsent(boneName, k -> new ArrayList<>()).add(animation);
         return this;
      }

      public AnimationDefinition build() {
         return new AnimationDefinition(this.length, this.looping, this.animationByBone);
      }
   }
}
