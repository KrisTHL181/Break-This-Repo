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

   public static <E> Codec<Weighted<E>> codec(final Codec<E> elementCodec) {
      return codec(elementCodec.fieldOf("data"));
   }

   public static <E> Codec<Weighted<E>> codec(final MapCodec<E> elementCodec) {
      return RecordCodecBuilder.create(
         i -> i.group(elementCodec.forGetter(Weighted::value), ExtraCodecs.NON_NEGATIVE_INT.fieldOf("weight").forGetter(Weighted::weight))
            .apply(i, Weighted::new)
      );
   }

   public static <B extends ByteBuf, T> StreamCodec<B, Weighted<T>> streamCodec(final StreamCodec<B, T> valueCodec) {
      return StreamCodec.composite(valueCodec, Weighted::value, ByteBufCodecs.VAR_INT, Weighted::weight, Weighted::new);
   }

   public <U> Weighted<U> map(final Function<T, U> function) {
      return new Weighted<>(function.apply(this.value()), this.weight);
   }
}
