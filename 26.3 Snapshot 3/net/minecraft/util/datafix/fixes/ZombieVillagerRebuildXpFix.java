package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import java.util.Optional;

public class ZombieVillagerRebuildXpFix extends NamedEntityFix {
   public ZombieVillagerRebuildXpFix(final Schema outputSchema, final boolean changesType) {
      super(outputSchema, changesType, "Zombie Villager XP rebuild", References.ENTITY, "minecraft:zombie_villager");
   }

   @Override
   protected Typed<?> fix(final Typed<?> entity) {
      return entity.update(DSL.remainderFinder(), remainder -> {
         Optional<Number> xp = remainder.get("Xp").asNumber().result();
         if (xp.isEmpty()) {
            int level = remainder.get("VillagerData").get("level").asInt(1);
            return remainder.set("Xp", remainder.createInt(VillagerRebuildLevelAndXpFix.getMinXpPerLevel(level)));
         } else {
            return remainder;
         }
      });
   }
}
