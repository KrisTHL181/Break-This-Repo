package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V3448 extends NamespacedSchema {
   public V3448(int p_282689_, Schema p_282757_) {
      super(p_282689_, p_282757_);
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema p_281949_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(p_281949_);
      p_281949_.register(
         map,
         "minecraft:decorated_pot",
         () -> DSL.optionalFields("sherds", DSL.list(References.ITEM_NAME.in(p_281949_)), "item", References.ITEM_STACK.in(p_281949_))
      );
      return map;
   }
}
