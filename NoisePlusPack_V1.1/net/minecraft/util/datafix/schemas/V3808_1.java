package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V3808_1 extends NamespacedSchema {
   public V3808_1(int p_335376_, Schema p_331497_) {
      super(p_335376_, p_331497_);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_334308_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_334308_);
      p_334308_.register(
         map,
         "minecraft:llama",
         p_390396_ -> DSL.optionalFields("Items", DSL.list(References.ITEM_STACK.in(p_334308_)), "SaddleItem", References.ITEM_STACK.in(p_334308_))
      );
      return map;
   }
}
