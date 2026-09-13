package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;

public class EntityGoatMissingStateFix extends NamedEntityFix {
   public EntityGoatMissingStateFix(Schema p_238339_) {
      super(p_238339_, false, "EntityGoatMissingStateFix", References.ENTITY, "minecraft:goat");
   }

   @Override
   protected Typed<?> fix(Typed<?> p_238295_) {
      return p_238295_.update(
         DSL.remainderFinder(), p_238370_ -> p_238370_.set("HasLeftHorn", p_238370_.createBoolean(true)).set("HasRightHorn", p_238370_.createBoolean(true))
      );
   }
}
