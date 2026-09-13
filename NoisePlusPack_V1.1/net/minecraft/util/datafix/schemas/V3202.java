package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V3202 extends NamespacedSchema {
   public V3202(int p_251767_, Schema p_252009_) {
      super(p_251767_, p_252009_);
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema p_252169_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(p_252169_);
      map.put("minecraft:hanging_sign", () -> V99.sign(p_252169_));
      return map;
   }
}
