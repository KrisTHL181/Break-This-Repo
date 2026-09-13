package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V4648 extends NamespacedSchema {
   public V4648(int p_460025_, Schema p_458240_) {
      super(p_460025_, p_458240_);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_455417_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_455417_);
      p_455417_.registerSimple(map, "minecraft:nautilus");
      p_455417_.registerSimple(map, "minecraft:zombie_nautilus");
      return map;
   }
}
