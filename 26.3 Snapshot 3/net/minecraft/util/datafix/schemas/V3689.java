package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V3689 extends NamespacedSchema {
   public V3689(final int versionKey, final Schema parent) {
      super(versionKey, parent);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(final Schema schema) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(schema);
      schema.registerSimple(map, "minecraft:breeze");
      schema.registerSimple(map, "minecraft:wind_charge");
      schema.registerSimple(map, "minecraft:breeze_wind_charge");
      return map;
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(final Schema schema) {
      Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(schema);
      schema.register(
         map,
         "minecraft:trial_spawner",
         () -> DSL.optionalFields(
            "spawn_potentials",
            DSL.list(DSL.fields("data", DSL.fields("entity", References.ENTITY_TREE.in(schema)))),
            "spawn_data",
            DSL.fields("entity", References.ENTITY_TREE.in(schema))
         )
      );
      return map;
   }
}
