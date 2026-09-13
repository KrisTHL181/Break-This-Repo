package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V4306 extends NamespacedSchema {
   public V4306(int p_392365_, Schema p_394985_) {
      super(p_392365_, p_394985_);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_393900_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_393900_);
      map.remove("minecraft:potion");
      p_393900_.register(map, "minecraft:splash_potion", () -> DSL.optionalFields("Item", References.ITEM_STACK.in(p_393900_)));
      p_393900_.register(map, "minecraft:lingering_potion", () -> DSL.optionalFields("Item", References.ITEM_STACK.in(p_393900_)));
      return map;
   }
}
