package net.minecraft.util;

import com.google.common.collect.Comparators;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.attribute.LerpFunction;

public record KeyframeTrack<T>(List<Keyframe<T>> keyframes, EasingType easingType) {
   public KeyframeTrack {
      if (keyframes.isEmpty()) {
         throw new IllegalArgumentException("Track has no keyframes");
      }
   }

   public static <T> MapCodec<KeyframeTrack<T>> mapCodec(Codec<T> p_454462_) {
      Codec<List<Keyframe<T>>> codec = Keyframe.codec(p_454462_).listOf().validate(KeyframeTrack::validateKeyframes);
      return RecordCodecBuilder.mapCodec(
         p_450913_ -> p_450913_.group(
               codec.fieldOf("keyframes").forGetter(KeyframeTrack::keyframes),
               EasingType.CODEC.optionalFieldOf("ease", EasingType.LINEAR).forGetter(KeyframeTrack::easingType)
            )
            .apply(p_450913_, KeyframeTrack::new)
      );
   }

   static <T> DataResult<List<Keyframe<T>>> validateKeyframes(List<Keyframe<T>> p_454646_) {
      if (p_454646_.isEmpty()) {
         return DataResult.error(() -> "Keyframes must not be empty");
      }

      if (!Comparators.isInOrder(p_454646_, Comparator.comparingInt(Keyframe::ticks))) {
         return DataResult.error(() -> "Keyframes must be ordered by ticks field");
      }

      if (p_454646_.size() > 1) {
         int i = 0;
         int j = p_454646_.getLast().ticks();

         for (Keyframe<T> keyframe : p_454646_) {
            if (keyframe.ticks() == j) {
               if (++i > 2) {
                  return DataResult.error(() -> "More than 2 keyframes on same tick: " + keyframe.ticks());
               }
            } else {
               i = 0;
            }

            j = keyframe.ticks();
         }
      }

      return DataResult.success(p_454646_);
   }

   public static DataResult<KeyframeTrack<?>> validatePeriod(KeyframeTrack<?> p_450421_, int p_454379_) {
      for (Keyframe<?> keyframe : p_450421_.keyframes()) {
         int i = keyframe.ticks();
         if (i < 0 || i > p_454379_) {
            return DataResult.error(() -> "Keyframe at tick " + keyframe.ticks() + " must be in range [0; " + p_454379_ + "]");
         }
      }

      return DataResult.success(p_450421_);
   }

   public KeyframeTrackSampler<T> bakeSampler(Optional<Integer> p_452848_, LerpFunction<T> p_450985_) {
      return new KeyframeTrackSampler<>(this, p_452848_, p_450985_);
   }

   public static class Builder<T> {
      private final com.google.common.collect.ImmutableList.Builder<Keyframe<T>> keyframes = ImmutableList.builder();
      private EasingType easing = EasingType.LINEAR;

      public KeyframeTrack.Builder<T> addKeyframe(int p_450573_, T p_459426_) {
         this.keyframes.add(new Keyframe(p_450573_, p_459426_));
         return this;
      }

      public KeyframeTrack.Builder<T> setEasing(EasingType p_452845_) {
         this.easing = p_452845_;
         return this;
      }

      public KeyframeTrack<T> build() {
         List<Keyframe<T>> list = (List<Keyframe<T>>)KeyframeTrack.validateKeyframes(this.keyframes.build()).getOrThrow();
         return new KeyframeTrack<>(list, this.easing);
      }
   }
}
