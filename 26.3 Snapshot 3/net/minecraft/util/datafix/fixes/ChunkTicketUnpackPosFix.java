package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import java.util.stream.IntStream;

public class ChunkTicketUnpackPosFix extends DataFix {
   private static final long CHUNK_COORD_BITS = 32L;
   private static final long CHUNK_COORD_MASK = 4294967295L;

   public ChunkTicketUnpackPosFix(final Schema outputSchema) {
      super(outputSchema, false);
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "ChunkTicketUnpackPosFix",
         this.getInputSchema().getType(References.SAVED_DATA_TICKETS),
         input -> input.update(
            DSL.remainderFinder(),
            remainder -> remainder.update(
               "data", data -> data.update("tickets", tickets -> tickets.createList(tickets.asStream().map(ticket -> ticket.update("chunk_pos", chunkPos -> {
                  long key = chunkPos.asLong(0L);
                  int x = (int)(key & 4294967295L);
                  int z = (int)(key >>> 32 & 4294967295L);
                  return chunkPos.createIntList(IntStream.of(x, z));
               }))))
            )
         )
      );
   }
}
