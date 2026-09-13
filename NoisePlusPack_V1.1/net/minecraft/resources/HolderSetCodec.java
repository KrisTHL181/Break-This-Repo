package net.minecraft.resources;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;

public class HolderSetCodec<E> implements Codec<HolderSet<E>> {
   private final ResourceKey<? extends Registry<E>> registryKey;
   private final Codec<Holder<E>> elementCodec;
   private final Codec<List<Holder<E>>> homogenousListCodec;
   private final Codec<Either<TagKey<E>, List<Holder<E>>>> registryAwareCodec;

   private static <E> Codec<List<Holder<E>>> homogenousList(Codec<Holder<E>> p_206668_, boolean p_206669_) {
      Codec<List<Holder<E>>> codec = p_206668_.listOf().validate(ExtraCodecs.ensureHomogenous(Holder::kind));
      return p_206669_ ? codec : ExtraCodecs.compactListCodec(p_206668_, codec);
   }

   public static <E> Codec<HolderSet<E>> create(ResourceKey<? extends Registry<E>> p_206686_, Codec<Holder<E>> p_206687_, boolean p_206688_) {
      return new HolderSetCodec<>(p_206686_, p_206687_, p_206688_);
   }

   private HolderSetCodec(ResourceKey<? extends Registry<E>> p_206660_, Codec<Holder<E>> p_206661_, boolean p_206662_) {
      this.registryKey = p_206660_;
      this.elementCodec = p_206661_;
      this.homogenousListCodec = homogenousList(p_206661_, p_206662_);
      this.registryAwareCodec = Codec.either(TagKey.hashedCodec(p_206660_), this.homogenousListCodec);
   }

   public <T> DataResult<Pair<HolderSet<E>, T>> decode(DynamicOps<T> p_206696_, T p_206697_) {
      if (p_206696_ instanceof RegistryOps<T> registryops) {
         Optional<HolderGetter<E>> optional = registryops.getter(this.registryKey);
         if (optional.isPresent()) {
            HolderGetter<E> holdergetter = optional.get();
            return this.registryAwareCodec
               .decode(p_206696_, p_206697_)
               .flatMap(
                  p_326147_ -> {
                     DataResult<HolderSet<E>> dataresult = (DataResult<HolderSet<E>>)((Either)p_326147_.getFirst())
                        .map(p_326145_ -> lookupTag(holdergetter, p_326145_), p_326140_ -> DataResult.success(HolderSet.direct(p_326140_)));
                     return dataresult.map(p_326149_ -> Pair.of(p_326149_, p_326147_.getSecond()));
                  }
               );
         }
      }

      return this.decodeWithoutRegistry(p_206696_, p_206697_);
   }

   private static <E> DataResult<HolderSet<E>> lookupTag(HolderGetter<E> p_331398_, TagKey<E> p_328227_) {
      return p_331398_.get(p_328227_)
         .<DataResult<HolderSet<E>>>map(DataResult::success)
         .orElseGet(() -> DataResult.error(() -> "Missing tag: '" + p_328227_.location() + "' in '" + p_328227_.registry().identifier() + "'"));
   }

   public <T> DataResult<T> encode(HolderSet<E> p_206674_, DynamicOps<T> p_206675_, T p_206676_) {
      if (p_206675_ instanceof RegistryOps<T> registryops) {
         Optional<HolderOwner<E>> optional = registryops.owner(this.registryKey);
         if (optional.isPresent()) {
            if (!p_206674_.canSerializeIn(optional.get())) {
               return DataResult.error(() -> "HolderSet " + p_206674_ + " is not valid in current registry set");
            }

            return this.registryAwareCodec.encode(p_206674_.unwrap().mapRight(List::copyOf), p_206675_, p_206676_);
         }
      }

      return this.encodeWithoutRegistry(p_206674_, p_206675_, p_206676_);
   }

   private <T> DataResult<Pair<HolderSet<E>, T>> decodeWithoutRegistry(DynamicOps<T> p_206671_, T p_206672_) {
      return this.elementCodec.listOf().decode(p_206671_, p_206672_).flatMap(p_206666_ -> {
         List<Holder.Direct<E>> list = new ArrayList<>();

         for (Holder<E> holder : (List)p_206666_.getFirst()) {
            if (!(holder instanceof Holder.Direct<E> direct)) {
               return DataResult.error(() -> "Can't decode element " + holder + " without registry");
            }

            list.add(direct);
         }

         return DataResult.success(new Pair(HolderSet.direct(list), p_206666_.getSecond()));
      });
   }

   private <T> DataResult<T> encodeWithoutRegistry(HolderSet<E> p_206690_, DynamicOps<T> p_206691_, T p_206692_) {
      return this.homogenousListCodec.encode(p_206690_.stream().toList(), p_206691_, p_206692_);
   }
}
