package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.stream.IntStream;

public class WorldSpawnDataFix extends DataFix {
   public WorldSpawnDataFix(Schema p_424564_) {
      super(p_424564_, false);
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "WorldSpawnDataFix",
         this.getInputSchema().getType(References.LEVEL),
         p_427430_ -> p_427430_.update(
            DSL.remainderFinder(),
            p_430212_ -> {
               int i = p_430212_.get("SpawnX").asInt(0);
               int j = p_430212_.get("SpawnY").asInt(0);
               int k = p_430212_.get("SpawnZ").asInt(0);
               float f = p_430212_.get("SpawnAngle").asFloat(0.0F);
               Dynamic<?> dynamic = p_430212_.emptyMap()
                  .set("dimension", p_430212_.createString("minecraft:overworld"))
                  .set("pos", p_430212_.createIntList(IntStream.of(i, j, k)))
                  .set("yaw", p_430212_.createFloat(f))
                  .set("pitch", p_430212_.createFloat(0.0F));
               p_430212_ = p_430212_.remove("SpawnX").remove("SpawnY").remove("SpawnZ").remove("SpawnAngle");
               return p_430212_.set("spawn", dynamic);
            }
         )
      );
   }
}
