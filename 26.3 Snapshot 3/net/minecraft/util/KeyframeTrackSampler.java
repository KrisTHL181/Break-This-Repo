package net.minecraft.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.attribute.LerpFunction;

public class KeyframeTrackSampler<T> {
   private final Optional<Integer> periodTicks;
   private final LerpFunction<T> lerp;
   private final List<KeyframeTrackSampler.Segment<T>> segments;

   KeyframeTrackSampler(final KeyframeTrack<T> track, final Optional<Integer> periodTicks, final LerpFunction<T> lerp) {
      this.periodTicks = periodTicks;
      this.lerp = lerp;
      this.segments = bakeSegments(track, periodTicks);
   }

   private static <T> List<KeyframeTrackSampler.Segment<T>> bakeSegments(final KeyframeTrack<T> track, final Optional<Integer> periodTicks) {
      List<Keyframe<T>> keyframes = track.keyframes();
      if (keyframes.size() == 1) {
         T value = keyframes.getFirst().value();
         return List.of(new KeyframeTrackSampler.Segment<>(EasingType.CONSTANT, value, 0, value, 0));
      }

      List<KeyframeTrackSampler.Segment<T>> segments = new ArrayList<>();
      if (periodTicks.isPresent()) {
         Keyframe<T> firstKeyframe = keyframes.getFirst();
         Keyframe<T> lastKeyframe = keyframes.getLast();
         segments.add(new KeyframeTrackSampler.Segment<>(track, lastKeyframe, lastKeyframe.ticks() - periodTicks.get(), firstKeyframe, firstKeyframe.ticks()));
         addSegmentsFromKeyframes(track, keyframes, segments);
         segments.add(new KeyframeTrackSampler.Segment<>(track, lastKeyframe, lastKeyframe.ticks(), firstKeyframe, firstKeyframe.ticks() + periodTicks.get()));
      } else {
         addSegmentsFromKeyframes(track, keyframes, segments);
      }

      return List.copyOf(segments);
   }

   private static <T> void addSegmentsFromKeyframes(
      final KeyframeTrack<T> track, final List<Keyframe<T>> keyframes, final List<KeyframeTrackSampler.Segment<T>> output
   ) {
      for (int i = 0; i < keyframes.size() - 1; i++) {
         Keyframe<T> keyframe = keyframes.get(i);
         Keyframe<T> nextKeyframe = keyframes.get(i + 1);
         output.add(new KeyframeTrackSampler.Segment<>(track, keyframe, keyframe.ticks(), nextKeyframe, nextKeyframe.ticks()));
      }
   }

   public T sample(final long ticks) {
      long sampleTicks = this.loopTicks(ticks);
      KeyframeTrackSampler.Segment<T> segment = this.getSegmentAt(sampleTicks);
      if (sampleTicks <= segment.fromTicks) {
         return segment.fromValue;
      }

      if (sampleTicks >= segment.toTicks) {
         return segment.toValue;
      }

      float alpha = (float)(sampleTicks - segment.fromTicks) / (segment.toTicks - segment.fromTicks);
      float easedAlpha = segment.easing.apply(alpha);
      return this.lerp.apply(easedAlpha, segment.fromValue, segment.toValue);
   }

   private KeyframeTrackSampler.Segment<T> getSegmentAt(final long currentTicks) {
      for (KeyframeTrackSampler.Segment<T> segment : this.segments) {
         if (currentTicks < segment.toTicks) {
            return segment;
         }
      }

      return this.segments.getLast();
   }

   private long loopTicks(final long ticks) {
      return this.periodTicks.isPresent() ? Math.floorMod(ticks, this.periodTicks.get()) : ticks;
   }

   private record Segment<T>(EasingType easing, T fromValue, int fromTicks, T toValue, int toTicks) {
      public Segment(final KeyframeTrack<T> track, final Keyframe<T> from, final int fromTicks, final Keyframe<T> to, final int toTicks) {
         this(track.easingType(), from.value(), fromTicks, to.value(), toTicks);
      }
   }
}
