package net.minecraft.core.registries;

import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;

public interface MultiRegistryBootstrap {
   Set<ResourceKey<? extends Registry<?>>> requestedRegistries();

   void run(MultiRegistryBootstrap.BootstrapGetter registries);

   interface BootstrapGetter {
      <T> BootstrapContext<T> get(ResourceKey<? extends Registry<T>> key);
   }
}
