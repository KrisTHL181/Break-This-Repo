package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V2509 extends NamespacedSchema {
   public V2509(int p_17881_, Schema p_17882_) {
      super(p_17881_, p_17882_);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_17890_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_17890_);
      map.remove("minecraft:zombie_pigman");
      p_17890_.registerSimple(map, "minecraft:zombified_piglin");
      return map;
   }
}
