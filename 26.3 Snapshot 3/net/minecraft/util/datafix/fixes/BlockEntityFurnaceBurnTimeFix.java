package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class BlockEntityFurnaceBurnTimeFix extends NamedEntityFix {
   public BlockEntityFurnaceBurnTimeFix(final Schema outputSchema, final String entityType) {
      super(outputSchema, false, "BlockEntityFurnaceBurnTimeFix" + entityType, References.BLOCK_ENTITY, entityType);
   }

   public Dynamic<?> fixBurnTime(Dynamic<?> data) {
      data = data.renameField("CookTime", "cooking_time_spent");
      data = data.renameField("CookTimeTotal", "cooking_total_time");
      data = data.renameField("BurnTime", "lit_time_remaining");
      return data.setFieldIfPresent("lit_total_time", data.get("lit_time_remaining").result());
   }

   @Override
   protected Typed<?> fix(final Typed<?> entity) {
      return entity.update(DSL.remainderFinder(), this::fixBurnTime);
   }
}
