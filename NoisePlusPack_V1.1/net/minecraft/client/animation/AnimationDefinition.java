package net.minecraft.client.animation;

import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record AnimationDefinition(float lengthInSeconds, boolean looping, Map<String, List<AnimationChannel>> boneAnimations) {
   public KeyframeAnimation bake(ModelPart p_409469_) {
      return KeyframeAnimation.bake(p_409469_, this);
   }

   @OnlyIn(Dist.CLIENT)
   public static class Builder {
      private final float length;
      private final Map<String, List<AnimationChannel>> animationByBone = Maps.newHashMap();
      private boolean looping;

      public static AnimationDefinition.Builder withLength(float p_232276_) {
         return new AnimationDefinition.Builder(p_232276_);
      }

      private Builder(float p_232273_) {
         this.length = p_232273_;
      }

      public AnimationDefinition.Builder looping() {
         this.looping = true;
         return this;
      }

      public AnimationDefinition.Builder addAnimation(String p_232280_, AnimationChannel p_232281_) {
         this.animationByBone.computeIfAbsent(p_232280_, p_325294_ -> new ArrayList<>()).add(p_232281_);
         return this;
      }

      public AnimationDefinition build() {
         return new AnimationDefinition(this.length, this.looping, this.animationByBone);
      }
   }
}
