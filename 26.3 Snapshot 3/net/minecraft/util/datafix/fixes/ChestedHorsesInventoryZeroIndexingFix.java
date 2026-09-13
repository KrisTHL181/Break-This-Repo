package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Dynamic;

public class ChestedHorsesInventoryZeroIndexingFix extends DataFix {
   public ChestedHorsesInventoryZeroIndexingFix(final Schema v3807) {
      super(v3807, false);
   }

   protected TypeRewriteRule makeRule() {
      OpticFinder<Pair<String, Pair<Either<Pair<String, String>, Unit>, Pair<Either<?, Unit>, Dynamic<?>>>>> itemStackFinder = DSL.typeFinder(
         this.getInputSchema().getType(References.ITEM_STACK)
      );
      Type<?> entityType = this.getInputSchema().getType(References.ENTITY);
      return TypeRewriteRule.seq(
         this.horseLikeInventoryIndexingFixer(itemStackFinder, entityType, "minecraft:llama"),
         new TypeRewriteRule[]{
            this.horseLikeInventoryIndexingFixer(itemStackFinder, entityType, "minecraft:trader_llama"),
            this.horseLikeInventoryIndexingFixer(itemStackFinder, entityType, "minecraft:mule"),
            this.horseLikeInventoryIndexingFixer(itemStackFinder, entityType, "minecraft:donkey")
         }
      );
   }

   private TypeRewriteRule horseLikeInventoryIndexingFixer(
      final OpticFinder<Pair<String, Pair<Either<Pair<String, String>, Unit>, Pair<Either<?, Unit>, Dynamic<?>>>>> itemStackFinder,
      final Type<?> schema,
      final String horseId
   ) {
      Type<?> choiceType = this.getInputSchema().getChoiceType(References.ENTITY, horseId);
      OpticFinder<?> entityFinder = DSL.namedChoice(horseId, choiceType);
      OpticFinder<?> itemsFieldFinder = choiceType.findField("Items");
      return this.fixTypeEverywhereTyped(
         "Fix non-zero indexing in chest horse type " + horseId,
         schema,
         input -> input.updateTyped(
            entityFinder,
            horseLike -> horseLike.updateTyped(
               itemsFieldFinder,
               items -> items.update(
                  itemStackFinder,
                  namedStack -> namedStack.mapSecond(
                     itemStack -> itemStack.mapSecond(
                        pair -> pair.mapSecond(remainder -> remainder.update("Slot", slot -> slot.createByte((byte)(slot.asInt(2) - 2))))
                     )
                  )
               )
            )
         )
      );
   }
}
