package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.datafixers.util.Pair;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V4301 extends NamespacedSchema {
   public V4301(final int versionKey, final Schema parent) {
      super(versionKey, parent);
   }

   public void registerTypes(
      final Schema schema, final Map<String, Supplier<TypeTemplate>> entityTypes, final Map<String, Supplier<TypeTemplate>> blockEntityTypes
   ) {
      super.registerTypes(schema, entityTypes, blockEntityTypes);
      schema.registerType(
         true,
         References.ENTITY_EQUIPMENT,
         () -> DSL.optional(
            DSL.field(
               "equipment",
               DSL.optionalFields(
                  new Pair[]{
                     Pair.of("mainhand", References.ITEM_STACK.in(schema)),
                     Pair.of("offhand", References.ITEM_STACK.in(schema)),
                     Pair.of("feet", References.ITEM_STACK.in(schema)),
                     Pair.of("legs", References.ITEM_STACK.in(schema)),
                     Pair.of("chest", References.ITEM_STACK.in(schema)),
                     Pair.of("head", References.ITEM_STACK.in(schema)),
                     Pair.of("body", References.ITEM_STACK.in(schema)),
                     Pair.of("saddle", References.ITEM_STACK.in(schema))
                  }
               )
            )
         )
      );
   }
}
