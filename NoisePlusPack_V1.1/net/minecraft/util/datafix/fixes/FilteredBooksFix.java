package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import net.minecraft.util.Util;

public class FilteredBooksFix extends ItemStackTagFix {
   public FilteredBooksFix(Schema p_216660_) {
      super(
         p_216660_, "Remove filtered text from books", p_216664_ -> p_216664_.equals("minecraft:writable_book") || p_216664_.equals("minecraft:written_book")
      );
   }

   @Override
   protected Typed<?> fixItemStackTag(Typed<?> p_397735_) {
      return Util.writeAndReadTypedOrThrow(p_397735_, p_397735_.getType(), p_390253_ -> p_390253_.remove("filtered_title").remove("filtered_pages"));
   }
}
