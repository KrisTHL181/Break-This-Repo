package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V4302 extends NamespacedSchema {
   public V4302(int p_397562_, Schema p_396494_) {
      super(p_397562_, p_396494_);
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema p_391267_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(p_391267_);
      p_391267_.registerSimple(map, "minecraft:test_block");
      p_391267_.register(
         map,
         "minecraft:test_instance_block",
         () -> DSL.optionalFields(
            "data",
            DSL.optionalFields("error_message", References.TEXT_COMPONENT.in(p_391267_)),
            "errors",
            DSL.list(DSL.optionalFields("text", References.TEXT_COMPONENT.in(p_391267_)))
         )
      );
      return map;
   }
}
