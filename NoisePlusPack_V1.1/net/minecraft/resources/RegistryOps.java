package net.minecraft.resources;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.Registry;
import net.minecraft.util.ExtraCodecs;

public class RegistryOps<T> extends DelegatingOps<T> {
   private final RegistryOps.RegistryInfoLookup lookupProvider;

   public static <T> RegistryOps<T> create(DynamicOps<T> p_256342_, HolderLookup.Provider p_255950_) {
      return create(p_256342_, new RegistryOps.HolderLookupAdapter(p_255950_));
   }

   public static <T> RegistryOps<T> create(DynamicOps<T> p_256278_, RegistryOps.RegistryInfoLookup p_256479_) {
      return new RegistryOps<>(p_256278_, p_256479_);
   }

   public static <T> Dynamic<T> injectRegistryContext(Dynamic<T> p_331489_, HolderLookup.Provider p_331408_) {
      return new Dynamic(p_331408_.createSerializationContext(p_331489_.getOps()), p_331489_.getValue());
   }

   private RegistryOps(DynamicOps<T> p_256313_, RegistryOps.RegistryInfoLookup p_255799_) {
      super(p_256313_);
      this.lookupProvider = p_255799_;
   }

   public <U> RegistryOps<U> withParent(DynamicOps<U> p_332969_) {
      return (RegistryOps<U>)(p_332969_ == this.delegate ? this : new RegistryOps((DynamicOps<T>)p_332969_, this.lookupProvider));
   }

   public <E> Optional<HolderOwner<E>> owner(ResourceKey<? extends Registry<? extends E>> p_255757_) {
      return this.lookupProvider.lookup(p_255757_).map(RegistryOps.RegistryInfo::owner);
   }

   public <E> Optional<HolderGetter<E>> getter(ResourceKey<? extends Registry<? extends E>> p_256031_) {
      return this.lookupProvider.lookup(p_256031_).map(RegistryOps.RegistryInfo::getter);
   }

   @Override
   public boolean equals(Object p_332753_) {
      if (this == p_332753_) {
         return true;
      } else if (p_332753_ != null && this.getClass() == p_332753_.getClass()) {
         RegistryOps<?> registryops = (RegistryOps<?>)p_332753_;
         return this.delegate.equals(registryops.delegate) && this.lookupProvider.equals(registryops.lookupProvider);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return this.delegate.hashCode() * 31 + this.lookupProvider.hashCode();
   }

   public static <E, O> RecordCodecBuilder<O, HolderGetter<E>> retrieveGetter(ResourceKey<? extends Registry<? extends E>> p_206833_) {
      return ExtraCodecs.retrieveContext(
            p_274811_ -> p_274811_ instanceof RegistryOps<?> registryops
               ? registryops.lookupProvider
                  .lookup(p_206833_)
                  .map(p_255527_ -> DataResult.success(p_255527_.getter(), p_255527_.elementsLifecycle()))
                  .orElseGet(() -> DataResult.error(() -> "Unknown registry: " + p_206833_))
               : DataResult.error(() -> "Not a registry ops")
         )
         .forGetter(p_255526_ -> null);
   }

   public static <E, O> RecordCodecBuilder<O, Holder.Reference<E>> retrieveElement(ResourceKey<E> p_256347_) {
      ResourceKey<? extends Registry<E>> resourcekey = ResourceKey.createRegistryKey(p_256347_.registry());
      return ExtraCodecs.retrieveContext(
            p_274808_ -> p_274808_ instanceof RegistryOps<?> registryops
               ? registryops.lookupProvider
                  .lookup(resourcekey)
                  .flatMap(p_255518_ -> p_255518_.getter().get(p_256347_))
                  .<DataResult<E>>map(DataResult::success)
                  .orElseGet(() -> DataResult.error(() -> "Can't find value: " + p_256347_))
               : DataResult.error(() -> "Not a registry ops")
         )
         .forGetter(p_255524_ -> null);
   }

   static final class HolderLookupAdapter implements RegistryOps.RegistryInfoLookup {
      private final HolderLookup.Provider lookupProvider;
      private final Map<ResourceKey<? extends Registry<?>>, Optional<? extends RegistryOps.RegistryInfo<?>>> lookups = new ConcurrentHashMap<>();

      public HolderLookupAdapter(HolderLookup.Provider p_335468_) {
         this.lookupProvider = p_335468_;
      }

      @Override
      public <E> Optional<RegistryOps.RegistryInfo<E>> lookup(ResourceKey<? extends Registry<? extends E>> p_330389_) {
         return (Optional<RegistryOps.RegistryInfo<E>>)this.lookups.computeIfAbsent(p_330389_, this::createLookup);
      }

      private Optional<RegistryOps.RegistryInfo<Object>> createLookup(ResourceKey<? extends Registry<?>> p_335602_) {
         return this.lookupProvider.lookup(p_335602_).map(RegistryOps.RegistryInfo::fromRegistryLookup);
      }

      @Override
      public boolean equals(Object p_330775_) {
         return this == p_330775_
            ? true
            : p_330775_ instanceof RegistryOps.HolderLookupAdapter registryops$holderlookupadapter
               && this.lookupProvider.equals(registryops$holderlookupadapter.lookupProvider);
      }

      @Override
      public int hashCode() {
         return this.lookupProvider.hashCode();
      }
   }

   public record RegistryInfo<T>(HolderOwner<T> owner, HolderGetter<T> getter, Lifecycle elementsLifecycle) {
      public static <T> RegistryOps.RegistryInfo<T> fromRegistryLookup(HolderLookup.RegistryLookup<T> p_329148_) {
         return new RegistryOps.RegistryInfo<>(p_329148_, p_329148_, p_329148_.registryLifecycle());
      }
   }

   public interface RegistryInfoLookup {
      <T> Optional<RegistryOps.RegistryInfo<T>> lookup(ResourceKey<? extends Registry<? extends T>> var1);
   }
}
