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
import net.minecraft.core.component.DataComponentLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagLoader;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

public interface Registry<T> extends IdMap<T>, Keyable, HolderLookup.RegistryLookup<T> {
   @Override
   ResourceKey<? extends Registry<T>> key();

   default Codec<T> byNameCodec() {
      return this.referenceHolderWithLifecycle().flatComapMap(Holder.Reference::value, value -> this.safeCastToReference(this.wrapAsHolder((T)value)));
   }

   default Codec<Holder<T>> holderByNameCodec() {
      return this.referenceHolderWithLifecycle().flatComapMap(holder -> holder, this::safeCastToReference);
   }

   private Codec<Holder.Reference<T>> referenceHolderWithLifecycle() {
      Codec<Holder.Reference<T>> referenceCodec = Identifier.CODEC
         .comapFlatMap(
            name -> this.get(name)
               .<DataResult>map(DataResult::success)
               .orElseGet(() -> DataResult.error(() -> "Unknown registry key in " + this.key() + ": " + name)),
            holder -> holder.key().identifier()
         );
      return ExtraCodecs.overrideLifecycle(
         referenceCodec, e -> this.registrationInfo(e.key()).map(RegistrationInfo::lifecycle).orElse(Lifecycle.experimental())
      );
   }

   private DataResult<Holder.Reference<T>> safeCastToReference(final Holder<T> holder) {
      return holder instanceof Holder.Reference<T> reference
         ? DataResult.success(reference)
         : DataResult.error(() -> "Unregistered holder in " + this.key() + ": " + holder);
   }

   default <U> Stream<U> keys(final DynamicOps<U> ops) {
      return this.keySet().stream().map(k -> (U)ops.createString(k.toString()));
   }

   @Nullable Identifier getKey(T thing);

   Optional<ResourceKey<T>> getResourceKey(T thing);

   @Override
   int getId(@Nullable T thing);

   @Nullable T getValue(@Nullable ResourceKey<T> key);

   @Nullable T getValue(@Nullable Identifier key);

   Optional<RegistrationInfo> registrationInfo(ResourceKey<T> element);

   default Optional<T> getOptional(final @Nullable Identifier key) {
      return Optional.ofNullable(this.getValue(key));
   }

   default Optional<T> getOptional(final @Nullable ResourceKey<T> key) {
      return Optional.ofNullable(this.getValue(key));
   }

   Optional<Holder.Reference<T>> getAny();

   default T getValueOrThrow(final ResourceKey<T> key) {
      T value = this.getValue(key);
      if (value == null) {
         throw new IllegalStateException("Missing key in " + this.key() + ": " + key);
      } else {
         return value;
      }
   }

   Set<Identifier> keySet();

   Set<Entry<ResourceKey<T>, T>> entrySet();

   Set<ResourceKey<T>> registryKeySet();

   Optional<Holder.Reference<T>> getRandom(RandomSource random);

   default Stream<T> stream() {
      return StreamSupport.stream(this.spliterator(), false);
   }

   boolean containsKey(Identifier key);

   boolean containsKey(ResourceKey<T> key);

   static <T> T register(final Registry<? super T> registry, final String name, final T value) {
      return register(registry, Identifier.parse(name), value);
   }

   static <V, T extends V> T register(final Registry<V> registry, final Identifier location, final T value) {
      return register(registry, ResourceKey.create(registry.key(), location), value);
   }

   static <V, T extends V> T register(final Registry<V> registry, final ResourceKey<V> key, final T value) {
      ((WritableRegistry)registry).register(key, (V)value, RegistrationInfo.BUILT_IN);
      return value;
   }

   static <R, T extends R> Holder.Reference<T> registerForHolder(final Registry<R> registry, final ResourceKey<R> key, final T value) {
      return ((WritableRegistry)registry).register(key, (R)value, RegistrationInfo.BUILT_IN);
   }

   static <R, T extends R> Holder.Reference<T> registerForHolder(final Registry<R> registry, final Identifier location, final T value) {
      return registerForHolder(registry, ResourceKey.create(registry.key(), location), value);
   }

   Registry<T> freeze();

   Holder.Reference<T> createIntrusiveHolder(T value);

   Optional<Holder.Reference<T>> get(int id);

   Optional<Holder.Reference<T>> get(Identifier id);

   Holder<T> wrapAsHolder(T value);

   default Iterable<Holder<T>> getTagOrEmpty(final TagKey<T> id) {
      return (Iterable<Holder<T>>)DataFixUtils.orElse(this.get(id), List.of());
   }

   Stream<HolderSet.Named<T>> getTags();

   default IdMap<Holder<T>> asHolderIdMap() {
      return new IdMap<Holder<T>>() {
         public int getId(final Holder<T> thing) {
            return Registry.this.getId(thing.value());
         }

         public @Nullable Holder<T> byId(final int id) {
            return (Holder<T>)Registry.this.get(id).orElse(null);
         }

         @Override
         public int size() {
            return Registry.this.size();
         }

         @Override
         public Iterator<Holder<T>> iterator() {
            return Registry.this.listElements().map(e -> (Holder<T>)e).iterator();
         }
      };
   }

   Registry.PendingTags<T> prepareTagReload(TagLoader.LoadResult<T> tags);

   DataComponentLookup<T> componentLookup();

   interface PendingTags<T> {
      ResourceKey<? extends Registry<? extends T>> key();

      HolderLookup.RegistryLookup<T> lookup();

      void apply();

      int size();
   }
}
