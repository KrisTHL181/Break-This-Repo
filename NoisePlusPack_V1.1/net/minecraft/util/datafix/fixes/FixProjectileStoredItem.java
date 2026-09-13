package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.function.Function;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.ExtraDataFixUtils;

public class FixProjectileStoredItem extends DataFix {
   private static final String EMPTY_POTION = "minecraft:empty";

   public FixProjectileStoredItem(Schema p_310923_) {
      super(p_310923_, true);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(References.ENTITY);
      Type<?> type1 = this.getOutputSchema().getType(References.ENTITY);
      return this.fixTypeEverywhereTyped(
         "Fix AbstractArrow item type",
         type,
         type1,
         ExtraDataFixUtils.chainAllFilters(
            this.fixChoice("minecraft:trident", FixProjectileStoredItem::castUnchecked),
            this.fixChoice("minecraft:arrow", FixProjectileStoredItem::fixArrow),
            this.fixChoice("minecraft:spectral_arrow", FixProjectileStoredItem::fixSpectralArrow)
         )
      );
   }

   private Function<Typed<?>, Typed<?>> fixChoice(String p_312752_, FixProjectileStoredItem.SubFixer<?> p_310778_) {
      Type<?> type = this.getInputSchema().getChoiceType(References.ENTITY, p_312752_);
      Type<?> type1 = this.getOutputSchema().getChoiceType(References.ENTITY, p_312752_);
      return fixChoiceCap(p_312752_, p_310778_, type, type1);
   }

   private static <T> Function<Typed<?>, Typed<?>> fixChoiceCap(
      String p_312294_, FixProjectileStoredItem.SubFixer<?> p_310164_, Type<?> p_310703_, Type<T> p_312528_
   ) {
      OpticFinder<?> opticfinder = DSL.namedChoice(p_312294_, p_310703_);
      return p_313205_ -> p_313205_.updateTyped(opticfinder, p_312528_, p_312567_ -> p_310164_.fix(p_312567_, p_312528_));
   }

   private static <T> Typed<T> fixArrow(Typed<?> p_312190_, Type<T> p_311775_) {
      return Util.writeAndReadTypedOrThrow(p_312190_, p_311775_, p_312479_ -> p_312479_.set("item", createItemStack(p_312479_, getArrowType(p_312479_))));
   }

   private static String getArrowType(Dynamic<?> p_311918_) {
      return p_311918_.get("Potion").asString("minecraft:empty").equals("minecraft:empty") ? "minecraft:arrow" : "minecraft:tipped_arrow";
   }

   private static <T> Typed<T> fixSpectralArrow(Typed<?> p_311496_, Type<T> p_311551_) {
      return Util.writeAndReadTypedOrThrow(p_311496_, p_311551_, p_310800_ -> p_310800_.set("item", createItemStack(p_310800_, "minecraft:spectral_arrow")));
   }

   private static Dynamic<?> createItemStack(Dynamic<?> p_310249_, String p_312956_) {
      return p_310249_.createMap(
         ImmutableMap.of(p_310249_.createString("id"), p_310249_.createString(p_312956_), p_310249_.createString("Count"), p_310249_.createInt(1))
      );
   }

   private static <T> Typed<T> castUnchecked(Typed<?> p_310006_, Type<T> p_312989_) {
      return new Typed(p_312989_, p_310006_.getOps(), p_310006_.getValue());
   }

   interface SubFixer<F> {
      Typed<F> fix(Typed<?> var1, Type<F> var2);
   }
}
