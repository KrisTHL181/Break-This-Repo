package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V2511_1 extends NamespacedSchema {
   public V2511_1(final int versionKey, final Schema parent) {
      super(versionKey, parent);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(final Schema schema) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(schema);
      schema.register(map, "minecraft:potion", name -> DSL.optionalFields("Item", References.ITEM_STACK.in(schema)));
      return map;
   }
}
