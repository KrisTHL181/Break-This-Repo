package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.datafixers.util.Pair;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V4301 extends NamespacedSchema {
   public V4301(int p_397475_, Schema p_394460_) {
      super(p_397475_, p_394460_);
   }

   public void registerTypes(Schema p_394238_, Map<String, Supplier<TypeTemplate>> p_394467_, Map<String, Supplier<TypeTemplate>> p_393395_) {
      super.registerTypes(p_394238_, p_394467_, p_393395_);
      p_394238_.registerType(
         true,
         References.ENTITY_EQUIPMENT,
         () -> DSL.optional(
            DSL.field(
               "equipment",
               DSL.optionalFields(
                  new Pair[]{
                     Pair.of("mainhand", References.ITEM_STACK.in(p_394238_)),
                     Pair.of("offhand", References.ITEM_STACK.in(p_394238_)),
                     Pair.of("feet", References.ITEM_STACK.in(p_394238_)),
                     Pair.of("legs", References.ITEM_STACK.in(p_394238_)),
                     Pair.of("chest", References.ITEM_STACK.in(p_394238_)),
                     Pair.of("head", References.ITEM_STACK.in(p_394238_)),
                     Pair.of("body", References.ITEM_STACK.in(p_394238_)),
                     Pair.of("saddle", References.ITEM_STACK.in(p_394238_))
                  }
               )
            )
         )
      );
   }
}
