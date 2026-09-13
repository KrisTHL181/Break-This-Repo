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

   private static <E> Codec<List<Holder<E>>> homogenousList(final Codec<Holder<E>> elementCodec, final boolean alwaysUseList) {
      Codec<List<Holder<E>>> listCodec = elementCodec.listOf().validate(ExtraCodecs.ensureHomogenous(Holder::kind));
      return alwaysUseList ? listCodec : ExtraCodecs.compactListCodec(elementCodec, listCodec);
   }

   public static <E> Codec<HolderSet<E>> create(
      final ResourceKey<? extends Registry<E>> registryKey, final Codec<Holder<E>> elementCodec, final boolean alwaysUseList
   ) {
      return new HolderSetCodec<>(registryKey, elementCodec, alwaysUseList);
   }

   private HolderSetCodec(final ResourceKey<? extends Registry<E>> registryKey, final Codec<Holder<E>> elementCodec, final boolean alwaysUseList) {
      this.registryKey = registryKey;
      this.elementCodec = elementCodec;
      this.homogenousListCodec = homogenousList(elementCodec, alwaysUseList);
      this.registryAwareCodec = Codec.either(TagKey.hashedCodec(registryKey), this.homogenousListCodec);
   }

   public <T> DataResult<Pair<HolderSet<E>, T>> decode(final DynamicOps<T> ops, final T input) {
      if (ops instanceof RegistryOps<T> registryOps) {
         Optional<HolderGetter<E>> registryOptional = registryOps.getter(this.registryKey);
         if (registryOptional.isPresent()) {
            HolderGetter<E> registry = registryOptional.get();
            return this.registryAwareCodec
               .decode(ops, input)
               .flatMap(
                  p -> {
                     DataResult<HolderSet<E>> result = (DataResult<HolderSet<E>>)((Either)p.getFirst())
                        .map(tag -> lookupTag(registry, tag), values -> DataResult.success(HolderSet.direct(values)));
                     return result.map(holders -> Pair.of(holders, p.getSecond()));
                  }
               );
         }
      }

      return this.decodeWithoutRegistry(ops, input);
   }

   private static <E> DataResult<HolderSet<E>> lookupTag(final HolderGetter<E> registry, final TagKey<E> key) {
      return registry.get(key)
         .<DataResult<HolderSet<E>>>map(DataResult::success)
         .orElseGet(() -> DataResult.error(() -> "Missing tag: '" + key.location() + "' in '" + key.registry().identifier() + "'"));
   }

   public <T> DataResult<T> encode(final HolderSet<E> input, final DynamicOps<T> ops, final T prefix) {
      if (ops instanceof RegistryOps<T> registryOps) {
         Optional<? extends HolderOwner<E>> maybeOwner = registryOps.getter(this.registryKey);
         if (maybeOwner.isPresent()) {
            if (!input.canSerializeIn((HolderOwner<E>)maybeOwner.get())) {
               return DataResult.error(() -> "HolderSet " + input + " is not valid in current registry set");
            }

            return this.registryAwareCodec.encode(input.unwrap().mapRight(List::copyOf), ops, prefix);
         }
      }

      return this.encodeWithoutRegistry(input, ops, prefix);
   }

   private <T> DataResult<Pair<HolderSet<E>, T>> decodeWithoutRegistry(final DynamicOps<T> ops, final T input) {
      return this.elementCodec.listOf().decode(ops, input).flatMap(p -> {
         List<Holder.Direct<E>> directHolders = new ArrayList<>();

         for (Holder<E> holder : (List)p.getFirst()) {
            if (!(holder instanceof Holder.Direct<E> direct)) {
               return DataResult.error(() -> "Can't decode element " + holder + " without registry");
            }

            directHolders.add(direct);
         }

         return DataResult.success(new Pair(HolderSet.direct(directHolders), p.getSecond()));
      });
   }

   private <T> DataResult<T> encodeWithoutRegistry(final HolderSet<E> input, final DynamicOps<T> ops, final T prefix) {
      return this.homogenousListCodec.encode(input.stream().toList(), ops, prefix);
   }
}
