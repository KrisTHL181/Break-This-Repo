package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class FireResistantToDamageResistantComponentFix extends DataComponentRemainderFix {
   public FireResistantToDamageResistantComponentFix(final Schema outputSchema) {
      super(outputSchema, "FireResistantToDamageResistantComponentFix", "minecraft:fire_resistant", "minecraft:damage_resistant");
   }

   @Override
   protected <T> Dynamic<T> fixComponent(final Dynamic<T> input) {
      return input.emptyMap().set("types", input.createString("#minecraft:is_fire"));
   }
}
