package net.minecraft.world.level.biome;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.FeatureTags;
import net.minecraft.util.Util;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.slf4j.Logger;

public class BiomeGenerationSettings {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final BiomeGenerationSettings EMPTY = new BiomeGenerationSettings(HolderSet.empty(), List.of());
   public static final MapCodec<BiomeGenerationSettings> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            WorldCarver.LIST_CODEC.promotePartial(Util.prefix("Carver: ", LOGGER::error)).fieldOf("carvers").forGetter(b -> b.carvers),
            PlacedFeature.LIST_OF_LISTS_CODEC.promotePartial(Util.prefix("Features: ", LOGGER::error)).fieldOf("features").forGetter(b -> b.features)
         )
         .apply(i, BiomeGenerationSettings::new)
   );
   private final HolderSet<WorldCarver> carvers;
   private final List<HolderSet<PlacedFeature>> features;
   private final Supplier<List<Feature>> boneMealFeatures;
   private final Supplier<Set<PlacedFeature>> featureSet;

   private BiomeGenerationSettings(final HolderSet<WorldCarver> carvers, final List<HolderSet<PlacedFeature>> features) {
      this.carvers = carvers;
      this.features = features;
      this.boneMealFeatures = Suppliers.memoize(
         () -> features.stream()
            .flatMap(HolderSet::stream)
            .flatMap(feature -> ((PlacedFeature)feature.value()).getFeatures())
            .filter(feature -> feature.is(FeatureTags.CAN_SPAWN_FROM_BONE_MEAL))
            .map(Holder::value)
            .collect(ImmutableList.toImmutableList())
      );
      this.featureSet = Suppliers.memoize(() -> features.stream().flatMap(HolderSet::stream).map(Holder::value).collect(Collectors.toSet()));
   }

   public Iterable<Holder<WorldCarver>> getCarvers() {
      return this.carvers;
   }

   public List<Feature> getBoneMealFeatures() {
      return this.boneMealFeatures.get();
   }

   public List<HolderSet<PlacedFeature>> features() {
      return this.features;
   }

   public boolean hasFeature(final PlacedFeature feature) {
      return this.featureSet.get().contains(feature);
   }

   public static class Builder extends BiomeGenerationSettings.PlainBuilder {
      private final HolderGetter<PlacedFeature> placedFeatures;
      private final HolderGetter<WorldCarver> worldCarvers;

      public Builder(final HolderGetter<PlacedFeature> placedFeatures, final HolderGetter<WorldCarver> worldCarvers) {
         this.placedFeatures = placedFeatures;
         this.worldCarvers = worldCarvers;
      }

      public BiomeGenerationSettings.Builder addFeature(final GenerationStep.Decoration step, final ResourceKey<PlacedFeature> feature) {
         this.addFeature(step.ordinal(), this.placedFeatures.getOrThrow(feature));
         return this;
      }

      public BiomeGenerationSettings.Builder addCarver(final ResourceKey<WorldCarver> carver) {
         this.addCarver(this.worldCarvers.getOrThrow(carver));
         return this;
      }
   }

   public static class PlainBuilder {
      private final List<Holder<WorldCarver>> carvers = new ArrayList<>();
      private final List<List<Holder<PlacedFeature>>> features = new ArrayList<>();

      public BiomeGenerationSettings.PlainBuilder addFeature(final GenerationStep.Decoration step, final Holder<PlacedFeature> feature) {
         return this.addFeature(step.ordinal(), feature);
      }

      public BiomeGenerationSettings.PlainBuilder addFeature(final int index, final Holder<PlacedFeature> feature) {
         this.addFeatureStepsUpTo(index);
         this.features.get(index).add(feature);
         return this;
      }

      public BiomeGenerationSettings.PlainBuilder addCarver(final Holder<WorldCarver> carver) {
         this.carvers.add(carver);
         return this;
      }

      private void addFeatureStepsUpTo(final int index) {
         while (this.features.size() <= index) {
            this.features.add(Lists.newArrayList());
         }
      }

      public BiomeGenerationSettings build() {
         return new BiomeGenerationSettings(
            HolderSet.direct(this.carvers), this.features.stream().map(HolderSet::direct).collect(ImmutableList.toImmutableList())
         );
      }
   }
}
