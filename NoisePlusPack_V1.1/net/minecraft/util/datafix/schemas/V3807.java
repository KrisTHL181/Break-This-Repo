package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V3807 extends NamespacedSchema {
   public V3807(int p_329422_, Schema p_333525_) {
      super(p_329422_, p_333525_);
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema p_328421_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(p_328421_);
      p_328421_.register(
         map,
         "minecraft:vault",
         () -> DSL.optionalFields(
            "config",
            DSL.optionalFields("key_item", References.ITEM_STACK.in(p_328421_)),
            "server_data",
            DSL.optionalFields("items_to_eject", DSL.list(References.ITEM_STACK.in(p_328421_))),
            "shared_data",
            DSL.optionalFields("display_item", References.ITEM_STACK.in(p_328421_))
         )
      );
      return map;
   }
}
