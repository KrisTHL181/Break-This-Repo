package net.minecraft.data.worldgen.features;

import java.util.List;
import java.util.Optional;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.feature.ChorusPlantFeature;
import net.minecraft.world.level.levelgen.feature.EndGatewayFeature;
import net.minecraft.world.level.levelgen.feature.EndIslandFeature;
import net.minecraft.world.level.levelgen.feature.EndPlatformFeature;
import net.minecraft.world.level.levelgen.feature.EndPodiumFeature;
import net.minecraft.world.level.levelgen.feature.EndSpikeFeature;
import net.minecraft.world.level.levelgen.feature.Feature;

public class EndFeatures {
   public static final ResourceKey<Feature> END_PLATFORM = FeatureUtils.createKey("end_platform");
   public static final ResourceKey<Feature> END_PODIUM_ACTIVE = FeatureUtils.createKey("end_podium_active");
   public static final ResourceKey<Feature> END_PODIUM_INACTIVE = FeatureUtils.createKey("end_podium_inactive");
   public static final ResourceKey<Feature> END_SPIKE = FeatureUtils.createKey("end_spike");
   public static final ResourceKey<Feature> END_GATEWAY_RETURN = FeatureUtils.createKey("end_gateway_return");
   public static final ResourceKey<Feature> END_GATEWAY_DELAYED = FeatureUtils.createKey("end_gateway_delayed");
   public static final ResourceKey<Feature> CHORUS_PLANT = FeatureUtils.createKey("chorus_plant");
   public static final ResourceKey<Feature> END_ISLAND = FeatureUtils.createKey("end_island");

   public static void bootstrap(final BootstrapContext<Feature> context) {
      context.register(END_PLATFORM, new EndPlatformFeature());
      context.register(END_PODIUM_ACTIVE, new EndPodiumFeature(true));
      context.register(END_PODIUM_INACTIVE, new EndPodiumFeature(false));
      context.register(END_SPIKE, new EndSpikeFeature(List.of(), false, Optional.empty()));
      context.register(END_GATEWAY_RETURN, EndGatewayFeature.knownExit(ServerLevel.END_SPAWN_POINT, true));
      context.register(END_GATEWAY_DELAYED, EndGatewayFeature.delayedExitSearch());
      context.register(CHORUS_PLANT, new ChorusPlantFeature());
      context.register(END_ISLAND, new EndIslandFeature());
   }
}
