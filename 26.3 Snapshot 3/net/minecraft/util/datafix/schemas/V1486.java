package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V1486 extends NamespacedSchema {
   public V1486(final int versionKey, final Schema parent) {
      super(versionKey, parent);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(final Schema schema) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(schema);
      map.put("minecraft:cod", map.remove("minecraft:cod_mob"));
      map.put("minecraft:salmon", map.remove("minecraft:salmon_mob"));
      return map;
   }
}
