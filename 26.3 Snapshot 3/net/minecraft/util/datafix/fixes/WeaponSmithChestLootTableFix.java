package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;

public class WeaponSmithChestLootTableFix extends NamedEntityFix {
   public WeaponSmithChestLootTableFix(final Schema outputSchema, final boolean changesType) {
      super(outputSchema, changesType, "WeaponSmithChestLootTableFix", References.BLOCK_ENTITY, "minecraft:chest");
   }

   @Override
   protected Typed<?> fix(final Typed<?> entity) {
      return entity.update(
         DSL.remainderFinder(),
         tag -> {
            String lootTable = tag.get("LootTable").asString("");
            return lootTable.equals("minecraft:chests/village_blacksmith")
               ? tag.set("LootTable", tag.createString("minecraft:chests/village/village_weaponsmith"))
               : tag;
         }
      );
   }
}
