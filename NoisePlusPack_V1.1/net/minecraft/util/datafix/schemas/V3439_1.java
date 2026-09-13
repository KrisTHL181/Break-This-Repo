package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V3439_1 extends NamespacedSchema {
   public V3439_1(int p_410186_, Schema p_407808_) {
      super(p_410186_, p_407808_);
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema p_406936_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(p_406936_);
      this.register(map, "minecraft:hanging_sign", () -> V3439.sign(p_406936_));
      return map;
   }
}
