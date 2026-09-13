package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;

public class PlayerRespawnDataFix extends DataFix {
   public PlayerRespawnDataFix(Schema p_430063_) {
      super(p_430063_, false);
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "PlayerRespawnDataFix",
         this.getInputSchema().getType(References.PLAYER),
         p_429654_ -> p_429654_.update(
            DSL.remainderFinder(),
            p_423847_ -> p_423847_.update(
               "respawn",
               p_424271_ -> p_424271_.set("dimension", p_424271_.createString(p_424271_.get("dimension").asString("minecraft:overworld")))
                  .set("yaw", p_424271_.createFloat(p_424271_.get("angle").asFloat(0.0F)))
                  .set("pitch", p_424271_.createFloat(0.0F))
                  .remove("angle")
            )
         )
      );
   }
}
