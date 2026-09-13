package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;
import net.minecraft.util.datafix.ExtraDataFixUtils;

public class LegacyDragonFightFix extends DataFix {
   public LegacyDragonFightFix(Schema p_289761_) {
      super(p_289761_, false);
   }

   private static <T> Dynamic<T> fixDragonFight(Dynamic<T> p_328149_) {
      return p_328149_.update("ExitPortalLocation", ExtraDataFixUtils::fixBlockPos);
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "LegacyDragonFightFix", this.getInputSchema().getType(References.LEVEL), p_289787_ -> p_289787_.update(DSL.remainderFinder(), p_326607_ -> {
            OptionalDynamic<?> optionaldynamic = p_326607_.get("DragonFight");
            if (optionaldynamic.result().isPresent()) {
               return p_326607_;
            }

            Dynamic<?> dynamic = p_326607_.get("DimensionData").get("1").get("DragonFight").orElseEmptyMap();
            return p_326607_.set("DragonFight", fixDragonFight(dynamic));
         })
      );
   }
}
