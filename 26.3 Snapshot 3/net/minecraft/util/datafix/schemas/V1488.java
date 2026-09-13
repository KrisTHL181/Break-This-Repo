package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V1488 extends NamespacedSchema {
   public V1488(final int versionKey, final Schema parent) {
      super(versionKey, parent);
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(final Schema schema) {
      Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(schema);
      schema.register(
         map,
         "minecraft:command_block",
         () -> DSL.optionalFields("CustomName", References.TEXT_COMPONENT.in(schema), "LastOutput", References.TEXT_COMPONENT.in(schema))
      );
      return map;
   }
}
