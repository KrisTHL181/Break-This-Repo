package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V3438 extends NamespacedSchema {
   public V3438(int p_277419_, Schema p_277767_) {
      super(p_277419_, p_277767_);
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema p_277667_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(p_277667_);
      map.put("minecraft:brushable_block", map.remove("minecraft:suspicious_sand"));
      p_277667_.registerSimple(map, "minecraft:calibrated_sculk_sensor");
      return map;
   }
}
