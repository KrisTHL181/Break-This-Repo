package net.minecraft.client.resources.metadata.animation;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.util.ExtraCodecs;

public record AnimationFrame(int index, Optional<Integer> time) {
   public static final Codec<AnimationFrame> FULL_CODEC = RecordCodecBuilder.create(
      i -> i.group(
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("index").forGetter(AnimationFrame::index),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("time").forGetter(AnimationFrame::time)
         )
         .apply(i, AnimationFrame::new)
   );
   public static final Codec<AnimationFrame> CODEC = Codec.either(ExtraCodecs.NON_NEGATIVE_INT, FULL_CODEC)
      .xmap(either -> (AnimationFrame)either.map(AnimationFrame::new, v -> v), frame -> frame.time.isPresent() ? Either.right(frame) : Either.left(frame.index));

   public AnimationFrame(final int index) {
      this(index, Optional.empty());
   }

   public int timeOr(final int defaultFrameTime) {
      return this.time.orElse(defaultFrameTime);
   }
}
