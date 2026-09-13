package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V3327 extends NamespacedSchema {
   public V3327(int p_273350_, Schema p_273379_) {
      super(p_273350_, p_273379_);
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema p_273248_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(p_273248_);
      p_273248_.register(
         map,
         "minecraft:decorated_pot",
         () -> DSL.optionalFields("shards", DSL.list(References.ITEM_NAME.in(p_273248_)), "item", References.ITEM_STACK.in(p_273248_))
      );
      p_273248_.register(map, "minecraft:suspicious_sand", () -> DSL.optionalFields("item", References.ITEM_STACK.in(p_273248_)));
      return map;
   }
}
