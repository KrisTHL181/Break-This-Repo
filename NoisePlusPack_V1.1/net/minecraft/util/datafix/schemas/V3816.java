package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V3816 extends NamespacedSchema {
   public V3816(int p_331584_, Schema p_327914_) {
      super(p_331584_, p_327914_);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_335343_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_335343_);
      p_335343_.registerSimple(map, "minecraft:bogged");
      return map;
   }
}
