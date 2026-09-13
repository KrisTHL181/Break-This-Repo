package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V4543 extends NamespacedSchema {
   public V4543(int p_430630_, Schema p_428511_) {
      super(p_430630_, p_428511_);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_431064_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_431064_);
      p_431064_.registerSimple(map, "minecraft:mannequin");
      return map;
   }
}
