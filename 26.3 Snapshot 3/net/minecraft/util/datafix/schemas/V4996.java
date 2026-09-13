package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.SequencedMap;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V4996 extends NamespacedSchema {
   public V4996(final int versionKey, final Schema parent) {
      super(versionKey, parent);
   }

   public static SequencedMap<String, Supplier<TypeTemplate>> components(final Schema schema) {
      SequencedMap<String, Supplier<TypeTemplate>> components = V4059.components(schema);
      components.put(
         "minecraft:pot_decorations",
         () -> DSL.optionalFields(
            "back",
            References.ITEM_STACK.in(schema),
            "left",
            References.ITEM_STACK.in(schema),
            "right",
            References.ITEM_STACK.in(schema),
            "front",
            References.ITEM_STACK.in(schema)
         )
      );
      return components;
   }

   public void registerTypes(
      final Schema schema, final Map<String, Supplier<TypeTemplate>> entityTypes, final Map<String, Supplier<TypeTemplate>> blockEntityTypes
   ) {
      super.registerTypes(schema, entityTypes, blockEntityTypes);
      schema.registerType(true, References.DATA_COMPONENTS, () -> DSL.optionalFieldsLazy(components(schema)));
   }
}
