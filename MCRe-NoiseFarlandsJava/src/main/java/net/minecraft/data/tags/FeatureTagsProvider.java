package net.minecraft.data.tags;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.worldgen.features.VegetationFeatures;
import net.minecraft.tags.FeatureTags;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public class FeatureTagsProvider extends TagsProvider<ConfiguredFeature<?, ?>> {
    public FeatureTagsProvider(final PackOutput output, final CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, Registries.CONFIGURED_FEATURE, lookupProvider);
    }

    @Override
    protected void addTags(final HolderLookup.Provider registries) {
        this.tag(FeatureTags.CAN_SPAWN_FROM_BONE_MEAL)
            .add(
                VegetationFeatures.FLOWER_DEFAULT,
                VegetationFeatures.FLOWER_FLOWER_FOREST,
                VegetationFeatures.FLOWER_SWAMP,
                VegetationFeatures.FLOWER_PLAIN,
                VegetationFeatures.FLOWER_MEADOW,
                VegetationFeatures.FLOWER_CHERRY,
                VegetationFeatures.WILDFLOWER,
                VegetationFeatures.FLOWER_PALE_GARDEN
            );
    }
}