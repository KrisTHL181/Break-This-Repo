package net.minecraft.core.component;

import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMaps;
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
      .xmap(p_330428_ -> {
         if (p_330428_.isEmpty()) {
            return EMPTY;
         }

         Reference2ObjectMap<DataComponentType<?>, Optional<?>> reference2objectmap = new Reference2ObjectArrayMap(p_330428_.size());

         for (Entry<DataComponentPatch.PatchKey, ?> entry : p_330428_.entrySet()) {
            DataComponentPatch.PatchKey datacomponentpatch$patchkey = entry.getKey();
            if (datacomponentpatch$patchkey.removed()) {
               reference2objectmap.put(datacomponentpatch$patchkey.type(), Optional.empty());
            } else {
               reference2objectmap.put(datacomponentpatch$patchkey.type(), Optional.of(entry.getValue()));
            }
         }

         return new DataComponentPatch(reference2objectmap);
      }, p_335950_ -> {
         Reference2ObjectMap<DataComponentPatch.PatchKey, Object> reference2objectmap = new Reference2ObjectArrayMap(p_335950_.map.size());
         ObjectIterator var2 = Reference2ObjectMaps.fastIterable(p_335950_.map).iterator();

         while (var2.hasNext()) {
            Entry<DataComponentType<?>, Optional<?>> entry = (Entry<DataComponentType<?>, Optional<?>>)var2.next();
            DataComponentType<?> datacomponenttype = entry.getKey();
            if (!datacomponenttype.isTransient()) {
               Optional<?> optional = entry.getValue();
               if (optional.isPresent()) {
                  reference2objectmap.put(new DataComponentPatch.PatchKey(datacomponenttype, false), optional.get());
               } else {
                  reference2objectmap.put(new DataComponentPatch.PatchKey(datacomponenttype, true), Unit.INSTANCE);
               }
            }
         }

         return reference2objectmap;
      });
   public static final StreamCodec<RegistryFriendlyByteBuf, DataComponentPatch> STREAM_CODEC = createStreamCodec(new DataComponentPatch.CodecGetter() {
      @Override
      public <T> StreamCodec<RegistryFriendlyByteBuf, T> apply(DataComponentType<T> p_397940_) {
         return p_397940_.streamCodec().cast();
      }
   });
   public static final StreamCodec<RegistryFriendlyByteBuf, DataComponentPatch> DELIMITED_STREAM_CODEC = createStreamCodec(
      new DataComponentPatch.CodecGetter() {
         @Override
         public <T> StreamCodec<RegistryFriendlyByteBuf, T> apply(DataComponentType<T> p_392185_) {
            StreamCodec<RegistryFriendlyByteBuf, T> streamcodec = p_392185_.streamCodec().cast();
            return streamcodec.apply(ByteBufCodecs.registryFriendlyLengthPrefixed(Integer.MAX_VALUE));
         }
      }
   );
   private static final String REMOVED_PREFIX = "!";
   final Reference2ObjectMap<DataComponentType<?>, Optional<?>> map;

   private static StreamCodec<RegistryFriendlyByteBuf, DataComponentPatch> createStreamCodec(final DataComponentPatch.CodecGetter p_395003_) {
      return new StreamCodec<RegistryFriendlyByteBuf, DataComponentPatch>() {
         public DataComponentPatch decode(RegistryFriendlyByteBuf p_394008_) {
            int i = p_394008_.readVarInt();
            int j = p_394008_.readVarInt();
            if (i == 0 && j == 0) {
               return DataComponentPatch.EMPTY;
            }

            int k = i + j;
            Reference2ObjectMap<DataComponentType<?>, Optional<?>> reference2objectmap = new Reference2ObjectArrayMap(Math.min(k, 65536));

            for (int l = 0; l < i; l++) {
               DataComponentType<?> datacomponenttype = DataComponentType.STREAM_CODEC.decode(p_394008_);
               Object object = p_395003_.apply(datacomponenttype).decode(p_394008_);
               reference2objectmap.put(datacomponenttype, Optional.of(object));
            }

            for (int i1 = 0; i1 < j; i1++) {
               DataComponentType<?> datacomponenttype1 = DataComponentType.STREAM_CODEC.decode(p_394008_);
               reference2objectmap.put(datacomponenttype1, Optional.empty());
            }

            return new DataComponentPatch(reference2objectmap);
         }

         public void encode(RegistryFriendlyByteBuf p_393727_, DataComponentPatch p_394312_) {
            if (p_394312_.isEmpty()) {
               p_393727_.writeVarInt(0);
               p_393727_.writeVarInt(0);
            } else {
               int i = 0;
               int j = 0;
               ObjectIterator var5 = Reference2ObjectMaps.fastIterable(p_394312_.map).iterator();

               while (var5.hasNext()) {
                  it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<DataComponentType<?>, Optional<?>> entry = (it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<DataComponentType<?>, Optional<?>>)var5.next();
                  if (((Optional)entry.getValue()).isPresent()) {
                     i++;
                  } else {
                     j++;
                  }
               }

               p_393727_.writeVarInt(i);
               p_393727_.writeVarInt(j);
               var5 = Reference2ObjectMaps.fastIterable(p_394312_.map).iterator();

               while (var5.hasNext()) {
                  it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<DataComponentType<?>, Optional<?>> entry1 = (it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<DataComponentType<?>, Optional<?>>)var5.next();
                  Optional<?> optional = (Optional<?>)entry1.getValue();
                  if (optional.isPresent()) {
                     DataComponentType<?> datacomponenttype = (DataComponentType<?>)entry1.getKey();
                     DataComponentType.STREAM_CODEC.encode(p_393727_, datacomponenttype);
                     this.encodeComponent(p_393727_, datacomponenttype, optional.get());
                  }
               }

               var5 = Reference2ObjectMaps.fastIterable(p_394312_.map).iterator();

               while (var5.hasNext()) {
                  it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<DataComponentType<?>, Optional<?>> entry2 = (it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<DataComponentType<?>, Optional<?>>)var5.next();
                  if (((Optional)entry2.getValue()).isEmpty()) {
                     DataComponentType<?> datacomponenttype1 = (DataComponentType<?>)entry2.getKey();
                     DataComponentType.STREAM_CODEC.encode(p_393727_, datacomponenttype1);
                  }
               }
            }
         }

         private <T> void encodeComponent(RegistryFriendlyByteBuf p_391817_, DataComponentType<T> p_392724_, Object p_397568_) {
            p_395003_.apply(p_392724_).encode(p_391817_, (T)p_397568_);
         }
      };
   }

   DataComponentPatch(Reference2ObjectMap<DataComponentType<?>, Optional<?>> p_329783_) {
      this.map = p_329783_;
   }

   public static DataComponentPatch.Builder builder() {
      return new DataComponentPatch.Builder();
   }

   public <T> @Nullable Optional<? extends T> get(DataComponentType<? extends T> p_330742_) {
      return (Optional<? extends T>)this.map.get(p_330742_);
   }

   public Set<Entry<DataComponentType<?>, Optional<?>>> entrySet() {
      return this.map.entrySet();
   }

   public int size() {
      return this.map.size();
   }

   public DataComponentPatch forget(Predicate<DataComponentType<?>> p_333810_) {
      if (this.isEmpty()) {
         return EMPTY;
      }

      Reference2ObjectMap<DataComponentType<?>, Optional<?>> reference2objectmap = new Reference2ObjectArrayMap(this.map);
      reference2objectmap.keySet().removeIf(p_333810_);
      return reference2objectmap.isEmpty() ? EMPTY : new DataComponentPatch(reference2objectmap);
   }

   public boolean isEmpty() {
      return this.map.isEmpty();
   }

   public DataComponentPatch.SplitResult split() {
      if (this.isEmpty()) {
         return DataComponentPatch.SplitResult.EMPTY;
      }

      DataComponentMap.Builder datacomponentmap$builder = DataComponentMap.builder();
      Set<DataComponentType<?>> set = Sets.newIdentityHashSet();
      this.map.forEach((p_336136_, p_328765_) -> {
         if (p_328765_.isPresent()) {
            datacomponentmap$builder.setUnchecked(p_336136_, p_328765_.get());
         } else {
            set.add(p_336136_);
         }
      });
      return new DataComponentPatch.SplitResult(datacomponentmap$builder.build(), set);
   }

   @Override
   public boolean equals(Object p_334345_) {
      return this == p_334345_ ? true : p_334345_ instanceof DataComponentPatch datacomponentpatch && this.map.equals(datacomponentpatch.map);
   }

   @Override
   public int hashCode() {
      return this.map.hashCode();
   }

   @Override
   public String toString() {
      return toString(this.map);
   }

   static String toString(Reference2ObjectMap<DataComponentType<?>, Optional<?>> p_335670_) {
      StringBuilder stringbuilder = new StringBuilder();
      stringbuilder.append('{');
      boolean flag = true;
      ObjectIterator var3 = Reference2ObjectMaps.fastIterable(p_335670_).iterator();

      while (var3.hasNext()) {
         Entry<DataComponentType<?>, Optional<?>> entry = (Entry<DataComponentType<?>, Optional<?>>)var3.next();
         if (flag) {
            flag = false;
         } else {
            stringbuilder.append(", ");
         }

         Optional<?> optional = entry.getValue();
         if (optional.isPresent()) {
            stringbuilder.append(entry.getKey());
            stringbuilder.append("=>");
            stringbuilder.append(optional.get());
         } else {
            stringbuilder.append("!");
            stringbuilder.append(entry.getKey());
         }
      }

      stringbuilder.append('}');
      return stringbuilder.toString();
   }

   public static class Builder {
      private final Reference2ObjectMap<DataComponentType<?>, Optional<?>> map = new Reference2ObjectArrayMap();

      Builder() {
      }

      public <T> DataComponentPatch.Builder set(DataComponentType<T> p_329935_, T p_331578_) {
         this.map.put(p_329935_, Optional.of(p_331578_));
         return this;
      }

      public <T> DataComponentPatch.Builder remove(DataComponentType<T> p_329018_) {
         this.map.put(p_329018_, Optional.empty());
         return this;
      }

      public <T> DataComponentPatch.Builder set(TypedDataComponent<T> p_331095_) {
         return this.set(p_331095_.type(), p_331095_.value());
      }

      public DataComponentPatch build() {
         return this.map.isEmpty() ? DataComponentPatch.EMPTY : new DataComponentPatch(this.map);
      }
   }

   @FunctionalInterface
   interface CodecGetter {
      <T> StreamCodec<? super RegistryFriendlyByteBuf, T> apply(DataComponentType<T> var1);
   }

   record PatchKey(DataComponentType<?> type, boolean removed) {
      public static final Codec<DataComponentPatch.PatchKey> CODEC = Codec.STRING
         .flatXmap(
            p_330758_ -> {
               boolean flag = p_330758_.startsWith("!");
               if (flag) {
                  p_330758_ = p_330758_.substring("!".length());
               }

               Identifier identifier = Identifier.tryParse(p_330758_);
               DataComponentType<?> datacomponenttype = BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(identifier);
               if (datacomponenttype == null) {
                  return DataResult.error(() -> "No component with type: '" + identifier + "'");
               } else {
                  return datacomponenttype.isTransient()
                     ? DataResult.error(() -> "'" + identifier + "' is not a persistent component")
                     : DataResult.success(new DataComponentPatch.PatchKey(datacomponenttype, flag));
               }
            },
            p_448582_ -> {
               DataComponentType<?> datacomponenttype = p_448582_.type();
               Identifier identifier = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(datacomponenttype);
               return identifier == null
                  ? DataResult.error(() -> "Unregistered component: " + datacomponenttype)
                  : DataResult.success(p_448582_.removed() ? "!" + identifier : identifier.toString());
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
