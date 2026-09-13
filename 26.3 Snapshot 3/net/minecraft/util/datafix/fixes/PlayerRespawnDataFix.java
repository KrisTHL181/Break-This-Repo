package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;

public class PlayerRespawnDataFix extends DataFix {
   public PlayerRespawnDataFix(final Schema outputSchema) {
      super(outputSchema, false);
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "PlayerRespawnDataFix",
         this.getInputSchema().getType(References.PLAYER),
         input -> input.update(
            DSL.remainderFinder(),
            tag -> tag.update(
               "respawn",
               respawnTag -> respawnTag.set("dimension", respawnTag.createString(respawnTag.get("dimension").asString("minecraft:overworld")))
                  .set("yaw", respawnTag.createFloat(respawnTag.get("angle").asFloat(0.0F)))
                  .set("pitch", respawnTag.createFloat(0.0F))
                  .remove("angle")
            )
         )
      );
   }
}
