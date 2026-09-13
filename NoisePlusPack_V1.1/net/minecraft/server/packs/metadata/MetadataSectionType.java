package net.minecraft.server.packs.metadata;

import com.mojang.serialization.Codec;
import java.util.Optional;

public record MetadataSectionType<T>(String name, Codec<T> codec) {
   public MetadataSectionType.WithValue<T> withValue(T p_427425_) {
      return new MetadataSectionType.WithValue<>(this, p_427425_);
   }

   public record WithValue<T>(MetadataSectionType<T> type, T value) {
      public <U> Optional<U> unwrapToType(MetadataSectionType<U> p_422477_) {
         return p_422477_ == this.type ? Optional.of(this.value) : Optional.empty();
      }
   }
}
