package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import net.minecraft.util.Util;

public class FilteredBooksFix extends ItemStackTagFix {
   public FilteredBooksFix(final Schema outputSchema) {
      super(outputSchema, "Remove filtered text from books", id -> id.equals("minecraft:writable_book") || id.equals("minecraft:written_book"));
   }

   @Override
   protected Typed<?> fixItemStackTag(final Typed<?> tag) {
      return Util.writeAndReadTypedOrThrow(tag, tag.getType(), dynamic -> dynamic.remove("filtered_title").remove("filtered_pages"));
   }
}
