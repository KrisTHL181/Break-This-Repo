package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.SequencedMap;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V4307 extends NamespacedSchema {
   public V4307(final int versionKey, final Schema parent) {
      super(versionKey, parent);
   }

   public static SequencedMap<String, Supplier<TypeTemplate>> components(final Schema schema) {
      SequencedMap<String, Supplier<TypeTemplate>> components = V4059.components(schema);
      components.put("minecraft:can_place_on", () -> adventureModePredicate(schema));
      components.put("minecraft:can_break", () -> adventureModePredicate(schema));
      return components;
   }

   private static TypeTemplate adventureModePredicate(final Schema schema) {
      TypeTemplate predicate = DSL.optionalFields("blocks", DSL.or(References.BLOCK_NAME.in(schema), DSL.list(References.BLOCK_NAME.in(schema))));
      return DSL.or(predicate, DSL.list(predicate));
   }

   public void registerTypes(
      final Schema schema, final Map<String, Supplier<TypeTemplate>> entityTypes, final Map<String, Supplier<TypeTemplate>> blockEntityTypes
   ) {
      super.registerTypes(schema, entityTypes, blockEntityTypes);
      schema.registerType(true, References.DATA_COMPONENTS, () -> DSL.optionalFieldsLazy(components(schema)));
   }
}
