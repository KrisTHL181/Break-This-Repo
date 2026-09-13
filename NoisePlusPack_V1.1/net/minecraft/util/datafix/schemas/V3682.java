package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V3682 extends NamespacedSchema {
   public V3682(int p_311189_, Schema p_309665_) {
      super(p_311189_, p_309665_);
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema p_309752_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(p_309752_);
      p_309752_.register(map, "minecraft:crafter", () -> V1458.nameableInventory(p_309752_));
      return map;
   }
}
