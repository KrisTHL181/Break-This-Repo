package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V2831 extends NamespacedSchema {
   public V2831(final int versionKey, final Schema parent) {
      super(versionKey, parent);
   }

   public void registerTypes(
      final Schema schema, final Map<String, Supplier<TypeTemplate>> entityTypes, final Map<String, Supplier<TypeTemplate>> blockEntityTypes
   ) {
      super.registerTypes(schema, entityTypes, blockEntityTypes);
      schema.registerType(
         true,
         References.UNTAGGED_SPAWNER,
         () -> DSL.optionalFields(
            "SpawnPotentials",
            DSL.list(DSL.fields("data", DSL.fields("entity", References.ENTITY_TREE.in(schema)))),
            "SpawnData",
            DSL.fields("entity", References.ENTITY_TREE.in(schema))
         )
      );
   }
}
