package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class CopperGolemWeatherStateFix extends NamedEntityFix {
   public CopperGolemWeatherStateFix(final Schema outputSchema) {
      super(outputSchema, false, "CopperGolemWeatherStateFix", References.ENTITY, "minecraft:copper_golem");
   }

   @Override
   protected Typed<?> fix(final Typed<?> entity) {
      return entity.update(DSL.remainderFinder(), tag -> tag.update("weather_state", CopperGolemWeatherStateFix::fixWeatherState));
   }

   private static Dynamic<?> fixWeatherState(final Dynamic<?> value) {
      return switch (value.asInt(0)) {
         case 1 -> value.createString("exposed");
         case 2 -> value.createString("weathered");
         case 3 -> value.createString("oxidized");
         default -> value.createString("unaffected");
      };
   }
}
