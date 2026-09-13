package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;

public class EntitySalmonSizeFix extends NamedEntityFix {
   public EntitySalmonSizeFix(Schema p_370267_) {
      super(p_370267_, false, "EntitySalmonSizeFix", References.ENTITY, "minecraft:salmon");
   }

   @Override
   protected Typed<?> fix(Typed<?> p_370268_) {
      return p_370268_.update(DSL.remainderFinder(), p_370266_ -> {
         String s = p_370266_.get("type").asString("medium");
         return s.equals("large") ? p_370266_ : p_370266_.set("type", p_370266_.createString("medium"));
      });
   }
}
