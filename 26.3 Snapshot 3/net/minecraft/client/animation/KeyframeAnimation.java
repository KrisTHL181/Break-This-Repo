package net.minecraft.client.animation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.AnimationState;
import org.joml.Vector3f;

public class KeyframeAnimation {
   private final AnimationDefinition definition;
   private final List<KeyframeAnimation.Entry> entries;

   private KeyframeAnimation(final AnimationDefinition definition, final List<KeyframeAnimation.Entry> entries) {
      this.definition = definition;
      this.entries = entries;
   }

   static KeyframeAnimation bake(final ModelPart root, final AnimationDefinition definition) {
      List<KeyframeAnimation.Entry> entries = new ArrayList<>();
      Function<String, ModelPart> partLookup = root.createPartLookup();

      for (Map.Entry<String, List<AnimationChannel>> entry : definition.boneAnimations().entrySet()) {
         String partName = entry.getKey();
         List<AnimationChannel> channels = entry.getValue();
         ModelPart part = partLookup.apply(partName);
         if (part == null) {
            throw new IllegalArgumentException("Cannot animate " + partName + ", which does not exist in model");
         }

         for (AnimationChannel channel : channels) {
            entries.add(new KeyframeAnimation.Entry(part, channel.target(), channel.keyframes()));
         }
      }

      return new KeyframeAnimation(definition, List.copyOf(entries));
   }

   public void applyStatic() {
      this.apply(0L, 1.0F);
   }

   public void applyWalk(final float animationPos, final float animationSpeed, final float speedFactor, final float scaleFactor) {
      long time = (long)(animationPos * 50.0F * speedFactor);
      float scale = Math.min(animationSpeed * scaleFactor, 1.0F);
      this.apply(time, scale);
   }

   public void apply(final AnimationState animationState, final float currentTime) {
      this.apply(animationState, currentTime, 1.0F);
   }

   public void apply(final AnimationState animationState, final float currentTime, final float speedFactor) {
      animationState.ifStarted(state -> this.apply((long)((float)state.getTimeInMillis(currentTime) * speedFactor), 1.0F));
   }

   public void apply(final long millisSinceStart, final float targetScale) {
      float secondsSinceStart = this.getElapsedSeconds(millisSinceStart);
      Vector3f scratchVector = new Vector3f();

      for (KeyframeAnimation.Entry entry : this.entries) {
         entry.apply(secondsSinceStart, targetScale, scratchVector);
      }
   }

   private float getElapsedSeconds(final long millisSinceStart) {
      float secondsSinceStart = (float)millisSinceStart / 1000.0F;
      return this.definition.looping() ? secondsSinceStart % this.definition.lengthInSeconds() : secondsSinceStart;
   }

   private record Entry(ModelPart part, AnimationChannel.Target target, Keyframe[] keyframes) {
      public void apply(final float secondsSinceStart, final float targetScale, final Vector3f scratchVector) {
         int prev = Math.max(0, Mth.binarySearch(0, this.keyframes.length, i -> secondsSinceStart <= this.keyframes[i].timestamp()) - 1);
         int next = Math.min(this.keyframes.length - 1, prev + 1);
         Keyframe previousFrame = this.keyframes[prev];
         Keyframe nextFrame = this.keyframes[next];
         float keyframeTimeDelta = secondsSinceStart - previousFrame.timestamp();
         float lerpAlpha;
         if (next != prev) {
            lerpAlpha = Mth.clamp(keyframeTimeDelta / (nextFrame.timestamp() - previousFrame.timestamp()), 0.0F, 1.0F);
         } else {
            lerpAlpha = 0.0F;
         }

         nextFrame.interpolation().apply(scratchVector, lerpAlpha, this.keyframes, prev, next, targetScale);
         this.target.apply(this.part, scratchVector);
      }
   }
}
