package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;

public class ForcedChunkToTicketFix extends DataFix {
   public ForcedChunkToTicketFix(Schema p_394940_) {
      super(p_394940_, false);
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "ForcedChunkToTicketFix",
         this.getInputSchema().getType(References.SAVED_DATA_TICKETS),
         p_393948_ -> p_393948_.update(
            DSL.remainderFinder(),
            p_397039_ -> p_397039_.update(
               "data",
               p_395787_ -> p_395787_.renameAndFixField(
                  "Forced",
                  "tickets",
                  p_392097_ -> p_392097_.createList(
                     p_392097_.asLongStream()
                        .mapToObj(
                           p_391711_ -> p_397039_.emptyMap()
                              .set("type", p_397039_.createString("minecraft:forced"))
                              .set("level", p_397039_.createInt(31))
                              .set("ticks_left", p_397039_.createLong(0L))
                              .set("chunk_pos", p_397039_.createLong(p_391711_))
                        )
                  )
               )
            )
         )
      );
   }
}
