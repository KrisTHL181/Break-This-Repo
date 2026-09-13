package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V3082 extends NamespacedSchema {
   public V3082(int p_216797_, Schema p_216798_) {
      super(p_216797_, p_216798_);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_216803_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_216803_);
      p_216803_.register(map, "minecraft:chest_boat", p_216801_ -> DSL.optionalFields("Items", DSL.list(References.ITEM_STACK.in(p_216803_))));
      return map;
   }
}
