package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.stream.IntStream;

public class WorldSpawnDataFix extends DataFix {
   public WorldSpawnDataFix(final Schema outputSchema) {
      super(outputSchema, false);
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "WorldSpawnDataFix",
         this.getInputSchema().getType(References.LEVEL),
         input -> input.update(
            DSL.remainderFinder(),
            tag -> {
               int spawnX = tag.get("SpawnX").asInt(0);
               int spawnY = tag.get("SpawnY").asInt(0);
               int spawnZ = tag.get("SpawnZ").asInt(0);
               float angle = tag.get("SpawnAngle").asFloat(0.0F);
               Dynamic<?> spawnData = tag.emptyMap()
                  .set("dimension", tag.createString("minecraft:overworld"))
                  .set("pos", tag.createIntList(IntStream.of(spawnX, spawnY, spawnZ)))
                  .set("yaw", tag.createFloat(angle))
                  .set("pitch", tag.createFloat(0.0F));
               tag = tag.remove("SpawnX").remove("SpawnY").remove("SpawnZ").remove("SpawnAngle");
               return tag.set("spawn", spawnData);
            }
         )
      );
   }
}
