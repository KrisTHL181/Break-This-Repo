package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V1909 extends NamespacedSchema {
   public V1909(int p_17782_, Schema p_17783_) {
      super(p_17782_, p_17783_);
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema p_17785_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(p_17785_);
      p_17785_.register(map, "minecraft:jigsaw", () -> DSL.optionalFields("final_state", References.FLAT_BLOCK_STATE.in(p_17785_)));
      return map;
   }
}
