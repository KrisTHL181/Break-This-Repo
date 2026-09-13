package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.TaggedChoice.TaggedChoiceType;
import com.mojang.datafixers.util.Pair;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public abstract class SimplestEntityRenameFix extends DataFix {
   private final String name;

   public SimplestEntityRenameFix(final String name, final Schema outputSchema, final boolean changesType) {
      super(outputSchema, changesType);
      this.name = name;
   }

   public TypeRewriteRule makeRule() {
      TaggedChoiceType<String> oldType = this.getInputSchema().findChoiceType(References.ENTITY);
      TaggedChoiceType<String> newType = this.getOutputSchema().findChoiceType(References.ENTITY);
      Type<Pair<String, String>> entityNameType = DSL.named(References.ENTITY_NAME.typeName(), NamespacedSchema.namespacedString());
      if (!Objects.equals(this.getOutputSchema().getType(References.ENTITY_NAME), entityNameType)) {
         throw new IllegalStateException("Entity name type is not what was expected.");
      } else {
         return TypeRewriteRule.seq(this.fixTypeEverywhere(this.name, oldType, newType, ops -> input -> input.mapFirst(oldName -> {
            String newName = this.rename(oldName);
            Type<?> oldEntityType = (Type<?>)oldType.types().get(oldName);
            Type<?> newEntityType = (Type<?>)newType.types().get(newName);
            if (!newEntityType.equals(oldEntityType, true, true)) {
               throw new IllegalStateException(String.format(Locale.ROOT, "Dynamic type check failed: %s not equal to %s", newEntityType, oldEntityType));
            } else {
               return newName;
            }
         })), this.fixTypeEverywhere(this.name + " for entity name", entityNameType, ops -> input -> input.mapSecond(this::rename)));
      }
   }

   protected abstract String rename(final String name);
}
