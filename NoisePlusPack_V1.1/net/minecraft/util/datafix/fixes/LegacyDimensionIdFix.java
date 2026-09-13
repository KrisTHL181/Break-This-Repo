package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;

public class LegacyDimensionIdFix extends DataFix {
   public LegacyDimensionIdFix(Schema p_408929_) {
      super(p_408929_, false);
   }

   public TypeRewriteRule makeRule() {
      TypeRewriteRule typerewriterule = this.fixTypeEverywhereTyped(
         "PlayerLegacyDimensionFix", this.getInputSchema().getType(References.PLAYER), p_408950_ -> p_408950_.update(DSL.remainderFinder(), this::fixPlayer)
      );
      Type<?> type = this.getInputSchema().getType(References.SAVED_DATA_MAP_DATA);
      OpticFinder<?> opticfinder = type.findField("data");
      TypeRewriteRule typerewriterule1 = this.fixTypeEverywhereTyped(
         "MapLegacyDimensionFix", type, p_406831_ -> p_406831_.updateTyped(opticfinder, p_406575_ -> p_406575_.update(DSL.remainderFinder(), this::fixMap))
      );
      return TypeRewriteRule.seq(typerewriterule, typerewriterule1);
   }

   private <T> Dynamic<T> fixMap(Dynamic<T> p_407634_) {
      return p_407634_.update("dimension", this::fixDimensionId);
   }

   private <T> Dynamic<T> fixPlayer(Dynamic<T> p_410408_) {
      return p_410408_.update("Dimension", this::fixDimensionId);
   }

   private <T> Dynamic<T> fixDimensionId(Dynamic<T> p_410744_) {
      return (Dynamic<T>)DataFixUtils.orElse(p_410744_.asNumber().result().map(p_408638_ -> {
         return switch (p_408638_.intValue()) {
            case -1 -> p_410744_.createString("minecraft:the_nether");
            case 1 -> p_410744_.createString("minecraft:the_end");
            default -> p_410744_.createString("minecraft:overworld");
         };
      }), p_410744_);
   }
}
