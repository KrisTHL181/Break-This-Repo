package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;

public abstract class SimpleEntityRenameFix extends EntityRenameFix {
   public SimpleEntityRenameFix(final String name, final Schema outputSchema, final boolean changesType) {
      super(name, outputSchema, changesType);
   }

   @Override
   protected Pair<String, Typed<?>> fix(final String name, final Typed<?> entity) {
      Pair<String, Dynamic<?>> pair = this.getNewNameAndTag(name, (Dynamic<?>)entity.getOrCreate(DSL.remainderFinder()));
      return Pair.of((String)pair.getFirst(), entity.set(DSL.remainderFinder(), (Dynamic)pair.getSecond()));
   }

   protected abstract Pair<String, Dynamic<?>> getNewNameAndTag(final String name, final Dynamic<?> tag);
}
