package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V2688 extends NamespacedSchema {
   public V2688(int p_145872_, Schema p_145873_) {
      super(p_145872_, p_145873_);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_145881_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_145881_);
      p_145881_.registerSimple(map, "minecraft:glow_squid");
      p_145881_.register(map, "minecraft:glow_item_frame", p_264877_ -> DSL.optionalFields("Item", References.ITEM_STACK.in(p_145881_)));
      return map;
   }
}
