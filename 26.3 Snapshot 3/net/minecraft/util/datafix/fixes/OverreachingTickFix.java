package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.Optional;

public class OverreachingTickFix extends DataFix {
   public OverreachingTickFix(final Schema outputSchema) {
      super(outputSchema, false);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> chunkType = this.getInputSchema().getType(References.CHUNK);
      OpticFinder<?> blockTicksFinder = chunkType.findField("block_ticks");
      return this.fixTypeEverywhereTyped("Handle ticks saved in the wrong chunk", chunkType, chunk -> {
         Optional<? extends Typed<?>> blockTicksOpt = chunk.getOptionalTyped(blockTicksFinder);
         Optional<? extends Dynamic<?>> blockTicks = blockTicksOpt.isPresent() ? blockTicksOpt.get().write().result() : Optional.empty();
         return chunk.update(DSL.remainderFinder(), remainder -> {
            int chunkX = remainder.get("xPos").asInt(0);
            int chunkZ = remainder.get("zPos").asInt(0);
            Optional<? extends Dynamic<?>> fluidTicks = remainder.get("fluid_ticks").get().result();
            remainder = extractOverreachingTicks(remainder, chunkX, chunkZ, blockTicks, "neighbor_block_ticks");
            return extractOverreachingTicks(remainder, chunkX, chunkZ, fluidTicks, "neighbor_fluid_ticks");
         });
      });
   }

   private static Dynamic<?> extractOverreachingTicks(
      Dynamic<?> remainder, final int chunkX, final int chunkZ, final Optional<? extends Dynamic<?>> ticks, final String nameInUpgradeData
   ) {
      if (ticks.isPresent()) {
         List<? extends Dynamic<?>> overreachingTicks = ticks.get().asStream().filter(tick -> {
            int x = tick.get("x").asInt(0);
            int z = tick.get("z").asInt(0);
            int distX = Math.abs(chunkX - (x >> 4));
            int distZ = Math.abs(chunkZ - (z >> 4));
            return (distX != 0 || distZ != 0) && distX <= 1 && distZ <= 1;
         }).toList();
         if (!overreachingTicks.isEmpty()) {
            remainder = remainder.set(
               "UpgradeData", remainder.get("UpgradeData").orElseEmptyMap().set(nameInUpgradeData, remainder.createList(overreachingTicks.stream()))
            );
         }
      }

      return remainder;
   }
}
