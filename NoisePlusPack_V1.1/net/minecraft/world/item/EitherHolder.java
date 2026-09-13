package net.minecraft.world.item;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;

public record EitherHolder<T>(Either<Holder<T>, ResourceKey<T>> contents) {
   public EitherHolder(Holder<T> p_344455_) {
      this(Either.left(p_344455_));
   }

   public EitherHolder(ResourceKey<T> p_343550_) {
      this(Either.right(p_343550_));
   }

   public static <T> Codec<EitherHolder<T>> codec(ResourceKey<Registry<T>> p_343702_, Codec<Holder<T>> p_342656_) {
      return Codec.either(
            p_342656_,
            ResourceKey.codec(p_343702_).comapFlatMap(p_343571_ -> DataResult.error(() -> "Cannot parse as key without registry"), Function.identity())
         )
         .xmap(EitherHolder::new, EitherHolder::contents);
   }

   public static <T> StreamCodec<RegistryFriendlyByteBuf, EitherHolder<T>> streamCodec(
      ResourceKey<Registry<T>> p_343828_, StreamCodec<RegistryFriendlyByteBuf, Holder<T>> p_345169_
   ) {
      return StreamCodec.composite(ByteBufCodecs.either(p_345169_, ResourceKey.streamCodec(p_343828_)), EitherHolder::contents, EitherHolder::new);
   }

   public Optional<T> unwrap(Registry<T> p_342155_) {
      return (Optional<T>)this.contents.map(p_390807_ -> Optional.of(p_390807_.value()), p_342155_::getOptional);
   }

   public Optional<Holder<T>> unwrap(HolderLookup.Provider p_344960_) {
      return (Optional<Holder<T>>)this.contents.map(Optional::of, p_390806_ -> p_344960_.get(p_390806_).map(p_390808_ -> (Holder)p_390808_));
   }

   public Optional<ResourceKey<T>> key() {
      return (Optional<ResourceKey<T>>)this.contents.map(Holder::unwrapKey, Optional::of);
   }
}
