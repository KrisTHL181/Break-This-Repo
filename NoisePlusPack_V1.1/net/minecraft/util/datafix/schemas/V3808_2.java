package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V3808_2 extends NamespacedSchema {
   public V3808_2(int p_336421_, Schema p_336420_) {
      super(p_336421_, p_336420_);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_336416_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_336416_);
      p_336416_.register(
         map,
         "minecraft:trader_llama",
         p_390398_ -> DSL.optionalFields("Items", DSL.list(References.ITEM_STACK.in(p_336416_)), "SaddleItem", References.ITEM_STACK.in(p_336416_))
      );
      return map;
   }
}
