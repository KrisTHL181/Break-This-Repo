package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ChunkToProtochunkFix extends DataFix {
   private static final int NUM_SECTIONS = 16;

   public ChunkToProtochunkFix(final Schema outputSchema, final boolean changesType) {
      super(outputSchema, changesType);
   }

   public TypeRewriteRule makeRule() {
      return this.writeFixAndRead(
         "ChunkToProtoChunkFix",
         this.getInputSchema().getType(References.CHUNK),
         this.getOutputSchema().getType(References.CHUNK),
         chunk -> chunk.update("Level", ChunkToProtochunkFix::fixChunkData)
      );
   }

   private static <T> Dynamic<T> fixChunkData(final Dynamic<T> tag) {
      boolean terrainPopulated = tag.get("TerrainPopulated").asBoolean(false);
      boolean lightPopulated = tag.get("LightPopulated").asNumber().result().isEmpty() || tag.get("LightPopulated").asBoolean(false);
      String status;
      if (terrainPopulated) {
         if (lightPopulated) {
            status = "mobs_spawned";
         } else {
            status = "decorated";
         }
      } else {
         status = "carved";
      }

      return repackTicks(repackBiomes(tag)).set("Status", tag.createString(status)).set("hasLegacyStructureData", tag.createBoolean(true));
   }

   private static <T> Dynamic<T> repackBiomes(final Dynamic<T> tag) {
      return tag.update("Biomes", biomes -> (Dynamic)DataFixUtils.orElse(biomes.asByteBufferOpt().result().map(buffer -> {
         int[] newBiomes = new int[256];

         for (int i = 0; i < newBiomes.length; i++) {
            if (i < buffer.capacity()) {
               newBiomes[i] = buffer.get(i) & 255;
            }
         }

         return tag.createIntList(Arrays.stream(newBiomes));
      }), biomes));
   }

   private static <T> Dynamic<T> repackTicks(final Dynamic<T> tag) {
      return (Dynamic<T>)DataFixUtils.orElse(
         tag.get("TileTicks")
            .asStreamOpt()
            .result()
            .map(
               ticks -> {
                  List<ShortList> toBeTickedTag = IntStream.range(0, 16).mapToObj(i -> new ShortArrayList()).collect(Collectors.toList());
                  ticks.forEach(pendingTickTag -> {
                     int x = pendingTickTag.get("x").asInt(0);
                     int y = pendingTickTag.get("y").asInt(0);
                     int z = pendingTickTag.get("z").asInt(0);
                     short packedOffset = packOffsetCoordinates(x, y, z);
                     toBeTickedTag.get(y >> 4).add(packedOffset);
                  });
                  return tag.remove("TileTicks")
                     .set("ToBeTicked", tag.createList(toBeTickedTag.stream().map(l -> tag.createList(l.intStream().mapToObj(v -> tag.createShort((short)v))))));
               }
            ),
         tag
      );
   }

   private static short packOffsetCoordinates(final int x, final int y, final int z) {
      return (short)(x & 15 | (y & 15) << 4 | (z & 15) << 8);
   }
}
