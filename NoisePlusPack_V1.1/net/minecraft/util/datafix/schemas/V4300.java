package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V4300 extends NamespacedSchema {
   public V4300(int p_391864_, Schema p_395992_) {
      super(p_391864_, p_395992_);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_397315_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_397315_);
      p_397315_.register(map, "minecraft:llama", p_394953_ -> entityWithInventory(p_397315_));
      p_397315_.register(map, "minecraft:trader_llama", p_397421_ -> entityWithInventory(p_397315_));
      p_397315_.register(map, "minecraft:donkey", p_391407_ -> entityWithInventory(p_397315_));
      p_397315_.register(map, "minecraft:mule", p_392509_ -> entityWithInventory(p_397315_));
      p_397315_.registerSimple(map, "minecraft:horse");
      p_397315_.registerSimple(map, "minecraft:skeleton_horse");
      p_397315_.registerSimple(map, "minecraft:zombie_horse");
      return map;
   }

   private static TypeTemplate entityWithInventory(Schema p_393122_) {
      return DSL.optionalFields("Items", DSL.list(References.ITEM_STACK.in(p_393122_)));
   }
}
