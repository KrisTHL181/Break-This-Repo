package net.minecraft.core.component;

import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMaps;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Predicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import org.jspecify.annotations.Nullable;

public final class DataComponentPatch {
   public static final DataComponentPatch EMPTY = new DataComponentPatch(Reference2ObjectMaps.emptyMap());
   public static final Codec<DataComponentPatch> CODEC = Codec.dispatchedMap(DataComponentPatch.PatchKey.CODEC, DataComponentPatch.PatchKey::valueCodec)
      .xmap(data -> {
         if (data.isEmpty()) {
            return EMPTY;
         }

         Reference2ObjectMap<DataComponentType<?>, Optional<?>> map = new Reference2ObjectArrayMap(data.size());

         for (Entry<DataComponentPatch.PatchKey, ?> entry : data.entrySet()) {
            DataComponentPatch.PatchKey key = entry.getKey();
            if (key.removed()) {
               map.put(key.type(), Optional.empty());
            } else {
               map.put(key.type(), Optional.of(entry.getValue()));
            }
         }

         return new DataComponentPatch(map);
      }, patch -> {
         Reference2ObjectMap<DataComponentPatch.PatchKey, Object> map = new Reference2ObjectArrayMap(patch.map.size());
         Iterator i$ = Reference2ObjectMaps.fastIterable(patch.map).iterator();

         while (i$.hasNext()) {
            Entry<DataComponentType<?>, Optional<?>> entry = (Entry<DataComponentType<?>, Optional<?>>)i$.next();
            DataComponentType<?> type = entry.getKey();
            if (!type.isTransient()) {
               Optional<?> value = entry.getValue();
               if (value.isPresent()) {
                  map.put(new DataComponentPatch.PatchKey(type, false), value.get());
               } else {
                  map.put(new DataComponentPatch.PatchKey(type, true), Unit.INSTANCE);
               }
            }
         }

         return map;
      });
   public static final StreamCodec<RegistryFriendlyByteBuf, DataComponentPatch> STREAM_CODEC = createStreamCodec(new DataComponentPatch.CodecGetter() {
      @Override
      public <T> StreamCodec<RegistryFriendlyByteBuf, T> apply(final DataComponentType<T> type) {
         return type.streamCodec().cast();
      }
   });
   public static final StreamCodec<RegistryFriendlyByteBuf, DataComponentPatch> DELIMITED_STREAM_CODEC = createStreamCodec(
      new DataComponentPatch.CodecGetter() {
         @Override
         public <T> StreamCodec<RegistryFriendlyByteBuf, T> apply(final DataComponentType<T> type) {
            StreamCodec<RegistryFriendlyByteBuf, T> original = type.streamCodec().cast();
            return original.apply(ByteBufCodecs.registryFriendlyLengthPrefixed(Integer.MAX_VALUE));
         }
      }
   );
   private static final String REMOVED_PREFIX = "!";
   final Reference2ObjectMap<DataComponentType<?>, Optional<?>> map;

