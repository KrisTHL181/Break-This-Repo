package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import java.util.stream.IntStream;

public class ChunkTicketUnpackPosFix extends DataFix {
   private static final long CHUNK_COORD_BITS = 32L;
   private static final long CHUNK_COORD_MASK = 4294967295L;

   public ChunkTicketUnpackPosFix(Schema p_394610_) {
      super(p_394610_, false);
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "ChunkTicketUnpackPosFix",
         this.getInputSchema().getType(References.SAVED_DATA_TICKETS),
         p_393920_ -> p_393920_.update(
            DSL.remainderFinder(),
            p_393991_ -> p_393991_.update(
               "data",
               p_392234_ -> p_392234_.update(
                  "tickets", p_396915_ -> p_396915_.createList(p_396915_.asStream().map(p_391508_ -> p_391508_.update("chunk_pos", p_393742_ -> {
                     long i = p_393742_.asLong(0L);
                     int j = (int)(i & 4294967295L);
                     int k = (int)(i >>> 32 & 4294967295L);
                     return p_393742_.createIntList(IntStream.of(j, k));
                  })))
               )
            )
         )
      );
   }
}
