package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import java.util.Optional;

public class ZombieVillagerRebuildXpFix extends NamedEntityFix {
   public ZombieVillagerRebuildXpFix(Schema p_17298_, boolean p_17299_) {
      super(p_17298_, p_17299_, "Zombie Villager XP rebuild", References.ENTITY, "minecraft:zombie_villager");
   }

   @Override
   protected Typed<?> fix(Typed<?> p_17301_) {
      return p_17301_.update(DSL.remainderFinder(), p_326676_ -> {
         Optional<Number> optional = p_326676_.get("Xp").asNumber().result();
         if (optional.isEmpty()) {
            int i = p_326676_.get("VillagerData").get("level").asInt(1);
            return p_326676_.set("Xp", p_326676_.createInt(VillagerRebuildLevelAndXpFix.getMinXpPerLevel(i)));
         } else {
            return p_326676_;
         }
      });
   }
}