   private static StreamCodec<RegistryFriendlyByteBuf, DataComponentPatch> createStreamCodec(final DataComponentPatch.CodecGetter codecGetter) {
      return new StreamCodec<RegistryFriendlyByteBuf, DataComponentPatch>() {
         public DataComponentPatch decode(final RegistryFriendlyByteBuf input) {
            int positiveCount = input.readVarInt();
            int negativeCount = input.readVarInt();
            if (positiveCount == 0 && negativeCount == 0) {
               return DataComponentPatch.EMPTY;
            }

            int expectedSize = positiveCount + negativeCount;
            Reference2ObjectMap<DataComponentType<?>, Optional<?>> map = new Reference2ObjectArrayMap(Math.min(expectedSize, 65536));

            for (int i = 0; i < positiveCount; i++) {
               DataComponentType<?> type = DataComponentType.STREAM_CODEC.decode(input);
               Object value = codecGetter.apply(type).decode(input);
               map.put(type, Optional.of(value));
            }

            for (int i = 0; i < negativeCount; i++) {
               DataComponentType<?> type = DataComponentType.STREAM_CODEC.decode(input);
               map.put(type, Optional.empty());
            }

            return new DataComponentPatch(map);
         }

         public void encode(final RegistryFriendlyByteBuf output, final DataComponentPatch patch) {
            if (patch.isEmpty()) {
               output.writeVarInt(0);
               output.writeVarInt(0);
            } else {
               int positiveCount = 0;
               int negativeCount = 0;
               ObjectIterator var5 = Reference2ObjectMaps.fastIterable(patch.map).iterator();

               while (var5.hasNext()) {
                  it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<DataComponentType<?>, Optional<?>> entry = (it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<DataComponentType<?>, Optional<?>>)var5.next();
                  if (((Optional)entry.getValue()).isPresent()) {
                     positiveCount++;
                  } else {
                     negativeCount++;
                  }
               }

               output.writeVarInt(positiveCount);
               output.writeVarInt(negativeCount);
               var5 = Reference2ObjectMaps.fastIterable(patch.map).iterator();

               while (var5.hasNext()) {
                  it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<DataComponentType<?>, Optional<?>> entry = (it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<DataComponentType<?>, Optional<?>>)var5.next();
                  Optional<?> value = (Optional<?>)entry.getValue();
                  if (value.isPresent()) {
                     DataComponentType<?> type = (DataComponentType<?>)entry.getKey();
                     DataComponentType.STREAM_CODEC.encode(output, type);
                     this.encodeComponent(output, type, value.get());
                  }
               }

               var5 = Reference2ObjectMaps.fastIterable(patch.map).iterator();

               while (var5.hasNext()) {
                  it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<DataComponentType<?>, Optional<?>> entry = (it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<DataComponentType<?>, Optional<?>>)var5.next();
                  if (((Optional)entry.getValue()).isEmpty()) {
                     DataComponentType<?> type = (DataComponentType<?>)entry.getKey();
                     DataComponentType.STREAM_CODEC.encode(output, type);
                  }
               }
            }
         }

         private <T> void encodeComponent(final RegistryFriendlyByteBuf output, final DataComponentType<T> type, final Object value) {
            codecGetter.apply(type).encode(output, (T)value);
         }
      };
   }

   DataComponentPatch(final Reference2ObjectMap<DataComponentType<?>, Optional<?>> map) {
      this.map = map;
   }

   public static DataComponentPatch.Builder builder() {
      return new DataComponentPatch.Builder();
   }

   public <T> @Nullable T get(final DataComponentGetter prototype, final DataComponentType<? extends T> type) {
      return getFromPatchAndPrototype(this.map, prototype, type);
   }

   static <T> @Nullable T getFromPatchAndPrototype(
      final Reference2ObjectMap<DataComponentType<?>, Optional<?>> patch, final DataComponentGetter prototype, final DataComponentType<? extends T> type
   ) {
      Optional<? extends T> value = (Optional<? extends T>)patch.get(type);
      return (T)(value != null ? value.orElse(null) : prototype.get(type));
   }

   public Set<Entry<DataComponentType<?>, Optional<?>>> entrySet() {
      return this.map.entrySet();
   }

   public int size() {
      return this.map.size();
   }

   public DataComponentPatch forget(final Predicate<DataComponentType<?>> test) {
      if (this.isEmpty()) {
         return EMPTY;
      }

      Reference2ObjectMap<DataComponentType<?>, Optional<?>> newMap = new Reference2ObjectArrayMap(this.map);
      newMap.keySet().removeIf(test);
      return newMap.isEmpty() ? EMPTY : new DataComponentPatch(newMap);
   }

   public boolean isEmpty() {
      return this.map.isEmpty();
   }

