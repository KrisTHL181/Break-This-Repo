package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V4421 extends NamespacedSchema {
   public V4421(int p_408892_, Schema p_406464_) {
      super(p_408892_, p_406464_);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_406727_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_406727_);
      p_406727_.registerSimple(map, "minecraft:happy_ghast");
      return map;
   }
}
