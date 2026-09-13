package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.ExtraDataFixUtils;

public class ProjectileStoredWeaponFix extends DataFix {
   public ProjectileStoredWeaponFix(final Schema outputSchema) {
      super(outputSchema, true);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> inputEntityType = this.getInputSchema().getType(References.ENTITY);
      Type<?> outputEntityType = this.getOutputSchema().getType(References.ENTITY);
      return this.fixTypeEverywhereTyped(
         "Fix Arrow stored weapon",
         inputEntityType,
         outputEntityType,
         ExtraDataFixUtils.chainAllFilters(this.fixChoice("minecraft:arrow"), this.fixChoice("minecraft:spectral_arrow"))
      );
   }

   private Function<Typed<?>, Typed<?>> fixChoice(final String entityName) {
      Type<?> inputEntityChoiceType = this.getInputSchema().getChoiceType(References.ENTITY, entityName);
      Type<?> outputEntityChoiceType = this.getOutputSchema().getChoiceType(References.ENTITY, entityName);
      return fixChoiceCap(entityName, inputEntityChoiceType, outputEntityChoiceType);
   }

   private static <T> Function<Typed<?>, Typed<?>> fixChoiceCap(
      final String entityName, final Type<?> inputEntityChoiceType, final Type<T> outputEntityChoiceType
   ) {
      OpticFinder<?> entityF = DSL.namedChoice(entityName, inputEntityChoiceType);
      return input -> input.updateTyped(
         entityF, outputEntityChoiceType, typed -> Util.writeAndReadTypedOrThrow(typed, outputEntityChoiceType, UnaryOperator.identity())
      );
   }
}
