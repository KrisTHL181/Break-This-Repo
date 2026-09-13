package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V3818_5 extends NamespacedSchema {
   public V3818_5(int p_335884_, Schema p_328072_) {
      super(p_335884_, p_328072_);
   }

   public void registerTypes(Schema p_328796_, Map<String, Supplier<TypeTemplate>> p_329307_, Map<String, Supplier<TypeTemplate>> p_331445_) {
      super.registerTypes(p_328796_, p_329307_, p_331445_);
      p_328796_.registerType(
         true,
         References.ITEM_STACK,
         () -> DSL.optionalFields("id", References.ITEM_NAME.in(p_328796_), "components", References.DATA_COMPONENTS.in(p_328796_))
      );
   }
}
