package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import net.minecraft.util.datafix.schemas.NamespacedSchema;
import org.apache.commons.lang3.mutable.MutableBoolean;

public class FixWolfHealth extends NamedEntityFix {
   private static final String WOLF_ID = "minecraft:wolf";
   private static final String WOLF_HEALTH = "minecraft:generic.max_health";

   public FixWolfHealth(final Schema outputSchema) {
      super(outputSchema, false, "FixWolfHealth", References.ENTITY, "minecraft:wolf");
   }

   @Override
   protected Typed<?> fix(final Typed<?> entity) {
      return entity.update(
         DSL.remainderFinder(),
         dynamic -> {
            MutableBoolean healthAdjusted = new MutableBoolean(false);
            dynamic = dynamic.update(
               "Attributes",
               attributes -> attributes.createList(
                  attributes.asStream()
                     .map(
                        attribute -> "minecraft:generic.max_health".equals(NamespacedSchema.ensureNamespaced(attribute.get("Name").asString("")))
                           ? attribute.update("Base", base -> {
                              if (base.asDouble(0.0) == 20.0) {
                                 healthAdjusted.setTrue();
                                 return base.createDouble(40.0);
                              } else {
                                 return base;
                              }
                           })
                           : attribute
                     )
               )
            );
            if (healthAdjusted.isTrue()) {
               dynamic = dynamic.update("Health", health -> health.createFloat(health.asFloat(0.0F) * 2.0F));
            }

            return dynamic;
         }
      );
   }
}
