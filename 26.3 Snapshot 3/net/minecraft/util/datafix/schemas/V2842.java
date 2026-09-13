package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V2842 extends NamespacedSchema {
   public V2842(final int versionKey, final Schema parent) {
      super(versionKey, parent);
   }

   public void registerTypes(
      final Schema schema, final Map<String, Supplier<TypeTemplate>> entityTypes, final Map<String, Supplier<TypeTemplate>> blockEntityTypes
   ) {
      super.registerTypes(schema, entityTypes, blockEntityTypes);
      schema.registerType(
         false,
         References.CHUNK,
         () -> DSL.optionalFields(
            "entities",
            DSL.list(References.ENTITY_TREE.in(schema)),
            "block_entities",
            DSL.list(DSL.or(References.BLOCK_ENTITY.in(schema), DSL.remainder())),
            "block_ticks",
            DSL.list(DSL.fields("i", References.BLOCK_NAME.in(schema))),
            "sections",
            DSL.list(
               DSL.optionalFields(
                  "biomes",
                  DSL.optionalFields("palette", DSL.list(References.BIOME.in(schema))),
                  "block_states",
                  DSL.optionalFields("palette", DSL.list(References.BLOCK_STATE.in(schema)))
               )
            ),
            "structures",
            DSL.optionalFields("starts", DSL.compoundList(References.STRUCTURE_FEATURE.in(schema)))
         )
      );
   }
}
