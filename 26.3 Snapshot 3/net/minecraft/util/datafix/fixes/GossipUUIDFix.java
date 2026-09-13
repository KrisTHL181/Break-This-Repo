package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class GossipUUIDFix extends NamedEntityFix {
   public GossipUUIDFix(final Schema outputSchema, final String entityName) {
      super(outputSchema, false, "Gossip for for " + entityName, References.ENTITY, entityName);
   }

   @Override
   protected Typed<?> fix(final Typed<?> entity) {
      return entity.update(
         DSL.remainderFinder(),
         tag -> tag.update(
            "Gossips",
            gossips -> (Dynamic)DataFixUtils.orElse(
               gossips.asStreamOpt()
                  .result()
                  .map(s -> s.map(gossip -> (Dynamic)AbstractUUIDFix.replaceUUIDLeastMost((Dynamic<?>)gossip, "Target", "Target").orElse((Dynamic<?>)gossip)))
                  .map(gossips::createList),
               gossips
            )
         )
      );
   }
}
