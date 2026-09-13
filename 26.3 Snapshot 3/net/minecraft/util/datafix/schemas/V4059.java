package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.SequencedMap;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V4059 extends NamespacedSchema {
   public V4059(final int versionKey, final Schema parent) {
      super(versionKey, parent);
   }

   public static SequencedMap<String, Supplier<TypeTemplate>> components(final Schema schema) {
      SequencedMap<String, Supplier<TypeTemplate>> components = V3818_3.components(schema);
      components.remove("minecraft:food");
      components.put("minecraft:use_remainder", () -> References.ITEM_STACK.in(schema));
      components.put(
         "minecraft:equippable",
         () -> DSL.optionalFields("allowed_entities", DSL.or(References.ENTITY_NAME.in(schema), DSL.list(References.ENTITY_NAME.in(schema))))
      );
      components.put("minecraft:sulfur_cube_content", () -> References.ITEM_STACK.in(schema));
      return components;
   }

   public void registerTypes(
      final Schema schema, final Map<String, Supplier<TypeTemplate>> entityTypes, final Map<String, Supplier<TypeTemplate>> blockEntityTypes
   ) {
      super.registerTypes(schema, entityTypes, blockEntityTypes);
      schema.registerType(true, References.DATA_COMPONENTS, () -> DSL.optionalFieldsLazy(components(schema)));
   }
}
