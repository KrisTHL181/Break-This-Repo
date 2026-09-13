package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V2704 extends NamespacedSchema {
   public V2704(int p_145883_, Schema p_145884_) {
      super(p_145883_, p_145884_);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_145892_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_145892_);
      p_145892_.registerSimple(map, "minecraft:goat");
      return map;
   }
}
