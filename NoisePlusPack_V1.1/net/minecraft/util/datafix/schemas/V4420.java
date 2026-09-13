package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V4420 extends NamespacedSchema {
   public V4420(int p_410265_, Schema p_410480_) {
      super(p_410265_, p_410480_);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_406237_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_406237_);
      p_406237_.register(map, "minecraft:area_effect_cloud", p_406580_ -> DSL.optionalFields("custom_particle", References.PARTICLE.in(p_406237_)));
      return map;
   }
}
