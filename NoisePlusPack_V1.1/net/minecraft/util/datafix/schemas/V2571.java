package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V2571 extends NamespacedSchema {
   public V2571(int p_145845_, Schema p_145846_) {
      super(p_145845_, p_145846_);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_145854_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_145854_);
      p_145854_.registerSimple(map, "minecraft:goat");
      return map;
   }
}
