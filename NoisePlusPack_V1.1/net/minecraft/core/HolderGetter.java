package net.minecraft.core;

import java.util.Optional;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;

public interface HolderGetter<T> {
   Optional<Holder.Reference<T>> get(ResourceKey<T> var1);

   default Holder.Reference<T> getOrThrow(ResourceKey<T> p_255990_) {
      return this.get(p_255990_).orElseThrow(() -> new IllegalStateException("Missing element " + p_255990_));
   }

   Optional<HolderSet.Named<T>> get(TagKey<T> var1);

   default HolderSet.Named<T> getOrThrow(TagKey<T> p_256125_) {
      return this.get(p_256125_).orElseThrow(() -> new IllegalStateException("Missing tag " + p_256125_));
   }

   default Optional<Holder<T>> getRandomElementOf(TagKey<T> p_430702_, RandomSource p_422877_) {
      return this.get(p_430702_).flatMap(p_421113_ -> p_421113_.getRandomElement(p_422877_));
   }

   interface Provider {
      <T> Optional<? extends HolderGetter<T>> lookup(ResourceKey<? extends Registry<? extends T>> var1);

      default <T> HolderGetter<T> lookupOrThrow(ResourceKey<? extends Registry<? extends T>> p_255881_) {
         return (HolderGetter<T>)this.lookup(p_255881_).orElseThrow(() -> new IllegalStateException("Registry " + p_255881_.identifier() + " not found"));
      }

      default <T> Optional<Holder.Reference<T>> get(ResourceKey<T> p_331697_) {
         return this.lookup(p_331697_.registryKey()).flatMap(p_325667_ -> p_325667_.get(p_331697_));
      }

      default <T> Holder.Reference<T> getOrThrow(ResourceKey<T> p_393982_) {
         return this.lookup(p_393982_.registryKey())
            .flatMap(p_389657_ -> p_389657_.get(p_393982_))
            .orElseThrow(() -> new IllegalStateException("Missing element " + p_393982_));
      }
   }
}
