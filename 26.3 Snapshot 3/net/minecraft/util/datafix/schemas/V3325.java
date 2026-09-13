package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V3325 extends NamespacedSchema {
   public V3325(final int versionKey, final Schema parent) {
      super(versionKey, parent);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(final Schema schema) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(schema);
      schema.register(map, "minecraft:item_display", name -> DSL.optionalFields("item", References.ITEM_STACK.in(schema)));
      schema.register(map, "minecraft:block_display", name -> DSL.optionalFields("block_state", References.BLOCK_STATE.in(schema)));
      schema.register(map, "minecraft:text_display", () -> DSL.optionalFields("text", References.TEXT_COMPONENT.in(schema)));
      return map;
   }
}
