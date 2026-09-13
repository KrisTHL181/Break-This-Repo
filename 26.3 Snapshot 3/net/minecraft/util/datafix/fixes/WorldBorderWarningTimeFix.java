package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;

public class WorldBorderWarningTimeFix extends DataFix {
   public WorldBorderWarningTimeFix(final Schema outputSchema) {
      super(outputSchema, false);
   }

   protected TypeRewriteRule makeRule() {
      return this.writeFixAndRead(
         "WorldBorderWarningTimeFix",
         this.getInputSchema().getType(References.SAVED_DATA_WORLD_BORDER),
         this.getOutputSchema().getType(References.SAVED_DATA_WORLD_BORDER),
         input -> input.update("data", tag -> tag.update("warning_time", warningTime -> tag.createInt(warningTime.asInt(15) * 20)))
      );
   }
}
