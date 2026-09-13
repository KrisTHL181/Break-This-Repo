package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import net.minecraft.util.datafix.LegacyComponentDataFixUtils;

public class ScoreboardDisplayNameFix extends DataFix {
   private final String name;
   private final TypeReference type;

   public ScoreboardDisplayNameFix(final Schema outputSchema, final String name, final TypeReference type) {
      super(outputSchema, false);
      this.name = name;
      this.type = type;
   }

   protected TypeRewriteRule makeRule() {
      Type<?> inputType = this.getInputSchema().getType(this.type);
      OpticFinder<?> displayNameF = inputType.findField("DisplayName");
      OpticFinder<Pair<String, String>> textComponentF = DSL.typeFinder(this.getInputSchema().getType(References.TEXT_COMPONENT));
      return this.fixTypeEverywhereTyped(
         this.name,
         inputType,
         team -> team.updateTyped(
            displayNameF,
            displayName -> displayName.update(textComponentF, textComponent -> textComponent.mapSecond(LegacyComponentDataFixUtils::createTextComponentJson))
         )
      );
   }
}
