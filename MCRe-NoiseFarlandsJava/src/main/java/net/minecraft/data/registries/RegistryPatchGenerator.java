package net.minecraft.data.registries;

import com.mojang.datafixers.DataFixUtils;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.Cloner;
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
        final CompletableFuture<HolderLookup.Provider> vanilla, final RegistrySetBuilder packBuilder
    ) {
        return vanilla.thenApply(
            parent -> {
                RegistryAccess.Frozen staticRegistries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
                Cloner.Factory cloner = new Cloner.Factory();
                RegistryDataLoader.WORLDGEN_REGISTRIES.forEach(registryData -> registryData.runWithArguments(cloner::addCodec));
                RegistrySetBuilder.PatchedRegistries newRegistries = packBuilder.buildPatch(staticRegistries, parent, cloner);
                HolderLookup.Provider fullPatchedRegistry = newRegistries.full();
                Optional<? extends HolderLookup.RegistryLookup<Biome>> biomes = fullPatchedRegistry.lookup(Registries.BIOME);
                Optional<? extends HolderLookup.RegistryLookup<PlacedFeature>> features = fullPatchedRegistry.lookup(Registries.PLACED_FEATURE);
                if (biomes.isPresent() || features.isPresent()) {
                    VanillaRegistries.validateThatAllBiomeFeaturesHaveBiomeFilter(
                        DataFixUtils.orElseGet(features, () -> parent.lookupOrThrow(Registries.PLACED_FEATURE)),
                        DataFixUtils.orElseGet(biomes, () -> parent.lookupOrThrow(Registries.BIOME))
                    );
                }

                return newRegistries;
            }
        );
    }
}