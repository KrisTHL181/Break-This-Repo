package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class CopperGolemWeatherStateFix extends NamedEntityFix {
   public CopperGolemWeatherStateFix(Schema p_422355_) {
      super(p_422355_, false, "CopperGolemWeatherStateFix", References.ENTITY, "minecraft:copper_golem");
   }

   @Override
   protected Typed<?> fix(Typed<?> p_429584_) {
      return p_429584_.update(DSL.remainderFinder(), p_425081_ -> p_425081_.update("weather_state", CopperGolemWeatherStateFix::fixWeatherState));
   }

   private static Dynamic<?> fixWeatherState(Dynamic<?> p_426793_) {
      return switch (p_426793_.asInt(0)) {
         case 1 -> p_426793_.createString("exposed");
         case 2 -> p_426793_.createString("weathered");
         case 3 -> p_426793_.createString("oxidized");
         default -> p_426793_.createString("unaffected");
      };
   }
}
