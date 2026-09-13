package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V4302 extends NamespacedSchema {
   public V4302(final int versionKey, final Schema parent) {
      super(versionKey, parent);
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(final Schema schema) {
      Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(schema);
      schema.registerSimple(map, "minecraft:test_block");
      schema.register(
         map,
         "minecraft:test_instance_block",
         () -> DSL.optionalFields(
            "data",
            DSL.optionalFields("error_message", References.TEXT_COMPONENT.in(schema)),
            "errors",
            DSL.list(DSL.optionalFields("text", References.TEXT_COMPONENT.in(schema)))
         )
      );
      return map;
   }
}
