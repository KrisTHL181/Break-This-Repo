package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V4533 extends NamespacedSchema {
   public V4533(int p_424337_, Schema p_429888_) {
      super(p_424337_, p_429888_);
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema p_427553_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(p_427553_);
      p_427553_.register(map, "minecraft:shelf", () -> DSL.optionalFields("Items", DSL.list(References.ITEM_STACK.in(p_427553_))));
      return map;
   }
}
