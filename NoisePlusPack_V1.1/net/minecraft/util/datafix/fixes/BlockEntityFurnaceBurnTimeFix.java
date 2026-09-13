package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class BlockEntityFurnaceBurnTimeFix extends NamedEntityFix {
   public BlockEntityFurnaceBurnTimeFix(Schema p_377710_, String p_378570_) {
      super(p_377710_, false, "BlockEntityFurnaceBurnTimeFix" + p_378570_, References.BLOCK_ENTITY, p_378570_);
   }

   public Dynamic<?> fixBurnTime(Dynamic<?> p_376491_) {
      p_376491_ = p_376491_.renameField("CookTime", "cooking_time_spent");
      p_376491_ = p_376491_.renameField("CookTimeTotal", "cooking_total_time");
      p_376491_ = p_376491_.renameField("BurnTime", "lit_time_remaining");
      return p_376491_.setFieldIfPresent("lit_total_time", p_376491_.get("lit_time_remaining").result());
   }

   @Override
   protected Typed<?> fix(Typed<?> p_376974_) {
      return p_376974_.update(DSL.remainderFinder(), this::fixBurnTime);
   }
}
