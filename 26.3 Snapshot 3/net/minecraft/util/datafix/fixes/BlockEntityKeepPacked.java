package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class BlockEntityKeepPacked extends NamedEntityFix {
   public BlockEntityKeepPacked(final Schema schema, final boolean changesType) {
      super(schema, changesType, "BlockEntityKeepPacked", References.BLOCK_ENTITY, "DUMMY");
   }

   private static Dynamic<?> fixTag(final Dynamic<?> tag) {
      return tag.set("keepPacked", tag.createBoolean(true));
   }

   @Override
   protected Typed<?> fix(final Typed<?> entity) {
      return entity.update(DSL.remainderFinder(), BlockEntityKeepPacked::fixTag);
   }
}
