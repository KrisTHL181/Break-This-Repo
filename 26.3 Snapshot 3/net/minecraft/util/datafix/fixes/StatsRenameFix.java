package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.TaggedChoice.TaggedChoiceType;
import java.util.Map;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class StatsRenameFix extends DataFix {
   private final String name;
   private final Map<String, String> renames;

   public StatsRenameFix(final Schema outputSchema, final String name, final Map<String, String> renames) {
      super(outputSchema, false);
      this.name = name;
      this.renames = renames;
   }

   protected TypeRewriteRule makeRule() {
      return TypeRewriteRule.seq(this.createStatRule(), this.createCriteriaRule());
   }

   private TypeRewriteRule createCriteriaRule() {
      Type<?> outputType = this.getOutputSchema().getType(References.OBJECTIVE);
      Type<?> inputType = this.getInputSchema().getType(References.OBJECTIVE);
      OpticFinder<?> criteriaTypeFinder = inputType.findField("CriteriaType");
      TaggedChoiceType<?> choiceType = (TaggedChoiceType<?>)criteriaTypeFinder.type()
         .findChoiceType("type", -1)
         .orElseThrow(() -> new IllegalStateException("Can't find choice type for criteria"));
      Type<?> customFieldType = (Type<?>)choiceType.types().get("minecraft:custom");
      if (customFieldType == null) {
         throw new IllegalStateException("Failed to find custom criterion type variant");
      }

      OpticFinder<?> customTypeFinder = DSL.namedChoice("minecraft:custom", customFieldType);
      OpticFinder<String> idFinder = DSL.fieldFinder("id", NamespacedSchema.namespacedString());
      return this.fixTypeEverywhereTyped(
         this.name,
         inputType,
         outputType,
         input -> input.updateTyped(
            criteriaTypeFinder,
            stats -> stats.updateTyped(customTypeFinder, custom -> custom.update(idFinder, value -> this.renames.getOrDefault(value, value)))
         )
      );
   }

   private TypeRewriteRule createStatRule() {
      Type<?> outputType = this.getOutputSchema().getType(References.STATS);
      Type<?> inputType = this.getInputSchema().getType(References.STATS);
      OpticFinder<?> statsFinder = inputType.findField("stats");
      OpticFinder<?> customFinder = statsFinder.type().findField("minecraft:custom");
      OpticFinder<String> nameFinder = NamespacedSchema.namespacedString().finder();
      return this.fixTypeEverywhereTyped(
         this.name,
         inputType,
         outputType,
         input -> input.updateTyped(
            statsFinder, stats -> stats.updateTyped(customFinder, custom -> custom.update(nameFinder, value -> this.renames.getOrDefault(value, value)))
         )
      );
   }
}
