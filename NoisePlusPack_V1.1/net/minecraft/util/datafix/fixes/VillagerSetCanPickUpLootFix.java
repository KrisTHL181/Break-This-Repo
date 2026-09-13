package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class VillagerSetCanPickUpLootFix extends NamedEntityFix {
   private static final String CAN_PICK_UP_LOOT = "CanPickUpLoot";

   public VillagerSetCanPickUpLootFix(Schema p_365653_) {
      super(p_365653_, true, "Villager CanPickUpLoot default value", References.ENTITY, "Villager");
   }

   @Override
   protected Typed<?> fix(Typed<?> p_360984_) {
      return p_360984_.update(DSL.remainderFinder(), VillagerSetCanPickUpLootFix::fixValue);
   }

   private static Dynamic<?> fixValue(Dynamic<?> p_362251_) {
      return p_362251_.set("CanPickUpLoot", p_362251_.createBoolean(true));
   }
}
