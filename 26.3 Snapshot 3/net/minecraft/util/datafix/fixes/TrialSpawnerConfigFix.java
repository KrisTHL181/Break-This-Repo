package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TrialSpawnerConfigFix extends NamedEntityWriteReadFix {
   public TrialSpawnerConfigFix(final Schema outputSchema) {
      super(outputSchema, true, "Trial Spawner config tag fixer", References.BLOCK_ENTITY, "minecraft:trial_spawner");
   }

   private static <T> Dynamic<T> moveToConfigTag(Dynamic<T> input) {
      List<String> keysToMove = List.of(
         "spawn_range",
         "total_mobs",
         "simultaneous_mobs",
         "total_mobs_added_per_player",
         "simultaneous_mobs_added_per_player",
         "ticks_between_spawn",
         "spawn_potentials",
         "loot_tables_to_eject",
         "items_to_drop_when_ominous"
      );
      Map<Dynamic<T>, Dynamic<T>> map = new HashMap<>(keysToMove.size());

      for (String key : keysToMove) {
         Optional<Dynamic<T>> maybeValueForKey = input.get(key).get().result();
         if (maybeValueForKey.isPresent()) {
            map.put(input.createString(key), maybeValueForKey.get());
            input = input.remove(key);
         }
      }

      return map.isEmpty() ? input : input.set("normal_config", input.createMap(map));
   }

   @Override
   protected <T> Dynamic<T> fix(final Dynamic<T> input) {
      return moveToConfigTag(input);
   }
}
