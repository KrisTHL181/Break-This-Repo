package net.minecraft.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JavaOps;
import com.mojang.serialization.Lifecycle;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

public class PlaceholderLookupProvider implements HolderGetter.Provider {
   final HolderLookup.Provider context;
   final PlaceholderLookupProvider.UniversalLookup lookup = new PlaceholderLookupProvider.UniversalLookup();
   final Map<ResourceKey<Object>, Holder.Reference<Object>> holders = new HashMap<>();
   final Map<TagKey<Object>, HolderSet.Named<Object>> holderSets = new HashMap<>();

   public PlaceholderLookupProvider(HolderLookup.Provider p_396424_) {
      this.context = p_396424_;
   }

   @Override
   public <T> Optional<? extends HolderGetter<T>> lookup(ResourceKey<? extends Registry<? extends T>> p_392313_) {
      return Optional.of(this.lookup.castAsLookup());
   }

   public <V> RegistryOps<V> createSerializationContext(DynamicOps<V> p_396800_) {
      return RegistryOps.create(
         p_396800_,
         new RegistryOps.RegistryInfoLookup() {
            @Override
            public <T> Optional<RegistryOps.RegistryInfo<T>> lookup(ResourceKey<? extends Registry<? extends T>> p_391738_) {
               return PlaceholderLookupProvider.this.context
                  .lookup(p_391738_)
                  .map(RegistryOps.RegistryInfo::fromRegistryLookup)
                  .or(
                     () -> Optional.of(
                        new RegistryOps.RegistryInfo<>(
                           PlaceholderLookupProvider.this.lookup.castAsOwner(), PlaceholderLookupProvider.this.lookup.castAsLookup(), Lifecycle.experimental()
                        )
                     )
                  );
            }
         }
      );
   }

   public RegistryContextSwapper createSwapper() {
      return new RegistryContextSwapper() {
         @Override
         public <T> DataResult<T> swapTo(Codec<T> p_394376_, T p_394056_, HolderLookup.Provider p_397457_) {
            return p_394376_.encodeStart(PlaceholderLookupProvider.this.createSerializationContext(JavaOps.INSTANCE), p_394056_)
               .flatMap(p_395467_ -> p_394376_.parse(p_397457_.createSerializationContext(JavaOps.INSTANCE), p_395467_));
         }
      };
   }

   public boolean hasRegisteredPlaceholders() {
      return !this.holders.isEmpty() || !this.holderSets.isEmpty();
   }

   class UniversalLookup implements HolderGetter<Object>, HolderOwner<Object> {
      @Override
      public Optional<Holder.Reference<Object>> get(ResourceKey<Object> p_394615_) {
         return Optional.of(this.getOrCreate(p_394615_));
      }

      @Override
      public Holder.Reference<Object> getOrThrow(ResourceKey<Object> p_392521_) {
         return this.getOrCreate(p_392521_);
      }

      private Holder.Reference<Object> getOrCreate(ResourceKey<Object> p_392885_) {
         return PlaceholderLookupProvider.this.holders
            .computeIfAbsent(p_392885_, p_393201_ -> Holder.Reference.createStandAlone(this, (ResourceKey<Object>)p_393201_));
      }

      @Override
      public Optional<HolderSet.Named<Object>> get(TagKey<Object> p_395754_) {
         return Optional.of(this.getOrCreate(p_395754_));
      }

      @Override
      public HolderSet.Named<Object> getOrThrow(TagKey<Object> p_396896_) {
         return this.getOrCreate(p_396896_);
      }

      private HolderSet.Named<Object> getOrCreate(TagKey<Object> p_397236_) {
         return PlaceholderLookupProvider.this.holderSets.computeIfAbsent(p_397236_, p_391551_ -> HolderSet.emptyNamed(this, (TagKey<Object>)p_391551_));
      }

      public <T> HolderGetter<T> castAsLookup() {
         return this;
      }

      public <T> HolderOwner<T> castAsOwner() {
         return this;
      }
   }
}
