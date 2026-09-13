package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V1458 extends NamespacedSchema {
   public V1458(int p_394988_, Schema p_397437_) {
      super(p_394988_, p_397437_);
   }

   public void registerTypes(Schema p_397410_, Map<String, Supplier<TypeTemplate>> p_392054_, Map<String, Supplier<TypeTemplate>> p_395698_) {
      super.registerTypes(p_397410_, p_392054_, p_395698_);
      p_397410_.registerType(
         true,
         References.ENTITY,
         () -> DSL.and(
            References.ENTITY_EQUIPMENT.in(p_397410_),
            DSL.optionalFields("CustomName", References.TEXT_COMPONENT.in(p_397410_), DSL.taggedChoiceLazy("id", namespacedString(), p_392054_))
         )
      );
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema p_397691_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(p_397691_);
      p_397691_.register(map, "minecraft:beacon", () -> nameable(p_397691_));
      p_397691_.register(map, "minecraft:banner", () -> nameable(p_397691_));
      p_397691_.register(map, "minecraft:brewing_stand", () -> nameableInventory(p_397691_));
      p_397691_.register(map, "minecraft:chest", () -> nameableInventory(p_397691_));
      p_397691_.register(map, "minecraft:trapped_chest", () -> nameableInventory(p_397691_));
      p_397691_.register(map, "minecraft:dispenser", () -> nameableInventory(p_397691_));
      p_397691_.register(map, "minecraft:dropper", () -> nameableInventory(p_397691_));
      p_397691_.register(map, "minecraft:enchanting_table", () -> nameable(p_397691_));
      p_397691_.register(map, "minecraft:furnace", () -> nameableInventory(p_397691_));
      p_397691_.register(map, "minecraft:hopper", () -> nameableInventory(p_397691_));
      p_397691_.register(map, "minecraft:shulker_box", () -> nameableInventory(p_397691_));
      return map;
   }

   public static TypeTemplate nameableInventory(Schema p_393293_) {
      return DSL.optionalFields("Items", DSL.list(References.ITEM_STACK.in(p_393293_)), "CustomName", References.TEXT_COMPONENT.in(p_393293_));
   }

   public static TypeTemplate nameable(Schema p_397223_) {
      return DSL.optionalFields("CustomName", References.TEXT_COMPONENT.in(p_397223_));
   }
}
