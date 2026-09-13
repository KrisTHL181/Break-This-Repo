package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V2511_1 extends NamespacedSchema {
   public V2511_1(int p_397516_, Schema p_391861_) {
      super(p_397516_, p_391861_);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_396461_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_396461_);
      p_396461_.register(map, "minecraft:potion", p_396761_ -> DSL.optionalFields("Item", References.ITEM_STACK.in(p_396461_)));
      return map;
   }
}
