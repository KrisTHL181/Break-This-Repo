package net.minecraft.util.random;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.function.Function;
import net.minecraft.SharedConstants;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Util;
import org.slf4j.Logger;

public record Weighted<T>(T value, int weight) {
   private static final Logger LOGGER = LogUtils.getLogger();

   public Weighted {
      if (weight < 0) {
         throw (IllegalArgumentException)Util.pauseInIde(new IllegalArgumentException("Weight should be >= 0"));
      }

      if (weight == 0 && SharedConstants.IS_RUNNING_IN_IDE) {
         LOGGER.warn("Found 0 weight, make sure this is intentional!");
      }
   }

   public static <E> Codec<Weighted<E>> codec(Codec<E> p_396145_) {
      return codec(p_396145_.fieldOf("data"));
   }

   public static <E> Codec<Weighted<E>> codec(MapCodec<E> p_391567_) {
      return RecordCodecBuilder.create(
         p_391586_ -> p_391586_.group(p_391567_.forGetter(Weighted::value), ExtraCodecs.NON_NEGATIVE_INT.fieldOf("weight").forGetter(Weighted::weight))
            .apply(p_391586_, Weighted::new)
      );
   }

   public static <B extends ByteBuf, T> StreamCodec<B, Weighted<T>> streamCodec(StreamCodec<B, T> p_423325_) {
      return StreamCodec.composite(p_423325_, Weighted::value, ByteBufCodecs.VAR_INT, Weighted::weight, Weighted::new);
   }

   public <U> Weighted<U> map(Function<T, U> p_394740_) {
      return new Weighted<>(p_394740_.apply(this.value()), this.weight);
   }
}
