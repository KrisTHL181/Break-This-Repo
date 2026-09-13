package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class ZombieVillagerSetVillagerDataFinalized extends NamedEntityFix {
   private static final String VILLAGER_DATA_FINALIZED = "VillagerDataFinalized";

   public ZombieVillagerSetVillagerDataFinalized(final Schema outputSchema) {
      super(outputSchema, true, "Zombie Villager VillagerDataFinalized default value", References.ENTITY, "minecraft:zombie_villager");
   }

   @Override
   protected Typed<?> fix(final Typed<?> entity) {
      return entity.update(DSL.remainderFinder(), ZombieVillagerSetVillagerDataFinalized::fixValue);
   }

   private static Dynamic<?> fixValue(final Dynamic<?> tag) {
      return tag.set("VillagerDataFinalized", tag.createBoolean(true));
   }
}
