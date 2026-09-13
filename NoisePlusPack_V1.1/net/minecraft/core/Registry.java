package net.minecraft.core;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Keyable;
import com.mojang.serialization.Lifecycle;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagLoader;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

public interface Registry<T> extends Keyable, HolderLookup.RegistryLookup<T>, IdMap<T> {
   @Override
   ResourceKey<? extends Registry<T>> key();

   default Codec<T> byNameCodec() {
      return this.referenceHolderWithLifecycle().flatComapMap(Holder.Reference::value, p_325680_ -> this.safeCastToReference(this.wrapAsHolder((T)p_325680_)));
   }

   default Codec<Holder<T>> holderByNameCodec() {
      return this.referenceHolderWithLifecycle().flatComapMap(p_325683_ -> p_325683_, this::safeCastToReference);
   }

   private Codec<Holder.Reference<T>> referenceHolderWithLifecycle() {
      Codec<Holder.Reference<T>> codec = Identifier.CODEC
         .comapFlatMap(
            p_448565_ -> this.get(p_448565_)
               .<DataResult>map(DataResult::success)
               .orElseGet(() -> DataResult.error(() -> "Unknown registry key in " + this.key() + ": " + p_448565_)),
            p_448562_ -> p_448562_.key().identifier()
         );
      return ExtraCodecs.overrideLifecycle(
         codec, p_325682_ -> this.registrationInfo(p_325682_.key()).map(RegistrationInfo::lifecycle).orElse(Lifecycle.experimental())
      );
   }

   private DataResult<Holder.Reference<T>> safeCastToReference(Holder<T> p_329506_) {
      return p_329506_ instanceof Holder.Reference<T> reference
         ? DataResult.success(reference)
         : DataResult.error(() -> "Unregistered holder in " + this.key() + ": " + p_329506_);
   }

   default <U> Stream<U> keys(DynamicOps<U> p_123030_) {
      return this.keySet().stream().map(p_448564_ -> (U)p_123030_.createString(p_448564_.toString()));
   }

   @Nullable Identifier getKey(T var1);

   Optional<ResourceKey<T>> getResourceKey(T var1);

   @Override
   int getId(@Nullable T var1);

   @Nullable T getValue(@Nullable ResourceKey<T> var1);

   @Nullable T getValue(@Nullable Identifier var1);

   Optional<RegistrationInfo> registrationInfo(ResourceKey<T> var1);

   default Optional<T> getOptional(@Nullable Identifier p_452038_) {
      return Optional.ofNullable(this.getValue(p_452038_));
   }

   default Optional<T> getOptional(@Nullable ResourceKey<T> p_123010_) {
      return Optional.ofNullable(this.getValue(p_123010_));
   }

   Optional<Holder.Reference<T>> getAny();

   default T getValueOrThrow(ResourceKey<T> p_367641_) {
      T t = this.getValue(p_367641_);
      if (t == null) {
         throw new IllegalStateException("Missing key in " + this.key() + ": " + p_367641_);
      } else {
         return t;
      }
   }

   Set<Identifier> keySet();

   Set<Entry<ResourceKey<T>, T>> entrySet();

   Set<ResourceKey<T>> registryKeySet();

   Optional<Holder.Reference<T>> getRandom(RandomSource var1);

   default Stream<T> stream() {
      return StreamSupport.stream(this.spliterator(), false);
   }

   boolean containsKey(Identifier var1);

   boolean containsKey(ResourceKey<T> var1);

   static <T> T register(Registry<? super T> p_122962_, String p_122963_, T p_122964_) {
      return register(p_122962_, Identifier.parse(p_122963_), p_122964_);
   }

   static <V, T extends V> T register(Registry<V> p_122966_, Identifier p_452626_, T p_122968_) {
      return register(p_122966_, ResourceKey.create(p_122966_.key(), p_452626_), p_122968_);
   }

   static <V, T extends V> T register(Registry<V> p_194580_, ResourceKey<V> p_194581_, T p_194582_) {
      ((WritableRegistry)p_194580_).register(p_194581_, (V)p_194582_, RegistrationInfo.BUILT_IN);
      return p_194582_;
   }

   static <R, T extends R> Holder.Reference<T> registerForHolder(Registry<R> p_263347_, ResourceKey<R> p_263355_, T p_263428_) {
      return ((WritableRegistry)p_263347_).register(p_263355_, (R)p_263428_, RegistrationInfo.BUILT_IN);
   }

   static <R, T extends R> Holder.Reference<T> registerForHolder(Registry<R> p_263351_, Identifier p_458983_, T p_263423_) {
      return registerForHolder(p_263351_, ResourceKey.create(p_263351_.key(), p_458983_), p_263423_);
   }

   Registry<T> freeze();

   Holder.Reference<T> createIntrusiveHolder(T var1);

   Optional<Holder.Reference<T>> get(int var1);

   Optional<Holder.Reference<T>> get(Identifier var1);

   Holder<T> wrapAsHolder(T var1);

   default Iterable<Holder<T>> getTagOrEmpty(TagKey<T> p_206059_) {
      return (Iterable<Holder<T>>)DataFixUtils.orElse(this.get(p_206059_), List.of());
   }

   Stream<HolderSet.Named<T>> getTags();

   default IdMap<Holder<T>> asHolderIdMap() {
      return new IdMap<Holder<T>>() {
         public int getId(Holder<T> p_259992_) {
            return Registry.this.getId(p_259992_.value());
         }

         public @Nullable Holder<T> byId(int p_259972_) {
            return (Holder<T>)Registry.this.get(p_259972_).orElse(null);
         }

         @Override
         public int size() {
            return Registry.this.size();
         }

         @Override
         public Iterator<Holder<T>> iterator() {
            return Registry.this.listElements().map(p_260061_ -> (Holder<T>)p_260061_).iterator();
         }
      };
   }

   Registry.PendingTags<T> prepareTagReload(TagLoader.LoadResult<T> var1);

   interface PendingTags<T> {
      ResourceKey<? extends Registry<? extends T>> key();

      HolderLookup.RegistryLookup<T> lookup();

      void apply();

      int size();
   }
}
