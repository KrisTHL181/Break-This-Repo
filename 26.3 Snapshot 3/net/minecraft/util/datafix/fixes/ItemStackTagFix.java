package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public abstract class ItemStackTagFix extends DataFix {
   private final String name;
   private final Predicate<String> idFilter;

   public ItemStackTagFix(final Schema outputSchema, final String name, final Predicate<String> idFilter) {
      super(outputSchema, false);
      this.name = name;
      this.idFilter = idFilter;
   }

   public final TypeRewriteRule makeRule() {
      Type<?> itemStackType = this.getInputSchema().getType(References.ITEM_STACK);
      return this.fixTypeEverywhereTyped(this.name, itemStackType, createFixer(itemStackType, this.idFilter, this::fixItemStackTag));
   }

   public static UnaryOperator<Typed<?>> createFixer(final Type<?> itemStackType, final Predicate<String> idFilter, final UnaryOperator<Typed<?>> fixer) {
      OpticFinder<Pair<String, String>> idF = DSL.fieldFinder("id", DSL.named(References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
      OpticFinder<?> tagF = itemStackType.findField("tag");
      return input -> {
         Optional<Pair<String, String>> idOpt = input.getOptional(idF);
         return idOpt.isPresent() && idFilter.test((String)idOpt.get().getSecond()) ? input.updateTyped(tagF, fixer) : input;
      };
   }

   protected abstract Typed<?> fixItemStackTag(final Typed<?> tag);
}
