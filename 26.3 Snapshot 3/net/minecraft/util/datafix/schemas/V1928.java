package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V1928 extends NamespacedSchema {
   public V1928(final int versionKey, final Schema parent) {
      super(versionKey, parent);
   }

   protected static void registerMob(final Schema schema, final Map<String, Supplier<TypeTemplate>> map, final String name) {
      schema.registerSimple(map, name);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(final Schema schema) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(schema);
      map.remove("minecraft:illager_beast");
      registerMob(schema, map, "minecraft:ravager");
      return map;
   }
}
