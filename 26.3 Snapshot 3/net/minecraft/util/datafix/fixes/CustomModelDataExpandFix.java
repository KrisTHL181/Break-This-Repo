package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import java.util.Map;
import java.util.stream.Stream;

public class CustomModelDataExpandFix extends DataFix {
   public CustomModelDataExpandFix(final Schema outputSchema) {
      super(outputSchema, false);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> componentsType = this.getInputSchema().getType(References.DATA_COMPONENTS);
      return this.fixTypeEverywhereTyped(
         "Custom Model Data expansion",
         componentsType,
         component -> component.update(DSL.remainderFinder(), tag -> tag.update("minecraft:custom_model_data", cmd -> {
            float currentValue = cmd.asNumber(0.0F).floatValue();
            return cmd.createMap(Map.of(cmd.createString("floats"), cmd.createList(Stream.of(cmd.createFloat(currentValue)))));
         }))
      );
   }
}
