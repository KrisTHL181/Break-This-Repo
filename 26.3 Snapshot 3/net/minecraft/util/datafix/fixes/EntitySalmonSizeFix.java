package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;

public class EntitySalmonSizeFix extends NamedEntityFix {
   public EntitySalmonSizeFix(final Schema outputSchema) {
      super(outputSchema, false, "EntitySalmonSizeFix", References.ENTITY, "minecraft:salmon");
   }

   @Override
   protected Typed<?> fix(final Typed<?> entity) {
      return entity.update(DSL.remainderFinder(), tag -> {
         String type = tag.get("type").asString("medium");
         return type.equals("large") ? tag : tag.set("type", tag.createString("medium"));
      });
   }
}
