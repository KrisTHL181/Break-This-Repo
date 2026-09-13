package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class FilteredSignsFix extends NamedEntityWriteReadFix {
   public FilteredSignsFix(Schema p_216666_) {
      super(p_216666_, false, "Remove filtered text from signs", References.BLOCK_ENTITY, "minecraft:sign");
   }

   @Override
   protected <T> Dynamic<T> fix(Dynamic<T> p_395376_) {
      return p_395376_.remove("FilteredText1").remove("FilteredText2").remove("FilteredText3").remove("FilteredText4");
   }
}
