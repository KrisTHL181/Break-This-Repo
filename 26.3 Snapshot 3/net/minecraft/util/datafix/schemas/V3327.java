package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V3327 extends NamespacedSchema {
   public V3327(final int versionKey, final Schema parent) {
      super(versionKey, parent);
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(final Schema schema) {
      Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(schema);
      schema.register(
         map,
         "minecraft:decorated_pot",
         () -> DSL.optionalFields("shards", DSL.list(References.ITEM_NAME.in(schema)), "item", References.ITEM_STACK.in(schema))
      );
      schema.register(map, "minecraft:suspicious_sand", () -> DSL.optionalFields("item", References.ITEM_STACK.in(schema)));
      return map;
   }
}
