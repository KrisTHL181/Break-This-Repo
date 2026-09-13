package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class FireResistantToDamageResistantComponentFix extends DataComponentRemainderFix {
   public FireResistantToDamageResistantComponentFix(Schema p_365630_) {
      super(p_365630_, "FireResistantToDamageResistantComponentFix", "minecraft:fire_resistant", "minecraft:damage_resistant");
   }

   @Override
   protected <T> Dynamic<T> fixComponent(Dynamic<T> p_367049_) {
      return p_367049_.emptyMap().set("types", p_367049_.createString("#minecraft:is_fire"));
   }
}
