package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V808 extends NamespacedSchema {
   public V808(final int versionKey, final Schema parent) {
      super(versionKey, parent);
   }

   protected static void registerInventory(final Schema schema, final Map<String, Supplier<TypeTemplate>> map, final String name) {
      schema.register(map, name, () -> DSL.optionalFields("Items", DSL.list(References.ITEM_STACK.in(schema))));
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(final Schema schema) {
      Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(schema);
      registerInventory(schema, map, "minecraft:shulker_box");
      return map;
   }
}
