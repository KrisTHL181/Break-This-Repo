package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V3439 extends NamespacedSchema {
   public V3439(int p_396938_, Schema p_397912_) {
      super(p_396938_, p_397912_);
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema p_393475_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(p_393475_);
      this.register(map, "minecraft:sign", () -> sign(p_393475_));
      return map;
   }

   public static TypeTemplate sign(Schema p_391619_) {
      return DSL.optionalFields(
         "front_text",
         DSL.optionalFields(
            "messages", DSL.list(References.TEXT_COMPONENT.in(p_391619_)), "filtered_messages", DSL.list(References.TEXT_COMPONENT.in(p_391619_))
         ),
         "back_text",
         DSL.optionalFields(
            "messages", DSL.list(References.TEXT_COMPONENT.in(p_391619_)), "filtered_messages", DSL.list(References.TEXT_COMPONENT.in(p_391619_))
         )
      );
   }
}
