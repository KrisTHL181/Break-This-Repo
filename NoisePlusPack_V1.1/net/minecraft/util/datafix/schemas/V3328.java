package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V3328 extends NamespacedSchema {
   public V3328(int p_272971_, Schema p_273487_) {
      super(p_272971_, p_273487_);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_273489_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_273489_);
      p_273489_.registerSimple(map, "minecraft:interaction");
      return map;
   }
}
