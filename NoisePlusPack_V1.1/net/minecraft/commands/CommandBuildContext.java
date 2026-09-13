package net.minecraft.commands;

import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlagSet;

public interface CommandBuildContext extends HolderLookup.Provider {
   static CommandBuildContext simple(final HolderLookup.Provider p_255702_, final FeatureFlagSet p_255968_) {
      return new CommandBuildContext() {
         @Override
         public Stream<ResourceKey<? extends Registry<?>>> listRegistryKeys() {
            return p_255702_.listRegistryKeys();
         }

         @Override
         public <T> Optional<HolderLookup.RegistryLookup<T>> lookup(ResourceKey<? extends Registry<? extends T>> p_330252_) {
            return p_255702_.lookup(p_330252_).map(p_331454_ -> p_331454_.filterFeatures(p_255968_));
         }

         @Override
         public FeatureFlagSet enabledFeatures() {
            return p_255968_;
         }
      };
   }

   FeatureFlagSet enabledFeatures();
}
