package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;

public class InvalidBlockEntityLockFix extends DataFix {
   public InvalidBlockEntityLockFix(final Schema outputSchema) {
      super(outputSchema, false);
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "BlockEntityLockToComponentFix",
         this.getInputSchema().getType(References.BLOCK_ENTITY),
         blockEntity -> blockEntity.update(DSL.remainderFinder(), remainder -> {
            Optional<? extends Dynamic<?>> lock = remainder.get("lock").result();
            if (lock.isEmpty()) {
               return remainder;
            }

            Dynamic<?> newLock = InvalidLockComponentFix.fixLock(lock.get());
            return newLock != null ? remainder.set("lock", newLock) : remainder.remove("lock");
         })
      );
   }
}
