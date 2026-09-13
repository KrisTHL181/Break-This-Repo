package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V100 extends Schema {
   public V100(int p_17328_, Schema p_17329_) {
      super(p_17328_, p_17329_);
   }

   public void registerTypes(Schema p_17352_, Map<String, Supplier<TypeTemplate>> p_17353_, Map<String, Supplier<TypeTemplate>> p_17354_) {
      super.registerTypes(p_17352_, p_17353_, p_17354_);
      p_17352_.registerType(
         true,
         References.ENTITY_EQUIPMENT,
         () -> DSL.and(
            DSL.optional(DSL.field("ArmorItems", DSL.list(References.ITEM_STACK.in(p_17352_)))),
            new TypeTemplate[]{
               DSL.optional(DSL.field("HandItems", DSL.list(References.ITEM_STACK.in(p_17352_)))),
               DSL.optional(DSL.field("body_armor_item", References.ITEM_STACK.in(p_17352_))),
               DSL.optional(DSL.field("saddle", References.ITEM_STACK.in(p_17352_)))
            }
         )
      );
   }
}
