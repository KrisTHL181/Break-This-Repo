package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class DayTimeToClockFix extends DataFix {
   public DayTimeToClockFix(final Schema outputSchema) {
      super(outputSchema, false);
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "DayTimeToClockFix", this.getInputSchema().getType(References.LEVEL), typed -> typed.update(DSL.remainderFinder(), input -> {
            long gameTime = input.get("Time").asLong(0L);
            long dayTime = input.get("DayTime").asLong(gameTime);
            input = input.remove("DayTime");
            Dynamic<?> overworldClock = createClock(input, dayTime);
            return input.set("world_clocks", input.emptyMap().set("minecraft:overworld", overworldClock));
         })
      );
   }

   private static Dynamic<?> createClock(final Dynamic<?> input, final long totalTicks) {
      return input.emptyMap().set("total_ticks", input.createLong(totalTicks)).set("paused", input.createBoolean(false));
   }
}
