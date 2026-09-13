package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V4290 extends NamespacedSchema {
   public V4290(final int versionKey, final Schema parent) {
      super(versionKey, parent);
   }

   public void registerTypes(
      final Schema schema, final Map<String, Supplier<TypeTemplate>> entityTypes, final Map<String, Supplier<TypeTemplate>> blockEntityTypes
   ) {
      super.registerTypes(schema, entityTypes, blockEntityTypes);
      schema.registerType(
         true,
         References.TEXT_COMPONENT,
         () -> DSL.or(
            DSL.or(DSL.constType(DSL.string()), DSL.list(References.TEXT_COMPONENT.in(schema))),
            DSL.optionalFields(
               "extra",
               DSL.list(References.TEXT_COMPONENT.in(schema)),
               "separator",
               References.TEXT_COMPONENT.in(schema),
               "hoverEvent",
               DSL.taggedChoice(
                  "action",
                  DSL.string(),
                  Map.of(
                     "show_text",
                     DSL.optionalFields("contents", References.TEXT_COMPONENT.in(schema)),
                     "show_item",
                     DSL.optionalFields("contents", DSL.or(References.ITEM_STACK.in(schema), References.ITEM_NAME.in(schema))),
                     "show_entity",
                     DSL.optionalFields("type", References.ENTITY_NAME.in(schema), "name", References.TEXT_COMPONENT.in(schema))
                  )
               )
            )
         )
      );
   }
}
