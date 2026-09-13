package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;

public class ContainerBlockEntityLockPredicateFix extends DataFix {
   public ContainerBlockEntityLockPredicateFix(Schema p_362539_) {
      super(p_362539_, false);
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "ContainerBlockEntityLockPredicateFix",
         this.getInputSchema().findChoiceType(References.BLOCK_ENTITY),
         ContainerBlockEntityLockPredicateFix::fixBlockEntity
      );
   }

   private static Typed<?> fixBlockEntity(Typed<?> p_363963_) {
      return p_363963_.update(DSL.remainderFinder(), p_368586_ -> p_368586_.renameAndFixField("Lock", "lock", LockComponentPredicateFix::fixLock));
   }
}
