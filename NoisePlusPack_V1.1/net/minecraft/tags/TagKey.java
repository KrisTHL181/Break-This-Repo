package net.minecraft.tags;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public record TagKey<T>(ResourceKey<? extends Registry<T>> registry, Identifier location) {
   private static final Interner<TagKey<?>> VALUES = Interners.newWeakInterner();

   @Deprecated
   public TagKey {
   }

   public static <T> Codec<TagKey<T>> codec(ResourceKey<? extends Registry<T>> p_203878_) {
      return Identifier.CODEC.xmap(p_449211_ -> create(p_203878_, p_449211_), TagKey::location);
   }

   public static <T> Codec<TagKey<T>> hashedCodec(ResourceKey<? extends Registry<T>> p_203887_) {
      return Codec.STRING
         .comapFlatMap(
            p_449218_ -> p_449218_.startsWith("#")
               ? Identifier.read(p_449218_.substring(1)).map(p_449216_ -> create(p_203887_, p_449216_))
               : DataResult.error(() -> "Not a tag id"),
            p_449212_ -> "#" + p_449212_.location
         );
   }

   public static <T> StreamCodec<ByteBuf, TagKey<T>> streamCodec(ResourceKey<? extends Registry<T>> p_368582_) {
      return Identifier.STREAM_CODEC.map(p_449214_ -> create(p_368582_, p_449214_), TagKey::location);
   }

   public static <T> TagKey<T> create(ResourceKey<? extends Registry<T>> p_203883_, Identifier p_451670_) {
      return (TagKey<T>)VALUES.intern(new TagKey<>(p_203883_, p_451670_));
   }

   public boolean isFor(ResourceKey<? extends Registry<?>> p_207646_) {
      return this.registry == p_207646_;
   }

   public <E> Optional<TagKey<E>> cast(ResourceKey<? extends Registry<E>> p_207648_) {
      return this.isFor(p_207648_) ? Optional.of((TagKey<E>)this) : Optional.empty();
   }

   @Override
   public String toString() {
      return "TagKey[" + this.registry.identifier() + " / " + this.location + "]";
   }
}
