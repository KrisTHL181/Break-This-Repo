package net.minecraft.client.data.models;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import net.minecraft.client.resources.WaypointStyle;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.waypoints.WaypointStyleAsset;
import net.minecraft.world.waypoints.WaypointStyleAssets;

public class WaypointStyleProvider implements DataProvider {
   private final PackOutput.PathProvider pathProvider;

   public WaypointStyleProvider(final PackOutput output) {
      this.pathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "waypoint_style");
   }

   private static void bootstrap(final BiConsumer<ResourceKey<WaypointStyleAsset>, WaypointStyle> consumer) {
      consumer.accept(
         WaypointStyleAssets.DEFAULT,
         new WaypointStyle(
            128,
            332,
            List.of(
               Identifier.withDefaultNamespace("default_0"),
               Identifier.withDefaultNamespace("default_1"),
               Identifier.withDefaultNamespace("default_2"),
               Identifier.withDefaultNamespace("default_3")
            )
         )
      );
      consumer.accept(
         WaypointStyleAssets.BOWTIE,
         new WaypointStyle(
            64,
            332,
            List.of(
               Identifier.withDefaultNamespace("bowtie"),
               Identifier.withDefaultNamespace("default_0"),
               Identifier.withDefaultNamespace("default_1"),
               Identifier.withDefaultNamespace("default_2"),
               Identifier.withDefaultNamespace("default_3")
            )
         )
      );
   }

   @Override
   public CompletableFuture<?> run(final CachedOutput cache) {
      Map<ResourceKey<WaypointStyleAsset>, WaypointStyle> waypointStyles = new HashMap<>();
      bootstrap((id, asset) -> {
         if (waypointStyles.putIfAbsent(id, asset) != null) {
            throw new IllegalStateException("Tried to register waypoint style twice for id: " + id);
         }
      });
      return DataProvider.saveAll(cache, WaypointStyle.CODEC, this.pathProvider::json, waypointStyles);
   }

   @Override
   public String getName() {
      return "Waypoint Style Definitions";
   }
}
