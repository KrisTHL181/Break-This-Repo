package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import java.util.Objects;
import java.util.function.UnaryOperator;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class NamespacedTypeRenameFix extends DataFix {
   private final String name;
   private final TypeReference type;
   private final UnaryOperator<String> renamer;

   public NamespacedTypeRenameFix(final Schema outputSchema, final String name, final TypeReference type, final UnaryOperator<String> renamer) {
      super(outputSchema, false);
      this.name = name;
      this.type = type;
      this.renamer = renamer;
   }

   protected TypeRewriteRule makeRule() {
      Type<Pair<String, String>> fieldType = DSL.named(this.type.typeName(), NamespacedSchema.namespacedString());
      if (!Objects.equals(fieldType, this.getInputSchema().getType(this.type))) {
         throw new IllegalStateException("\"" + this.type.typeName() + "\" is not what was expected.");
      } else {
         return this.fixTypeEverywhere(this.name, fieldType, ops -> input -> input.mapSecond(this.renamer));
      }
   }
}
