package net.minecraft.data.worldgen.features;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.Feature;

public class FeatureUtils {
   public static void bootstrap(final BootstrapContext<Feature> context) {
      AquaticFeatures.bootstrap(context);
      CaveFeatures.bootstrap(context);
      EndFeatures.bootstrap(context);
      MiscOverworldFeatures.bootstrap(context);
      NetherFeatures.bootstrap(context);
      OreFeatures.bootstrap(context);
      PileFeatures.bootstrap(context);
      TreeFeatures.bootstrap(context);
      VegetationFeatures.bootstrap(context);
   }

   public static ResourceKey<Feature> createKey(final String name) {
      return ResourceKey.create(Registries.FEATURE, Identifier.withDefaultNamespace(name));
   }
}
