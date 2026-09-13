package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V4996_1 extends NamespacedSchema {
   public V4996_1(final int versionKey, final Schema parent) {
      super(versionKey, parent);
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(final Schema schema) {
      Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(schema);
      schema.register(
         map,
         "minecraft:decorated_pot",
         () -> DSL.optionalFields(
            "sherds",
            DSL.optionalFields(
               "back",
               References.ITEM_STACK.in(schema),
               "left",
               References.ITEM_STACK.in(schema),
               "right",
               References.ITEM_STACK.in(schema),
               "front",
               References.ITEM_STACK.in(schema)
            ),
            "item",
            References.ITEM_STACK.in(schema)
         )
      );
      return map;
   }
}
