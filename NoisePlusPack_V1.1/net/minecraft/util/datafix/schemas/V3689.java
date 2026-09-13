package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V3689 extends NamespacedSchema {
   public V3689(int p_312091_, Schema p_310599_) {
      super(p_312091_, p_310599_);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_310273_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_310273_);
      p_310273_.registerSimple(map, "minecraft:breeze");
      p_310273_.registerSimple(map, "minecraft:wind_charge");
      p_310273_.registerSimple(map, "minecraft:breeze_wind_charge");
      return map;
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema p_309732_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(p_309732_);
      p_309732_.register(
         map,
         "minecraft:trial_spawner",
         () -> DSL.optionalFields(
            "spawn_potentials",
            DSL.list(DSL.fields("data", DSL.fields("entity", References.ENTITY_TREE.in(p_309732_)))),
            "spawn_data",
            DSL.fields("entity", References.ENTITY_TREE.in(p_309732_))
         )
      );
      return map;
   }
}
