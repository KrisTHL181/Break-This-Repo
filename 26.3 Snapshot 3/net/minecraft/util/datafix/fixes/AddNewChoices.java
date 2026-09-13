package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TaggedChoice.TaggedChoiceType;
import java.util.Locale;

public class AddNewChoices extends DataFix {
   private final String name;
   private final TypeReference type;

   public AddNewChoices(final Schema outputSchema, final String name, final TypeReference type) {
      super(outputSchema, true);
      this.name = name;
      this.type = type;
   }

   public TypeRewriteRule makeRule() {
      TaggedChoiceType<?> inputType = this.getInputSchema().findChoiceType(this.type);
      TaggedChoiceType<?> outputType = this.getOutputSchema().findChoiceType(this.type);
      return this.cap(inputType, outputType);
   }

   private <K> TypeRewriteRule cap(final TaggedChoiceType<K> inputType, final TaggedChoiceType<?> outputType) {
      if (inputType.getKeyType() != outputType.getKeyType()) {
         throw new IllegalStateException("Could not inject: key type is not the same");
      }

      TaggedChoiceType<K> outputChoiceType = (TaggedChoiceType<K>)outputType;
      return this.fixTypeEverywhere(this.name, inputType, outputChoiceType, ops -> input -> {
         if (!outputChoiceType.hasType(input.getFirst())) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "%s: Unknown type %s in '%s'", this.name, input.getFirst(), this.type.typeName()));
         } else {
            return input;
         }
      });
   }
}
