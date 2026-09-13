package net.minecraft.util.datafix.fixes;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import java.util.function.Supplier;
import net.minecraft.util.datafix.ExtraDataFixUtils;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class ThrownPotionSplitFix extends EntityRenameFix {
   private final Supplier<ThrownPotionSplitFix.ItemIdFinder> itemIdFinder = Suppliers.memoize(
      () -> {
         Type<?> potionType = this.getInputSchema().getChoiceType(References.ENTITY, "minecraft:potion");
         Type<?> patchedPotionType = ExtraDataFixUtils.patchSubType(
            potionType, this.getInputSchema().getType(References.ENTITY), this.getOutputSchema().getType(References.ENTITY)
         );
         OpticFinder<?> itemFinder = patchedPotionType.findField("Item");
         OpticFinder<Pair<String, String>> itemIdFinder = DSL.fieldFinder("id", DSL.named(References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
         return new ThrownPotionSplitFix.ItemIdFinder(itemFinder, itemIdFinder);
      }
   );

   public ThrownPotionSplitFix(final Schema outputSchema) {
      super("ThrownPotionSplitFix", outputSchema, true);
   }

   @Override
   protected Pair<String, Typed<?>> fix(final String name, final Typed<?> entity) {
      if (!name.equals("minecraft:potion")) {
         return Pair.of(name, entity);
      }

      String itemId = this.itemIdFinder.get().getItemId(entity);
      return "minecraft:lingering_potion".equals(itemId) ? Pair.of("minecraft:lingering_potion", entity) : Pair.of("minecraft:splash_potion", entity);
   }

   private record ItemIdFinder(OpticFinder<?> itemFinder, OpticFinder<Pair<String, String>> itemIdFinder) {
      public String getItemId(final Typed<?> entity) {
         return entity.getOptionalTyped(this.itemFinder)
            .flatMap(item -> item.getOptional(this.itemIdFinder))
            .<String>map(Pair::getSecond)
            .map(NamespacedSchema::ensureNamespaced)
            .orElse("");
      }
   }
}
