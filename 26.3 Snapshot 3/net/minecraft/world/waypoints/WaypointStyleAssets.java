package net.minecraft.world.waypoints;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public interface WaypointStyleAssets {
   ResourceKey<? extends Registry<WaypointStyleAsset>> ROOT_ID = ResourceKey.createRegistryKey(Identifier.withDefaultNamespace("waypoint_style_asset"));
   ResourceKey<WaypointStyleAsset> DEFAULT = createId("default");
   ResourceKey<WaypointStyleAsset> BOWTIE = createId("bowtie");

   static ResourceKey<WaypointStyleAsset> createId(final String name) {
      return ResourceKey.create(ROOT_ID, Identifier.withDefaultNamespace(name));
   }
}
