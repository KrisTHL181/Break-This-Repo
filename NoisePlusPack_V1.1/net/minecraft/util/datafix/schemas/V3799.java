package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V3799 extends NamespacedSchema {
   public V3799(int p_331534_, Schema p_328281_) {
      super(p_331534_, p_328281_);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_335633_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_335633_);
      p_335633_.registerSimple(map, "minecraft:armadillo");
      return map;
   }
}
