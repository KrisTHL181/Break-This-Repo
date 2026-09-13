package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V2522 extends NamespacedSchema {
   public V2522(int p_17933_, Schema p_17934_) {
      super(p_17933_, p_17934_);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_17942_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_17942_);
      p_17942_.registerSimple(map, "minecraft:zoglin");
      return map;
   }
}
