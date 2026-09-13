package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import net.minecraft.util.datafix.schemas.NamespacedSchema;
import org.apache.commons.lang3.mutable.MutableBoolean;

public class FixWolfHealth extends NamedEntityFix {
   private static final String WOLF_ID = "minecraft:wolf";
   private static final String WOLF_HEALTH = "minecraft:generic.max_health";

   public FixWolfHealth(Schema p_394005_) {
      super(p_394005_, false, "FixWolfHealth", References.ENTITY, "minecraft:wolf");
   }

   @Override
   protected Typed<?> fix(Typed<?> p_396983_) {
      return p_396983_.update(
         DSL.remainderFinder(),
         p_394363_ -> {
            MutableBoolean mutableboolean = new MutableBoolean(false);
            p_394363_ = p_394363_.update(
               "Attributes",
               p_395655_ -> p_395655_.createList(
                  p_395655_.asStream()
                     .map(
                        p_395771_ -> "minecraft:generic.max_health".equals(NamespacedSchema.ensureNamespaced(p_395771_.get("Name").asString("")))
                           ? p_395771_.update("Base", p_394759_ -> {
                              if (p_394759_.asDouble(0.0) == 20.0) {
                                 mutableboolean.setTrue();
                                 return p_394759_.createDouble(40.0);
                              } else {
                                 return p_394759_;
                              }
                           })
                           : p_395771_
                     )
               )
            );
            if (mutableboolean.isTrue()) {
               p_394363_ = p_394363_.update("Health", p_397851_ -> p_397851_.createFloat(p_397851_.asFloat(0.0F) * 2.0F));
            }

            return p_394363_;
         }
      );
   }
}
