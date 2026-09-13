package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.function.UnaryOperator;

public class CriteriaRenameFix extends DataFix {
   private final String name;
   private final String advancementId;
   private final UnaryOperator<String> conversions;

   public CriteriaRenameFix(final Schema outputSchema, final String name, final String advancementId, final UnaryOperator<String> conversions) {
      super(outputSchema, false);
      this.name = name;
      this.advancementId = advancementId;
      this.conversions = conversions;
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         this.name, this.getInputSchema().getType(References.ADVANCEMENTS), input -> input.update(DSL.remainderFinder(), this::fixAdvancements)
      );
   }

   private Dynamic<?> fixAdvancements(final Dynamic<?> tag) {
      return tag.update(
         this.advancementId,
         advancement -> advancement.update(
            "criteria",
            criteria -> criteria.updateMapValues(
               e -> e.mapFirst(k -> (Dynamic)DataFixUtils.orElse(k.asString().map(s -> k.createString(this.conversions.apply(s))).result(), k))
            )
         )
      );
   }
}
