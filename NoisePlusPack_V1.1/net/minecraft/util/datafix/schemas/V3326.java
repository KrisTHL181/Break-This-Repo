package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V3326 extends NamespacedSchema {
   public V3326(int p_273243_, Schema p_273484_) {
      super(p_273243_, p_273484_);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_273599_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_273599_);
      p_273599_.registerSimple(map, "minecraft:sniffer");
      return map;
   }
}
