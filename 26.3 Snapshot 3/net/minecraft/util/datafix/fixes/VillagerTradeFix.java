package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class VillagerTradeFix extends DataFix {
   public VillagerTradeFix(final Schema outputSchema) {
      super(outputSchema, false);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> recipeType = this.getInputSchema().getType(References.VILLAGER_TRADE);
      OpticFinder<?> buyFinder = recipeType.findField("buy");
      OpticFinder<?> buyBFinder = recipeType.findField("buyB");
      OpticFinder<?> sellFinder = recipeType.findField("sell");
      OpticFinder<Pair<String, String>> idF = DSL.fieldFinder("id", DSL.named(References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
      Function<Typed<?>, Typed<?>> itemStackUpdater = itemStack -> this.updateItemStack(idF, itemStack);
      return this.fixTypeEverywhereTyped(
         "Villager trade fix",
         recipeType,
         recipe -> recipe.updateTyped(buyFinder, itemStackUpdater).updateTyped(buyBFinder, itemStackUpdater).updateTyped(sellFinder, itemStackUpdater)
      );
   }

   private Typed<?> updateItemStack(final OpticFinder<Pair<String, String>> idF, final Typed<?> itemStack) {
      return itemStack.update(idF, pair -> pair.mapSecond(name -> Objects.equals(name, "minecraft:carved_pumpkin") ? "minecraft:pumpkin" : name));
   }
}
