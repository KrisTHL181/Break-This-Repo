package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class LegacyWorldBorderFix extends DataFix {
   public LegacyWorldBorderFix(Schema p_431566_) {
      super(p_431566_, false);
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "LegacyWorldBorderFix",
         this.getInputSchema().getType(References.LEVEL),
         p_430066_ -> p_430066_.update(
            DSL.remainderFinder(),
            p_424210_ -> {
               Dynamic<?> dynamic = p_424210_.emptyMap()
                  .set("center_x", p_424210_.createDouble(p_424210_.get("BorderCenterX").asDouble(0.0)))
                  .set("center_z", p_424210_.createDouble(p_424210_.get("BorderCenterZ").asDouble(0.0)))
                  .set("size", p_424210_.createDouble(p_424210_.get("BorderSize").asDouble(5.999997E7F)))
                  .set("lerp_time", p_424210_.createLong(p_424210_.get("BorderSizeLerpTime").asLong(0L)))
                  .set("lerp_target", p_424210_.createDouble(p_424210_.get("BorderSizeLerpTarget").asDouble(0.0)))
                  .set("safe_zone", p_424210_.createDouble(p_424210_.get("BorderSafeZone").asDouble(5.0)))
                  .set("damage_per_block", p_424210_.createDouble(p_424210_.get("BorderDamagePerBlock").asDouble(0.2)))
                  .set("warning_blocks", p_424210_.createInt(p_424210_.get("BorderWarningBlocks").asInt(5)))
                  .set("warning_time", p_424210_.createInt(p_424210_.get("BorderWarningTime").asInt(15)));
               p_424210_ = p_424210_.remove("BorderCenterX")
                  .remove("BorderCenterZ")
                  .remove("BorderSize")
                  .remove("BorderSizeLerpTime")
                  .remove("BorderSizeLerpTarget")
                  .remove("BorderSafeZone")
                  .remove("BorderDamagePerBlock")
                  .remove("BorderWarningBlocks")
                  .remove("BorderWarningTime");
               return p_424210_.set("world_border", dynamic);
            }
         )
      );
   }
}
