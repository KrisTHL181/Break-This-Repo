package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V3439 extends NamespacedSchema {
   public V3439(final int versionKey, final Schema parent) {
      super(versionKey, parent);
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(final Schema schema) {
      Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(schema);
      this.register(map, "minecraft:sign", () -> sign(schema));
      return map;
   }

   public static TypeTemplate sign(final Schema schema) {
      return DSL.optionalFields(
         "front_text",
         DSL.optionalFields("messages", DSL.list(References.TEXT_COMPONENT.in(schema)), "filtered_messages", DSL.list(References.TEXT_COMPONENT.in(schema))),
         "back_text",
         DSL.optionalFields("messages", DSL.list(References.TEXT_COMPONENT.in(schema)), "filtered_messages", DSL.list(References.TEXT_COMPONENT.in(schema)))
      );
   }
}
