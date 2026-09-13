package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V2686 extends NamespacedSchema {
   public V2686(int p_145861_, Schema p_145862_) {
      super(p_145861_, p_145862_);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_145870_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_145870_);
      p_145870_.registerSimple(map, "minecraft:axolotl");
      return map;
   }
}
