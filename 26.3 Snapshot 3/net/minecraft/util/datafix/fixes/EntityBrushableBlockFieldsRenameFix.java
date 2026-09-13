package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class EntityBrushableBlockFieldsRenameFix extends NamedEntityFix {
   public EntityBrushableBlockFieldsRenameFix(final Schema outputSchema) {
      super(outputSchema, false, "EntityBrushableBlockFieldsRenameFix", References.BLOCK_ENTITY, "minecraft:brushable_block");
   }

   public Dynamic<?> fixTag(final Dynamic<?> input) {
      return input.renameField("loot_table", "LootTable").renameField("loot_table_seed", "LootTableSeed");
   }

   @Override
   protected Typed<?> fix(final Typed<?> entity) {
      return entity.update(DSL.remainderFinder(), this::fixTag);
   }
}
