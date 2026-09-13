package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V1904 extends NamespacedSchema {
   public V1904(int p_17757_, Schema p_17758_) {
      super(p_17757_, p_17758_);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_17766_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_17766_);
      p_17766_.registerSimple(map, "minecraft:cat");
      return map;
   }
}
