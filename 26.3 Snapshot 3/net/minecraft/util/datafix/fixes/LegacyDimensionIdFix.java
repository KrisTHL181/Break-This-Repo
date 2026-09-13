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
   public LegacyDimensionIdFix(final Schema outputSchema) {
      super(outputSchema, false);
   }

   public TypeRewriteRule makeRule() {
      TypeRewriteRule playerRule = this.fixTypeEverywhereTyped(
         "PlayerLegacyDimensionFix", this.getInputSchema().getType(References.PLAYER), input -> input.update(DSL.remainderFinder(), this::fixPlayer)
      );
      Type<?> dataType = this.getInputSchema().getType(References.SAVED_DATA_MAP_DATA);
      OpticFinder<?> mapDataF = dataType.findField("data");
      TypeRewriteRule mapRule = this.fixTypeEverywhereTyped(
         "MapLegacyDimensionFix", dataType, input -> input.updateTyped(mapDataF, data -> data.update(DSL.remainderFinder(), this::fixMap))
      );
      return TypeRewriteRule.seq(playerRule, mapRule);
   }

   private <T> Dynamic<T> fixMap(final Dynamic<T> remainder) {
      return remainder.update("dimension", this::fixDimensionId);
   }

   private <T> Dynamic<T> fixPlayer(final Dynamic<T> remainder) {
      return remainder.update("Dimension", this::fixDimensionId);
   }

   private <T> Dynamic<T> fixDimensionId(final Dynamic<T> id) {
      return (Dynamic<T>)DataFixUtils.orElse(id.asNumber().result().map(legacyId -> {
         return switch (legacyId.intValue()) {
            case -1 -> id.createString("minecraft:the_nether");
            case 1 -> id.createString("minecraft:the_end");
            default -> id.createString("minecraft:overworld");
         };
      }), id);
   }
}
