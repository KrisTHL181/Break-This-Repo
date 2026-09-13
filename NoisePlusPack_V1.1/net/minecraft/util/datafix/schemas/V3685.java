package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V3685 extends NamespacedSchema {
   public V3685(int p_311526_, Schema p_311989_) {
      super(p_311526_, p_311989_);
   }

   protected static TypeTemplate abstractArrow(Schema p_309540_) {
      return DSL.optionalFields("inBlockState", References.BLOCK_STATE.in(p_309540_), "item", References.ITEM_STACK.in(p_309540_));
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_312739_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_312739_);
      p_312739_.register(map, "minecraft:trident", () -> abstractArrow(p_312739_));
      p_312739_.register(map, "minecraft:spectral_arrow", () -> abstractArrow(p_312739_));
      p_312739_.register(map, "minecraft:arrow", () -> abstractArrow(p_312739_));
      return map;
   }
}
