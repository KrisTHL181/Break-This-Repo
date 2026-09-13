package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V1928 extends NamespacedSchema {
   public V1928(int p_17798_, Schema p_17799_) {
      super(p_17798_, p_17799_);
   }

   protected static void registerMob(Schema p_17803_, Map<String, Supplier<TypeTemplate>> p_17804_, String p_17805_) {
      p_17803_.registerSimple(p_17804_, p_17805_);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_17809_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_17809_);
      map.remove("minecraft:illager_beast");
      registerMob(p_17809_, map, "minecraft:ravager");
      return map;
   }
}
