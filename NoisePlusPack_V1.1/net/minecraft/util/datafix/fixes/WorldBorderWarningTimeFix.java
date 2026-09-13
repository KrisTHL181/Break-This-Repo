package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;

public class WorldBorderWarningTimeFix extends DataFix {
   public WorldBorderWarningTimeFix(Schema p_451619_) {
      super(p_451619_, false);
   }

   protected TypeRewriteRule makeRule() {
      return this.writeFixAndRead(
         "WorldBorderWarningTimeFix",
         this.getInputSchema().getType(References.SAVED_DATA_WORLD_BORDER),
         this.getOutputSchema().getType(References.SAVED_DATA_WORLD_BORDER),
         p_455039_ -> p_455039_.update("data", p_457585_ -> p_457585_.update("warning_time", p_454882_ -> p_457585_.createInt(p_454882_.asInt(15) * 20)))
      );
   }
}
