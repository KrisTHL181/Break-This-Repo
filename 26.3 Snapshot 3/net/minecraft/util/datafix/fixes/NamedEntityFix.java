package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;

public abstract class NamedEntityFix extends DataFix {
   private final String name;
   protected final String entityName;
   protected final TypeReference type;

   public NamedEntityFix(final Schema outputSchema, final boolean changesType, final String name, final TypeReference type, final String entityName) {
      super(outputSchema, changesType);
      this.name = name;
      this.type = type;
      this.entityName = entityName;
   }

   public TypeRewriteRule makeRule() {
      OpticFinder<?> entityF = DSL.namedChoice(this.entityName, this.getInputSchema().getChoiceType(this.type, this.entityName));
      return this.fixTypeEverywhereTyped(
         this.name,
         this.getInputSchema().getType(this.type),
         this.getOutputSchema().getType(this.type),
         input -> input.updateTyped(entityF, this.getOutputSchema().getChoiceType(this.type, this.entityName), this::fix)
      );
   }

   protected abstract Typed<?> fix(final Typed<?> entity);
}
