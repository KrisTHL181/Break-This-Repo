package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V1931 extends NamespacedSchema {
   public V1931(int p_17822_, Schema p_17823_) {
      super(p_17822_, p_17823_);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_17831_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_17831_);
      p_17831_.registerSimple(map, "minecraft:fox");
      return map;
   }
}
