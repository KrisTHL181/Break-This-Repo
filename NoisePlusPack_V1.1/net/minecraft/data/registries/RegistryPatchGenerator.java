package net.minecraft.data.registries;

import com.mojang.datafixers.DataFixUtils;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.Cloner;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class RegistryPatchGenerator {
   public static CompletableFuture<RegistrySetBuilder.PatchedRegistries> createLookup(
      CompletableFuture<HolderLookup.Provider> p_310881_, RegistrySetBuilder p_310262_
   ) {
      return p_310881_.thenApply(
         p_309945_ -> {
            RegistryAccess.Frozen registryaccess$frozen = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
            Cloner.Factory cloner$factory = new Cloner.Factory();
            RegistryDataLoader.WORLDGEN_REGISTRIES.forEach(p_313050_ -> p_313050_.runWithArguments(cloner$factory::addCodec));
            RegistrySetBuilder.PatchedRegistries registrysetbuilder$patchedregistries = p_310262_.buildPatch(registryaccess$frozen, p_309945_, cloner$factory);
            HolderLookup.Provider holderlookup$provider = registrysetbuilder$patchedregistries.full();
            Optional<? extends HolderLookup.RegistryLookup<Biome>> optional = holderlookup$provider.lookup(Registries.BIOME);
            Optional<? extends HolderLookup.RegistryLookup<PlacedFeature>> optional1 = holderlookup$provider.lookup(Registries.PLACED_FEATURE);
            if (optional.isPresent() || optional1.isPresent()) {
               VanillaRegistries.validateThatAllBiomeFeaturesHaveBiomeFilter(
                  (HolderGetter<PlacedFeature>)DataFixUtils.orElseGet(optional1, () -> p_309945_.lookupOrThrow(Registries.PLACED_FEATURE)),
                  (HolderLookup<Biome>)DataFixUtils.orElseGet(optional, () -> p_309945_.lookupOrThrow(Registries.BIOME))
               );
            }

            return registrysetbuilder$patchedregistries;
         }
      );
   }
}
