package net.minecraft.server.network;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record Filterable<T>(T raw, Optional<T> filtered) {
   public static <T> Codec<Filterable<T>> codec(Codec<T> p_327990_) {
      Codec<Filterable<T>> codec = RecordCodecBuilder.create(
         p_328042_ -> p_328042_.group(
               p_327990_.fieldOf("raw").forGetter(Filterable::raw), p_327990_.optionalFieldOf("filtered").forGetter(Filterable::filtered)
            )
            .apply(p_328042_, Filterable::new)
      );
      Codec<Filterable<T>> codec1 = p_327990_.xmap(Filterable::passThrough, Filterable::raw);
      return Codec.withAlternative(codec, codec1);
   }

   public static <B extends ByteBuf, T> StreamCodec<B, Filterable<T>> streamCodec(StreamCodec<B, T> p_328361_) {
      return StreamCodec.composite(p_328361_, Filterable::raw, p_328361_.apply(ByteBufCodecs::optional), Filterable::filtered, Filterable::new);
   }

   public static <T> Filterable<T> passThrough(T p_333360_) {
      return new Filterable<>(p_333360_, Optional.empty());
   }

   public static Filterable<String> from(FilteredText p_332002_) {
      return new Filterable<>(p_332002_.raw(), p_332002_.isFiltered() ? Optional.of(p_332002_.filteredOrEmpty()) : Optional.empty());
   }

   public T get(boolean p_335502_) {
      return p_335502_ ? this.filtered.orElse(this.raw) : this.raw;
   }

   public <U> Filterable<U> map(Function<T, U> p_328140_) {
      return new Filterable<>(p_328140_.apply(this.raw), this.filtered.map(p_328140_));
   }

   public <U> Optional<Filterable<U>> resolve(Function<T, Optional<U>> p_335887_) {
      Optional<U> optional = p_335887_.apply(this.raw);
      if (optional.isEmpty()) {
         return Optional.empty();
      } else if (this.filtered.isPresent()) {
         Optional<U> optional1 = p_335887_.apply(this.filtered.get());
         return optional1.isEmpty() ? Optional.empty() : Optional.of(new Filterable<>(optional.get(), optional1));
      } else {
         return Optional.of(new Filterable<>(optional.get(), Optional.empty()));
      }
   }
}
