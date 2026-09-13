package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V2505 extends NamespacedSchema {
   public V2505(int p_17870_, Schema p_17871_) {
      super(p_17870_, p_17871_);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_17879_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_17879_);
      p_17879_.register(map, "minecraft:piglin", () -> DSL.optionalFields("Inventory", DSL.list(References.ITEM_STACK.in(p_17879_))));
      return map;
   }
}
