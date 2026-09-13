package net.minecraft.world.clock;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public interface ClockTimeMarkers {
   ResourceKey<? extends Registry<ClockTimeMarker>> ROOT_ID = ResourceKey.createRegistryKey(Identifier.withDefaultNamespace("clock_time_marker"));
   ResourceKey<ClockTimeMarker> DAY = createKey("day");
   ResourceKey<ClockTimeMarker> NOON = createKey("noon");
   ResourceKey<ClockTimeMarker> NIGHT = createKey("night");
   ResourceKey<ClockTimeMarker> MIDNIGHT = createKey("midnight");
   ResourceKey<ClockTimeMarker> WAKE_UP_FROM_SLEEP = createKey("wake_up_from_sleep");
   ResourceKey<ClockTimeMarker> ROLL_VILLAGE_SIEGE = createKey("roll_village_siege");

   static ResourceKey<ClockTimeMarker> createKey(final String name) {
      return ResourceKey.create(ROOT_ID, Identifier.withDefaultNamespace(name));
   }
}
