package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V1800 extends NamespacedSchema {
   public V1800(int p_17732_, Schema p_17733_) {
      super(p_17732_, p_17733_);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_17744_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_17744_);
      p_17744_.registerSimple(map, "minecraft:panda");
      p_17744_.register(map, "minecraft:pillager", p_390381_ -> DSL.optionalFields("Inventory", DSL.list(References.ITEM_STACK.in(p_17744_))));
      return map;
   }
}
