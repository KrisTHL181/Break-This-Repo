package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;

public class ChunkDeleteIgnoredLightDataFix extends DataFix {
   public ChunkDeleteIgnoredLightDataFix(Schema p_216572_) {
      super(p_216572_, true);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(References.CHUNK);
      OpticFinder<?> opticfinder = type.findField("sections");
      return this.fixTypeEverywhereTyped(
         "ChunkDeleteIgnoredLightDataFix",
         type,
         p_216575_ -> {
            boolean flag = ((Dynamic)p_216575_.get(DSL.remainderFinder())).get("isLightOn").asBoolean(false);
            return !flag
               ? p_216575_.updateTyped(
                  opticfinder, p_216577_ -> p_216577_.update(DSL.remainderFinder(), p_216579_ -> p_216579_.remove("BlockLight").remove("SkyLight"))
               )
               : p_216575_;
         }
      );
   }
}
