package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class LegacyWorldBorderFix extends DataFix {
   public LegacyWorldBorderFix(final Schema outputSchema) {
      super(outputSchema, false);
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "LegacyWorldBorderFix",
         this.getInputSchema().getType(References.LEVEL),
         input -> input.update(
            DSL.remainderFinder(),
            tag -> {
               Dynamic<?> worldBorder = tag.emptyMap()
                  .set("center_x", tag.createDouble(tag.get("BorderCenterX").asDouble(0.0)))
                  .set("center_z", tag.createDouble(tag.get("BorderCenterZ").asDouble(0.0)))
                  .set("size", tag.createDouble(tag.get("BorderSize").asDouble(5.999997E7F)))
                  .set("lerp_time", tag.createLong(tag.get("BorderSizeLerpTime").asLong(0L)))
                  .set("lerp_target", tag.createDouble(tag.get("BorderSizeLerpTarget").asDouble(0.0)))
                  .set("safe_zone", tag.createDouble(tag.get("BorderSafeZone").asDouble(5.0)))
                  .set("damage_per_block", tag.createDouble(tag.get("BorderDamagePerBlock").asDouble(0.2)))
                  .set("warning_blocks", tag.createInt(tag.get("BorderWarningBlocks").asInt(5)))
                  .set("warning_time", tag.createInt(tag.get("BorderWarningTime").asInt(15)));
               tag = tag.remove("BorderCenterX")
                  .remove("BorderCenterZ")
                  .remove("BorderSize")
                  .remove("BorderSizeLerpTime")
                  .remove("BorderSizeLerpTarget")
                  .remove("BorderSafeZone")
                  .remove("BorderDamagePerBlock")
                  .remove("BorderWarningBlocks")
                  .remove("BorderWarningTime");
               return tag.set("world_border", worldBorder);
            }
         )
      );
   }
}
