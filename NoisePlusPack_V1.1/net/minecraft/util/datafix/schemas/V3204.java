package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V3204 extends NamespacedSchema {
   public V3204(int p_250011_, Schema p_250175_) {
      super(p_250011_, p_250175_);
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema p_250991_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(p_250991_);
      p_250991_.register(map, "minecraft:chiseled_bookshelf", () -> DSL.optionalFields("Items", DSL.list(References.ITEM_STACK.in(p_250991_))));
      return map;
   }
}
