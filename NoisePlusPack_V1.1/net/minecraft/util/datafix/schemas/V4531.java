package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V4531 extends NamespacedSchema {
   public V4531(int p_424642_, Schema p_430170_) {
      super(p_424642_, p_430170_);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_427442_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_427442_);
      p_427442_.registerSimple(map, "minecraft:copper_golem");
      return map;
   }
}
