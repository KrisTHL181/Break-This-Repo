package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V3683 extends NamespacedSchema {
   public V3683(int p_310878_, Schema p_312352_) {
      super(p_310878_, p_312352_);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_312423_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_312423_);
      p_312423_.register(map, "minecraft:tnt", () -> DSL.optionalFields("block_state", References.BLOCK_STATE.in(p_312423_)));
      return map;
   }
}
