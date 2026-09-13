package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.function.Predicate;

public abstract class ItemStackTagRemainderFix extends ItemStackTagFix {
   public ItemStackTagRemainderFix(final Schema outputSchema, final String name, final Predicate<String> idFilter) {
      super(outputSchema, name, idFilter);
   }

   protected abstract <T> Dynamic<T> fixItemStackTag(Dynamic<T> tag);

   @Override
   protected final Typed<?> fixItemStackTag(final Typed<?> tag) {
      return tag.update(DSL.remainderFinder(), this::fixItemStackTag);
   }
}
