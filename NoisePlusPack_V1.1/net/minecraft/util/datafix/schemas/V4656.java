package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V4656 extends NamespacedSchema {
   public V4656(int p_457115_, Schema p_451911_) {
      super(p_457115_, p_451911_);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_453866_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_453866_);
      p_453866_.registerSimple(map, "minecraft:camel_husk");
      p_453866_.registerSimple(map, "minecraft:parched");
      return map;
   }
}
