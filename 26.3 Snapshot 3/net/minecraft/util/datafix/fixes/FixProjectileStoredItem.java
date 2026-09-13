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

   public FixProjectileStoredItem(final Schema outputSchema) {
      super(outputSchema, true);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> inputEntityType = this.getInputSchema().getType(References.ENTITY);
      Type<?> outputEntityType = this.getOutputSchema().getType(References.ENTITY);
      return this.fixTypeEverywhereTyped(
         "Fix AbstractArrow item type",
         inputEntityType,
         outputEntityType,
         ExtraDataFixUtils.chainAllFilters(
            this.fixChoice("minecraft:trident", FixProjectileStoredItem::castUnchecked),
            this.fixChoice("minecraft:arrow", FixProjectileStoredItem::fixArrow),
            this.fixChoice("minecraft:spectral_arrow", FixProjectileStoredItem::fixSpectralArrow)
         )
      );
   }

   private Function<Typed<?>, Typed<?>> fixChoice(final String entityName, final FixProjectileStoredItem.SubFixer<?> fixer) {
      Type<?> inputEntityChoiceType = this.getInputSchema().getChoiceType(References.ENTITY, entityName);
      Type<?> outputEntityChoiceType = this.getOutputSchema().getChoiceType(References.ENTITY, entityName);
      return fixChoiceCap(entityName, fixer, inputEntityChoiceType, outputEntityChoiceType);
   }

   private static <T> Function<Typed<?>, Typed<?>> fixChoiceCap(
      final String entityName, final FixProjectileStoredItem.SubFixer<?> fixer, final Type<?> inputEntityChoiceType, final Type<T> outputEntityChoiceType
   ) {
      OpticFinder<?> entityF = DSL.namedChoice(entityName, inputEntityChoiceType);
      FixProjectileStoredItem.SubFixer<T> typedFixer = (FixProjectileStoredItem.SubFixer<T>)fixer;
      return input -> input.updateTyped(entityF, outputEntityChoiceType, typed -> typedFixer.fix(typed, outputEntityChoiceType));
   }

   private static <T> Typed<T> fixArrow(final Typed<?> typed, final Type<T> outputType) {
      return Util.writeAndReadTypedOrThrow(typed, outputType, input -> input.set("item", createItemStack(input, getArrowType(input))));
   }

   private static String getArrowType(final Dynamic<?> input) {
      return input.get("Potion").asString("minecraft:empty").equals("minecraft:empty") ? "minecraft:arrow" : "minecraft:tipped_arrow";
   }

   private static <T> Typed<T> fixSpectralArrow(final Typed<?> typed, final Type<T> outputType) {
      return Util.writeAndReadTypedOrThrow(typed, outputType, input -> input.set("item", createItemStack(input, "minecraft:spectral_arrow")));
   }

   private static Dynamic<?> createItemStack(final Dynamic<?> input, final String itemName) {
      return input.createMap(ImmutableMap.of(input.createString("id"), input.createString(itemName), input.createString("Count"), input.createInt(1)));
   }

   private static <T> Typed<T> castUnchecked(final Typed<?> input, final Type<T> outputType) {
      return new Typed(outputType, input.getOps(), input.getValue());
   }

   private interface SubFixer<F> {
      Typed<F> fix(final Typed<?> input, final Type<F> outputType);
   }
}
