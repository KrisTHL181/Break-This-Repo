package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V3203 extends NamespacedSchema {
   public V3203(int p_248570_, Schema p_251874_) {
      super(p_248570_, p_251874_);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_249141_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_249141_);
      p_249141_.registerSimple(map, "minecraft:camel");
      return map;
   }
}
