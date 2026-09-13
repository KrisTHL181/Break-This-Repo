package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V1470 extends NamespacedSchema {
   public V1470(final int versionKey, final Schema parent) {
      super(versionKey, parent);
   }

   protected static void registerMob(final Schema schema, final Map<String, Supplier<TypeTemplate>> map, final String name) {
      schema.registerSimple(map, name);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(final Schema schema) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(schema);
      registerMob(schema, map, "minecraft:turtle");
      registerMob(schema, map, "minecraft:cod_mob");
      registerMob(schema, map, "minecraft:tropical_fish");
      registerMob(schema, map, "minecraft:salmon_mob");
      registerMob(schema, map, "minecraft:puffer_fish");
      registerMob(schema, map, "minecraft:phantom");
      registerMob(schema, map, "minecraft:dolphin");
      registerMob(schema, map, "minecraft:drowned");
      schema.register(
         map, "minecraft:trident", name -> DSL.optionalFields("inBlockState", References.BLOCK_STATE.in(schema), "Trident", References.ITEM_STACK.in(schema))
      );
      return map;
   }
}
