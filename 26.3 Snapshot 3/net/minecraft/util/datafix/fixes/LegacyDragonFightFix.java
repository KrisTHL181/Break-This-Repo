package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;
import net.minecraft.util.datafix.ExtraDataFixUtils;

public class LegacyDragonFightFix extends DataFix {
   public LegacyDragonFightFix(final Schema outputSchema) {
      super(outputSchema, false);
   }

   private static <T> Dynamic<T> fixDragonFight(final Dynamic<T> tag) {
      return tag.update("ExitPortalLocation", ExtraDataFixUtils::fixBlockPos);
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "LegacyDragonFightFix", this.getInputSchema().getType(References.LEVEL), input -> input.update(DSL.remainderFinder(), tag -> {
            OptionalDynamic<?> dragonFight = tag.get("DragonFight");
            if (dragonFight.result().isPresent()) {
               return tag;
            }

            Dynamic<?> legacyFight = tag.get("DimensionData").get("1").get("DragonFight").orElseEmptyMap();
            return tag.set("DragonFight", fixDragonFight(legacyFight));
         })
      );
   }
}
