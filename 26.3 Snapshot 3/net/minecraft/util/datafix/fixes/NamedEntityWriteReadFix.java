package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.ExtraDataFixUtils;

public abstract class NamedEntityWriteReadFix extends DataFix {
   private final String name;
   private final String entityName;
   private final TypeReference type;

   public NamedEntityWriteReadFix(final Schema outputSchema, final boolean changesType, final String name, final TypeReference type, final String entityName) {
      super(outputSchema, changesType);
      this.name = name;
      this.type = type;
      this.entityName = entityName;
   }

   public TypeRewriteRule makeRule() {
      Type<?> inputEntityType = this.getInputSchema().getType(this.type);
      Type<?> inputEntityChoiceType = this.getInputSchema().getChoiceType(this.type, this.entityName);
      Type<?> outputEntityType = this.getOutputSchema().getType(this.type);
      OpticFinder<?> entityF = DSL.namedChoice(this.entityName, inputEntityChoiceType);
      Type<?> patchedEntityType = ExtraDataFixUtils.patchSubType(inputEntityType, inputEntityType, outputEntityType);
      return this.fix(inputEntityType, outputEntityType, patchedEntityType, entityF);
   }

   private <S, T, A> TypeRewriteRule fix(
      final Type<S> inputEntityType, final Type<T> outputEntityType, final Type<?> patchedEntityType, final OpticFinder<A> choiceFinder
   ) {
      return this.fixTypeEverywhereTyped(this.name, inputEntityType, outputEntityType, typed -> {
         if (typed.getOptional(choiceFinder).isEmpty()) {
            return ExtraDataFixUtils.cast(outputEntityType, typed);
         }

         Typed<?> fakeTyped = ExtraDataFixUtils.cast(patchedEntityType, typed);
         return Util.writeAndReadTypedOrThrow((Typed<A>)fakeTyped, outputEntityType, this::fix);
      });
   }

   protected abstract <T> Dynamic<T> fix(final Dynamic<T> input);
}
