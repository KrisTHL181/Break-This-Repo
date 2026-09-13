package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class RemoveGolemGossipFix extends NamedEntityFix {
   public RemoveGolemGossipFix(final Schema outputSchema, final boolean changesType) {
      super(outputSchema, changesType, "Remove Golem Gossip Fix", References.ENTITY, "minecraft:villager");
   }

   @Override
   protected Typed<?> fix(final Typed<?> entity) {
      return entity.update(DSL.remainderFinder(), RemoveGolemGossipFix::fixValue);
   }

   private static Dynamic<?> fixValue(final Dynamic<?> tag) {
      return tag.update("Gossips", gossips -> tag.createList(gossips.asStream().filter(attribute -> !attribute.get("Type").asString("").equals("golem"))));
   }
}
