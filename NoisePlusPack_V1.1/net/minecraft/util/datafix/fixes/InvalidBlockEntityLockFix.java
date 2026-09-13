package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;

public class InvalidBlockEntityLockFix extends DataFix {
   public InvalidBlockEntityLockFix(Schema p_376685_) {
      super(p_376685_, false);
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "BlockEntityLockToComponentFix",
         this.getInputSchema().getType(References.BLOCK_ENTITY),
         p_376955_ -> p_376955_.update(DSL.remainderFinder(), p_377054_ -> {
            Optional<? extends Dynamic<?>> optional = p_377054_.get("lock").result();
            if (optional.isEmpty()) {
               return p_377054_;
            }

            Dynamic<?> dynamic = InvalidLockComponentFix.fixLock(optional.get());
            return dynamic != null ? p_377054_.set("lock", dynamic) : p_377054_.remove("lock");
         })
      );
   }
}
