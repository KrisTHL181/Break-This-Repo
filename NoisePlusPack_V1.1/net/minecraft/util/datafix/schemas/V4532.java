package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V4532 extends NamespacedSchema {
   public V4532(int p_424726_, Schema p_430034_) {
      super(p_424726_, p_430034_);
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema p_430117_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(p_430117_);
      p_430117_.registerSimple(map, "minecraft:copper_golem_statue");
      return map;
   }
}
