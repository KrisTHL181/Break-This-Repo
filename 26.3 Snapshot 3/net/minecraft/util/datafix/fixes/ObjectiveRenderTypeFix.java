package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import java.util.Optional;

public class ObjectiveRenderTypeFix extends DataFix {
   public ObjectiveRenderTypeFix(final Schema outputSchema) {
      super(outputSchema, false);
   }

   private static String getRenderType(final String criteriaName) {
      return criteriaName.equals("health") ? "hearts" : "integer";
   }

   protected TypeRewriteRule makeRule() {
      Type<?> objectiveType = this.getInputSchema().getType(References.OBJECTIVE);
      return this.fixTypeEverywhereTyped("ObjectiveRenderTypeFix", objectiveType, typed -> typed.update(DSL.remainderFinder(), tag -> {
         Optional<String> renderType = tag.get("RenderType").asString().result();
         if (renderType.isEmpty()) {
            String criteriaName = tag.get("CriteriaName").asString("");
            String defaultRenderType = getRenderType(criteriaName);
            return tag.set("RenderType", tag.createString(defaultRenderType));
         } else {
            return tag;
         }
      }));
   }
}
