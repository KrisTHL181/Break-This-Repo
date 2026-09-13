package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V100 extends Schema {
   public V100(final int versionKey, final Schema parent) {
      super(versionKey, parent);
   }

   public void registerTypes(
      final Schema schema, final Map<String, Supplier<TypeTemplate>> entityTypes, final Map<String, Supplier<TypeTemplate>> blockEntityTypes
   ) {
      super.registerTypes(schema, entityTypes, blockEntityTypes);
      schema.registerType(
         true,
         References.ENTITY_EQUIPMENT,
         () -> DSL.and(
            DSL.optional(DSL.field("ArmorItems", DSL.list(References.ITEM_STACK.in(schema)))),
            new TypeTemplate[]{
               DSL.optional(DSL.field("HandItems", DSL.list(References.ITEM_STACK.in(schema)))),
               DSL.optional(DSL.field("body_armor_item", References.ITEM_STACK.in(schema))),
               DSL.optional(DSL.field("saddle", References.ITEM_STACK.in(schema)))
            }
         )
      );
   }
}
