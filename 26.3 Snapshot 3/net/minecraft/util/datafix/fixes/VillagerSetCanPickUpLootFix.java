package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class VillagerSetCanPickUpLootFix extends NamedEntityFix {
   private static final String CAN_PICK_UP_LOOT = "CanPickUpLoot";

   public VillagerSetCanPickUpLootFix(final Schema outputSchema) {
      super(outputSchema, true, "Villager CanPickUpLoot default value", References.ENTITY, "Villager");
   }

   @Override
   protected Typed<?> fix(final Typed<?> entity) {
      return entity.update(DSL.remainderFinder(), VillagerSetCanPickUpLootFix::fixValue);
   }

   private static Dynamic<?> fixValue(final Dynamic<?> tag) {
      return tag.set("CanPickUpLoot", tag.createBoolean(true));
   }
}
