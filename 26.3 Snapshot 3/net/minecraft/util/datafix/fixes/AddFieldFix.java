package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

public class AddFieldFix extends DataFix {
   private final String name;
   private final TypeReference type;
   private final String fieldName;
   private final String[] path;
   private final Function<Dynamic<?>, Dynamic<?>> fieldGenerator;

   public AddFieldFix(
      final Schema outputSchema, final TypeReference type, final String fieldName, final Function<Dynamic<?>, Dynamic<?>> fieldGenerator, final String... path
   ) {
      super(outputSchema, false);
      this.name = "Adding field `" + fieldName + "` to type `" + type.typeName().toLowerCase(Locale.ROOT) + "`";
      this.type = type;
      this.fieldName = fieldName;
      this.path = path;
      this.fieldGenerator = fieldGenerator;
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         this.name,
         this.getInputSchema().getType(this.type),
         this.getOutputSchema().getType(this.type),
         input -> input.update(DSL.remainderFinder(), dynamic -> this.addField(dynamic, 0))
      );
   }

   private Dynamic<?> addField(final Dynamic<?> dynamic, final int pathIndex) {
      if (pathIndex >= this.path.length) {
         return dynamic.set(this.fieldName, this.fieldGenerator.apply(dynamic));
      }

      Optional<? extends Dynamic<?>> field = dynamic.get(this.path[pathIndex]).result();
      return field.isEmpty() ? dynamic : this.addField((Dynamic<?>)field.get(), pathIndex + 1);
   }
}