   public DataComponentPatch.SplitResult split() {
      if (this.isEmpty()) {
         return DataComponentPatch.SplitResult.EMPTY;
      }

      DataComponentMap.Builder added = DataComponentMap.builder();
      Set<DataComponentType<?>> removed = Sets.newIdentityHashSet();
      this.map.forEach((type, optionalValue) -> {
         if (optionalValue.isPresent()) {
            added.setUnchecked(type, optionalValue.get());
         } else {
            removed.add(type);
         }
      });
      return new DataComponentPatch.SplitResult(added.build(), removed);
   }

   @Override
   public boolean equals(final Object obj) {
      return this == obj ? true : obj instanceof DataComponentPatch patch && this.map.equals(patch.map);
   }

   @Override
   public int hashCode() {
      return this.map.hashCode();
   }

   @Override
   public String toString() {
      return toString(this.map);
   }

   static String toString(final Reference2ObjectMap<DataComponentType<?>, Optional<?>> map) {
      StringBuilder builder = new StringBuilder();
      builder.append('{');
      boolean first = true;
      ObjectIterator var3 = Reference2ObjectMaps.fastIterable(map).iterator();

      while (var3.hasNext()) {
         Entry<DataComponentType<?>, Optional<?>> entry = (Entry<DataComponentType<?>, Optional<?>>)var3.next();
         if (first) {
            first = false;
         } else {
            builder.append(", ");
         }

         Optional<?> value = entry.getValue();
         if (value.isPresent()) {
            builder.append(entry.getKey());
            builder.append("=>");
            builder.append(value.get());
         } else {
            builder.append("!");
            builder.append(entry.getKey());
         }
      }

      builder.append('}');
      return builder.toString();
   }

   public static class Builder {
      private final Reference2ObjectMap<DataComponentType<?>, Optional<?>> map = new Reference2ObjectArrayMap();

      private Builder() {
      }

      public <T> DataComponentPatch.Builder set(final DataComponentType<T> type, final T value) {
         this.map.put(type, Optional.of(value));
         return this;
      }

      public <T> DataComponentPatch.Builder remove(final DataComponentType<T> type) {
         this.map.put(type, Optional.empty());
         return this;
      }

      public <T> DataComponentPatch.Builder set(final TypedDataComponent<T> component) {
         return this.set(component.type(), component.value());
      }

      public <T> DataComponentPatch.Builder set(final Iterable<TypedDataComponent<?>> components) {
         for (TypedDataComponent<?> component : components) {
            this.set(component);
         }

         return this;
      }

      public DataComponentPatch build() {
         return this.map.isEmpty() ? DataComponentPatch.EMPTY : new DataComponentPatch(this.map);
      }
   }

   @FunctionalInterface
   private interface CodecGetter {
      <T> StreamCodec<? super RegistryFriendlyByteBuf, T> apply(DataComponentType<T> type);
   }

   private record PatchKey(DataComponentType<?> type, boolean removed) {
      public static final Codec<DataComponentPatch.PatchKey> CODEC = Codec.STRING
         .flatXmap(
            string -> {
               boolean removed = string.startsWith("!");
               if (removed) {
                  string = string.substring("!".length());
               }

               Identifier id = Identifier.tryParse(string);
               DataComponentType<?> type = BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(id);
               if (type == null) {
                  return DataResult.error(() -> "No component with type: '" + id + "'");
               } else {
                  return type.isTransient()
                     ? DataResult.error(() -> "'" + id + "' is not a persistent component")
                     : DataResult.success(new DataComponentPatch.PatchKey(type, removed));
               }
            },
            key -> {
               DataComponentType<?> type = key.type();
               Identifier id = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);
               return id == null ? DataResult.error(() -> "Unregistered component: " + type) : DataResult.success(key.removed() ? "!" + id : id.toString());
            }
         );

      public Codec<?> valueCodec() {
         return this.removed ? Codec.EMPTY.codec() : this.type.codecOrThrow();
      }
   }

   public record SplitResult(DataComponentMap added, Set<DataComponentType<?>> removed) {
      public static final DataComponentPatch.SplitResult EMPTY = new DataComponentPatch.SplitResult(DataComponentMap.EMPTY, Set.of());
   }
}
