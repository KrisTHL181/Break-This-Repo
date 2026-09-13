package net.minecraft.client.resources.metadata.animation;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.util.ExtraCodecs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record AnimationFrame(int index, Optional<Integer> time) {
   public static final Codec<AnimationFrame> FULL_CODEC = RecordCodecBuilder.create(
      p_377783_ -> p_377783_.group(
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("index").forGetter(AnimationFrame::index),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("time").forGetter(AnimationFrame::time)
         )
         .apply(p_377783_, AnimationFrame::new)
   );
   public static final Codec<AnimationFrame> CODEC = Codec.either(ExtraCodecs.NON_NEGATIVE_INT, FULL_CODEC)
      .xmap(
         p_375401_ -> (AnimationFrame)p_375401_.map(AnimationFrame::new, p_378196_ -> p_378196_),
         p_378215_ -> p_378215_.time.isPresent() ? Either.right(p_378215_) : Either.left(p_378215_.index)
      );

   public AnimationFrame(int p_119004_) {
      this(p_119004_, Optional.empty());
   }

   public int timeOr(int p_378767_) {
      return this.time.orElse(p_378767_);
   }
}
