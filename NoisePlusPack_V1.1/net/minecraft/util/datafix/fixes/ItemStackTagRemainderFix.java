package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.function.Predicate;

public abstract class ItemStackTagRemainderFix extends ItemStackTagFix {
   public ItemStackTagRemainderFix(Schema p_391517_, String p_394498_, Predicate<String> p_393634_) {
      super(p_391517_, p_394498_, p_393634_);
   }

   protected abstract <T> Dynamic<T> fixItemStackTag(Dynamic<T> var1);

   @Override
   protected final Typed<?> fixItemStackTag(Typed<?> p_394400_) {
      return p_394400_.update(DSL.remainderFinder(), this::fixItemStackTag);
   }
}
