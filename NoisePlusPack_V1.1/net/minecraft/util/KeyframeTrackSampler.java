package net.minecraft.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.attribute.LerpFunction;

public class KeyframeTrackSampler<T> {
   private final Optional<Integer> periodTicks;
   private final LerpFunction<T> lerp;
   private final List<KeyframeTrackSampler.Segment<T>> segments;

   KeyframeTrackSampler(KeyframeTrack<T> p_458766_, Optional<Integer> p_451703_, LerpFunction<T> p_459156_) {
      this.periodTicks = p_451703_;
      this.lerp = p_459156_;
      this.segments = bakeSegments(p_458766_, p_451703_);
   }

   private static <T> List<KeyframeTrackSampler.Segment<T>> bakeSegments(KeyframeTrack<T> p_454303_, Optional<Integer> p_453183_) {
      List<Keyframe<T>> list = p_454303_.keyframes();
      if (list.size() == 1) {
         T t = list.getFirst().value();
         return List.of(new KeyframeTrackSampler.Segment<>(EasingType.CONSTANT, t, 0, t, 0));
      }

      List<KeyframeTrackSampler.Segment<T>> list1 = new ArrayList<>();
      if (p_453183_.isPresent()) {
         Keyframe<T> keyframe = list.getFirst();
         Keyframe<T> keyframe1 = list.getLast();
         list1.add(new KeyframeTrackSampler.Segment<>(p_454303_, keyframe1, keyframe1.ticks() - p_453183_.get(), keyframe, keyframe.ticks()));
         addSegmentsFromKeyframes(p_454303_, list, list1);
         list1.add(new KeyframeTrackSampler.Segment<>(p_454303_, keyframe1, keyframe1.ticks(), keyframe, keyframe.ticks() + p_453183_.get()));
      } else {
         addSegmentsFromKeyframes(p_454303_, list, list1);
      }

      return List.copyOf(list1);
   }

   private static <T> void addSegmentsFromKeyframes(KeyframeTrack<T> p_452523_, List<Keyframe<T>> p_451961_, List<KeyframeTrackSampler.Segment<T>> p_460278_) {
      for (int i = 0; i < p_451961_.size() - 1; i++) {
         Keyframe<T> keyframe = p_451961_.get(i);
         Keyframe<T> keyframe1 = p_451961_.get(i + 1);
         p_460278_.add(new KeyframeTrackSampler.Segment<>(p_452523_, keyframe, keyframe.ticks(), keyframe1, keyframe1.ticks()));
      }
   }

   public T sample(long p_453573_) {
      long i = this.loopTicks(p_453573_);
      KeyframeTrackSampler.Segment<T> segment = this.getSegmentAt(i);
      if (i <= segment.fromTicks) {
         return segment.fromValue;
      }

      if (i >= segment.toTicks) {
         return segment.toValue;
      }

      float f = (float)(i - segment.fromTicks) / (segment.toTicks - segment.fromTicks);
      float f1 = segment.easing.apply(f);
      return this.lerp.apply(f1, segment.fromValue, segment.toValue);
   }

   private KeyframeTrackSampler.Segment<T> getSegmentAt(long p_456058_) {
      for (KeyframeTrackSampler.Segment<T> segment : this.segments) {
         if (p_456058_ < segment.toTicks) {
            return segment;
         }
      }

      return this.segments.getLast();
   }

   private long loopTicks(long p_457899_) {
      return this.periodTicks.isPresent() ? Math.floorMod(p_457899_, this.periodTicks.get()) : p_457899_;
   }

   record Segment<T>(EasingType easing, T fromValue, int fromTicks, T toValue, int toTicks) {
      public Segment(KeyframeTrack<T> p_451749_, Keyframe<T> p_453699_, int p_451057_, Keyframe<T> p_455106_, int p_453773_) {
         this(p_451749_.easingType(), p_453699_.value(), p_451057_, p_455106_.value(), p_453773_);
      }
   }
}
