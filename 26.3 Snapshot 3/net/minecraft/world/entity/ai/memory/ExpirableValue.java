package net.minecraft.world.entity.ai.memory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;

public record ExpirableValue<T>(T value, Optional<Long> timeToLive) {
   public static <T> ExpirableValue<T> of(final T value) {
      return new ExpirableValue<>(value, Optional.empty());
   }

   public static <T> ExpirableValue<T> of(final T value, final long ticksUntilExpiry) {
      return new ExpirableValue<>(value, Optional.of(ticksUntilExpiry));
   }

   @Override
   public String toString() {
      return this.value + (this.timeToLive.isPresent() ? " (ttl: " + this.timeToLive.get() + ")" : "");
   }

   public static <T> Codec<ExpirableValue<T>> codec(final Codec<T> valueCodec) {
      return RecordCodecBuilder.create(
         i -> i.group(
               valueCodec.fieldOf("value").forGetter(ExpirableValue::value), Codec.LONG.lenientOptionalFieldOf("ttl").forGetter(ExpirableValue::timeToLive)
            )
            .apply(i, ExpirableValue::new)
      );
   }
}
