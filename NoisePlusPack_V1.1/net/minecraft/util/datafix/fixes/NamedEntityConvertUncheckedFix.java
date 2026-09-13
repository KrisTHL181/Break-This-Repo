package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.Typed;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import net.minecraft.util.datafix.ExtraDataFixUtils;

public class NamedEntityConvertUncheckedFix extends NamedEntityFix {
   public NamedEntityConvertUncheckedFix(Schema p_396819_, String p_394386_, TypeReference p_392880_, String p_391998_) {
      super(p_396819_, true, p_394386_, p_392880_, p_391998_);
   }

   @Override
   protected Typed<?> fix(Typed<?> p_391723_) {
      Type<?> type = this.getOutputSchema().getChoiceType(this.type, this.entityName);
      return ExtraDataFixUtils.cast(type, p_391723_);
   }
}
