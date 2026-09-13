package net.minecraft.server.network;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record Filterable<T>(T raw, Optional<T> filtered) {
   public static <T> Codec<Filterable<T>> codec(final Codec<T> valueCodec) {
      Codec<Filterable<T>> fullCodec = RecordCodecBuilder.create(
         i -> i.group(valueCodec.fieldOf("raw").forGetter(Filterable::raw), valueCodec.optionalFieldOf("filtered").forGetter(Filterable::filtered))
            .apply(i, Filterable::new)
      );
      Codec<Filterable<T>> simpleCodec = valueCodec.xmap(Filterable::passThrough, Filterable::raw);
      return Codec.withAlternative(fullCodec, simpleCodec);
   }

   public static <B extends ByteBuf, T> StreamCodec<B, Filterable<T>> streamCodec(final StreamCodec<B, T> valueCodec) {
      return StreamCodec.composite(valueCodec, Filterable::raw, valueCodec.apply(ByteBufCodecs::optional), Filterable::filtered, Filterable::new);
   }

   public static <T> Filterable<T> passThrough(final T value) {
      return new Filterable<>(value, Optional.empty());
   }

   public static Filterable<String> from(final FilteredText text) {
      return new Filterable<>(text.raw(), text.isFiltered() ? Optional.of(text.filteredOrEmpty()) : Optional.empty());
   }

   public T get(final boolean filterEnabled) {
      return filterEnabled ? this.filtered.orElse(this.raw) : this.raw;
   }

   public <U> Filterable<U> map(final Function<T, U> function) {
      return new Filterable<>(function.apply(this.raw), this.filtered.map(function));
   }

   public <U> Optional<Filterable<U>> resolve(final Function<T, Optional<U>> function) {
      Optional<U> newRaw = function.apply(this.raw);
      if (newRaw.isEmpty()) {
         return Optional.empty();
      } else if (this.filtered.isPresent()) {
         Optional<U> newFiltered = function.apply(this.filtered.get());
         return newFiltered.isEmpty() ? Optional.empty() : Optional.of(new Filterable<>(newRaw.get(), newFiltered));
      } else {
         return Optional.of(new Filterable<>(newRaw.get(), Optional.empty()));
      }
   }
}
