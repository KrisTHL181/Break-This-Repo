package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TrialSpawnerConfigFix extends NamedEntityWriteReadFix {
   public TrialSpawnerConfigFix(Schema p_334159_) {
      super(p_334159_, true, "Trial Spawner config tag fixer", References.BLOCK_ENTITY, "minecraft:trial_spawner");
   }

   private static <T> Dynamic<T> moveToConfigTag(Dynamic<T> p_330678_) {
      List<String> list = List.of(
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
      Map<Dynamic<T>, Dynamic<T>> map = new HashMap<>(list.size());

      for (String s : list) {
         Optional<Dynamic<T>> optional = p_330678_.get(s).get().result();
         if (optional.isPresent()) {
            map.put(p_330678_.createString(s), optional.get());
            p_330678_ = p_330678_.remove(s);
         }
      }

      return map.isEmpty() ? p_330678_ : p_330678_.set("normal_config", p_330678_.createMap(map));
   }

   @Override
   protected <T> Dynamic<T> fix(Dynamic<T> p_334514_) {
      return moveToConfigTag(p_334514_);
   }
}
