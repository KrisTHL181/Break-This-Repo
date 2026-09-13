package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedMap;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V3818_3 extends NamespacedSchema {
   public V3818_3(int p_333453_, Schema p_330765_) {
      super(p_333453_, p_330765_);
   }

   public static SequencedMap<String, Supplier<TypeTemplate>> components(Schema p_361272_) {
      SequencedMap<String, Supplier<TypeTemplate>> sequencedmap = new LinkedHashMap<>();
      sequencedmap.put("minecraft:bees", () -> DSL.list(DSL.optionalFields("entity_data", References.ENTITY_TREE.in(p_361272_))));
      sequencedmap.put("minecraft:block_entity_data", () -> References.BLOCK_ENTITY.in(p_361272_));
      sequencedmap.put("minecraft:bundle_contents", () -> DSL.list(References.ITEM_STACK.in(p_361272_)));
      sequencedmap.put(
         "minecraft:can_break",
         () -> DSL.optionalFields(
            "predicates", DSL.list(DSL.optionalFields("blocks", DSL.or(References.BLOCK_NAME.in(p_361272_), DSL.list(References.BLOCK_NAME.in(p_361272_)))))
         )
      );
      sequencedmap.put(
         "minecraft:can_place_on",
         () -> DSL.optionalFields(
            "predicates", DSL.list(DSL.optionalFields("blocks", DSL.or(References.BLOCK_NAME.in(p_361272_), DSL.list(References.BLOCK_NAME.in(p_361272_)))))
         )
      );
      sequencedmap.put("minecraft:charged_projectiles", () -> DSL.list(References.ITEM_STACK.in(p_361272_)));
      sequencedmap.put("minecraft:container", () -> DSL.list(DSL.optionalFields("item", References.ITEM_STACK.in(p_361272_))));
      sequencedmap.put("minecraft:entity_data", () -> References.ENTITY_TREE.in(p_361272_));
      sequencedmap.put("minecraft:pot_decorations", () -> DSL.list(References.ITEM_NAME.in(p_361272_)));
      sequencedmap.put("minecraft:food", () -> DSL.optionalFields("using_converts_to", References.ITEM_STACK.in(p_361272_)));
      sequencedmap.put("minecraft:custom_name", () -> References.TEXT_COMPONENT.in(p_361272_));
      sequencedmap.put("minecraft:item_name", () -> References.TEXT_COMPONENT.in(p_361272_));
      sequencedmap.put("minecraft:lore", () -> DSL.list(References.TEXT_COMPONENT.in(p_361272_)));
      sequencedmap.put(
         "minecraft:written_book_content",
         () -> DSL.optionalFields(
            "pages",
            DSL.list(
               DSL.or(
                  DSL.optionalFields("raw", References.TEXT_COMPONENT.in(p_361272_), "filtered", References.TEXT_COMPONENT.in(p_361272_)),
                  References.TEXT_COMPONENT.in(p_361272_)
               )
            )
         )
      );
      return sequencedmap;
   }

   public void registerTypes(Schema p_332951_, Map<String, Supplier<TypeTemplate>> p_332616_, Map<String, Supplier<TypeTemplate>> p_331121_) {
      super.registerTypes(p_332951_, p_332616_, p_331121_);
      p_332951_.registerType(true, References.DATA_COMPONENTS, () -> DSL.optionalFieldsLazy(components(p_332951_)));
   }
}
