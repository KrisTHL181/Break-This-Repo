package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Map;
import java.util.Map.Entry;

public class EntityFieldsRenameFix extends NamedEntityFix {
   private final Map<String, String> renames;

   public EntityFieldsRenameFix(final Schema outputSchema, final String name, final String entityType, final Map<String, String> renames) {
      super(outputSchema, false, name, References.ENTITY, entityType);
      this.renames = renames;
   }

   public Dynamic<?> fixTag(Dynamic<?> data) {
      for (Entry<String, String> entry : this.renames.entrySet()) {
         data = data.renameField(entry.getKey(), entry.getValue());
      }

      return data;
   }

   @Override
   protected Typed<?> fix(final Typed<?> entity) {
      return entity.update(DSL.remainderFinder(), this::fixTag);
   }
}
