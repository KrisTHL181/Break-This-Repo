package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.function.UnaryOperator;

public class BlockPropertyRenameAndFix extends AbstractBlockPropertyFix {
   private final String blockId;
   private final String oldPropertyName;
   private final String newPropertyName;
   private final UnaryOperator<String> valueFixer;

   public BlockPropertyRenameAndFix(Schema p_394707_, String p_391943_, String p_392530_, String p_391480_, String p_392149_, UnaryOperator<String> p_395499_) {
      super(p_394707_, p_391943_);
      this.blockId = p_392530_;
      this.oldPropertyName = p_391480_;
      this.newPropertyName = p_392149_;
      this.valueFixer = p_395499_;
   }

   @Override
   protected boolean shouldFix(String p_397317_) {
      return p_397317_.equals(this.blockId);
   }

   @Override
   protected <T> Dynamic<T> fixProperties(String p_396643_, Dynamic<T> p_397950_) {
      return p_397950_.renameAndFixField(
         this.oldPropertyName, this.newPropertyName, p_392508_ -> p_392508_.createString(this.valueFixer.apply(p_392508_.asString("")))
      );
   }
}
