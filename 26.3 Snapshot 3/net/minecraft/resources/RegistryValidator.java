package net.minecraft.resources;

import java.util.Map;
import net.minecraft.core.Registry;

@FunctionalInterface
public interface RegistryValidator<T> {
   RegistryValidator<?> NONE = (var0, var1) -> {};
   RegistryValidator<?> NON_EMPTY = (registry, loadingErrors) -> {
      if (registry.size() == 0) {
         loadingErrors.put(registry.key(), new IllegalStateException("Registry must be non-empty: " + registry.key().identifier()));
      }
   };

   static <T> RegistryValidator<T> none() {
      return (RegistryValidator<T>)NONE;
   }

   static <T> RegistryValidator<T> nonEmpty() {
      return (RegistryValidator<T>)NON_EMPTY;
   }

   void validate(Registry<T> registry, Map<ResourceKey<?>, Exception> loadingErrors);
}
