package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedMap;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V3818_3 extends NamespacedSchema {
   public V3818_3(final int versionKey, final Schema parent) {
      super(versionKey, parent);
   }

   public static SequencedMap<String, Supplier<TypeTemplate>> components(final Schema schema) {
      SequencedMap<String, Supplier<TypeTemplate>> components = new LinkedHashMap<>();
      components.put("minecraft:bees", () -> DSL.list(DSL.optionalFields("entity_data", References.ENTITY_TREE.in(schema))));
      components.put("minecraft:block_entity_data", () -> References.BLOCK_ENTITY.in(schema));
      components.put("minecraft:bundle_contents", () -> DSL.list(References.ITEM_STACK.in(schema)));
      components.put(
         "minecraft:can_break",
         () -> DSL.optionalFields(
            "predicates", DSL.list(DSL.optionalFields("blocks", DSL.or(References.BLOCK_NAME.in(schema), DSL.list(References.BLOCK_NAME.in(schema)))))
         )
      );
      components.put(
         "minecraft:can_place_on",
         () -> DSL.optionalFields(
            "predicates", DSL.list(DSL.optionalFields("blocks", DSL.or(References.BLOCK_NAME.in(schema), DSL.list(References.BLOCK_NAME.in(schema)))))
         )
      );
      components.put("minecraft:charged_projectiles", () -> DSL.list(References.ITEM_STACK.in(schema)));
      components.put("minecraft:container", () -> DSL.list(DSL.optionalFields("item", References.ITEM_STACK.in(schema))));
      components.put("minecraft:entity_data", () -> References.ENTITY_TREE.in(schema));
      components.put("minecraft:pot_decorations", () -> DSL.list(References.ITEM_NAME.in(schema)));
      components.put("minecraft:food", () -> DSL.optionalFields("using_converts_to", References.ITEM_STACK.in(schema)));
      components.put("minecraft:custom_name", () -> References.TEXT_COMPONENT.in(schema));
      components.put("minecraft:item_name", () -> References.TEXT_COMPONENT.in(schema));
      components.put("minecraft:lore", () -> DSL.list(References.TEXT_COMPONENT.in(schema)));
      components.put(
         "minecraft:written_book_content",
         () -> DSL.optionalFields(
            "pages",
            DSL.list(
               DSL.or(
                  DSL.optionalFields("raw", References.TEXT_COMPONENT.in(schema), "filtered", References.TEXT_COMPONENT.in(schema)),
                  References.TEXT_COMPONENT.in(schema)
               )
            )
         )
      );
      return components;
   }

   public void registerTypes(
      final Schema schema, final Map<String, Supplier<TypeTemplate>> entityTypes, final Map<String, Supplier<TypeTemplate>> blockEntityTypes
   ) {
      super.registerTypes(schema, entityTypes, blockEntityTypes);
      schema.registerType(true, References.DATA_COMPONENTS, () -> DSL.optionalFieldsLazy(components(schema)));
   }
}
