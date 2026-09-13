package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.SequencedMap;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V4059 extends NamespacedSchema {
   public V4059(int p_365559_, Schema p_368040_) {
      super(p_365559_, p_368040_);
   }

   public static SequencedMap<String, Supplier<TypeTemplate>> components(Schema p_363212_) {
      SequencedMap<String, Supplier<TypeTemplate>> sequencedmap = V3818_3.components(p_363212_);
      sequencedmap.remove("minecraft:food");
      sequencedmap.put("minecraft:use_remainder", () -> References.ITEM_STACK.in(p_363212_));
      sequencedmap.put(
         "minecraft:equippable",
         () -> DSL.optionalFields("allowed_entities", DSL.or(References.ENTITY_NAME.in(p_363212_), DSL.list(References.ENTITY_NAME.in(p_363212_))))
      );
      return sequencedmap;
   }

   public void registerTypes(Schema p_361152_, Map<String, Supplier<TypeTemplate>> p_368342_, Map<String, Supplier<TypeTemplate>> p_363758_) {
      super.registerTypes(p_361152_, p_368342_, p_363758_);
      p_361152_.registerType(true, References.DATA_COMPONENTS, () -> DSL.optionalFieldsLazy(components(p_361152_)));
   }
}
