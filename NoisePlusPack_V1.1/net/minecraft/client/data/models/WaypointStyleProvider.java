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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WaypointStyleProvider implements DataProvider {
   private final PackOutput.PathProvider pathProvider;

   public WaypointStyleProvider(PackOutput p_409425_) {
      this.pathProvider = p_409425_.createPathProvider(PackOutput.Target.RESOURCE_PACK, "waypoint_style");
   }

   private static void bootstrap(BiConsumer<ResourceKey<WaypointStyleAsset>, WaypointStyle> p_410218_) {
      p_410218_.accept(
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
      p_410218_.accept(
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
   public CompletableFuture<?> run(CachedOutput p_406451_) {
      Map<ResourceKey<WaypointStyleAsset>, WaypointStyle> map = new HashMap<>();
      bootstrap((p_406579_, p_409291_) -> {
         if (map.putIfAbsent(p_406579_, p_409291_) != null) {
            throw new IllegalStateException("Tried to register waypoint style twice for id: " + p_406579_);
         }
      });
      return DataProvider.saveAll(p_406451_, WaypointStyle.CODEC, this.pathProvider::json, map);
   }

   @Override
   public String getName() {
      return "Waypoint Style Definitions";
   }
}
