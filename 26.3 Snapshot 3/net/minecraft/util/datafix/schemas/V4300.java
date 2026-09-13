package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V4300 extends NamespacedSchema {
   public V4300(final int versionKey, final Schema parent) {
      super(versionKey, parent);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(final Schema schema) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(schema);
      schema.register(map, "minecraft:llama", name -> entityWithInventory(schema));
      schema.register(map, "minecraft:trader_llama", name -> entityWithInventory(schema));
      schema.register(map, "minecraft:donkey", name -> entityWithInventory(schema));
      schema.register(map, "minecraft:mule", name -> entityWithInventory(schema));
      schema.registerSimple(map, "minecraft:horse");
      schema.registerSimple(map, "minecraft:skeleton_horse");
      schema.registerSimple(map, "minecraft:zombie_horse");
      return map;
   }

   private static TypeTemplate entityWithInventory(final Schema schema) {
      return DSL.optionalFields("Items", DSL.list(References.ITEM_STACK.in(schema)));
   }
}
