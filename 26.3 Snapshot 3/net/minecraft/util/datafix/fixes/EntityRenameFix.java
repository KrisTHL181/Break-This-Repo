package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.TaggedChoice.TaggedChoiceType;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DynamicOps;
import java.util.Locale;
import java.util.function.Function;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.ExtraDataFixUtils;

public abstract class EntityRenameFix extends DataFix {
   protected final String name;

   public EntityRenameFix(final String name, final Schema outputSchema, final boolean changesType) {
      super(outputSchema, changesType);
      this.name = name;
   }

   public TypeRewriteRule makeRule() {
      TaggedChoiceType<String> oldType = this.getInputSchema().findChoiceType(References.ENTITY);
      TaggedChoiceType<String> newType = this.getOutputSchema().findChoiceType(References.ENTITY);
      Function<String, Type<?>> patchedInputTypes = Util.memoize(name -> {
         Type<?> type = (Type<?>)oldType.types().get(name);
         return ExtraDataFixUtils.patchSubType(type, oldType, newType);
      });
      return this.fixTypeEverywhere(
         this.name,
         oldType,
         newType,
         ops -> input -> {
            String oldName = (String)input.getFirst();
            Type<?> oldEntityType = patchedInputTypes.apply(oldName);
            Pair<String, Typed<?>> newEntity = this.fix(oldName, this.getEntity(input.getSecond(), ops, oldEntityType));
            Type<?> expectedType = (Type<?>)newType.types().get(newEntity.getFirst());
            if (!expectedType.equals(((Typed)newEntity.getSecond()).getType(), true, true)) {
               throw new IllegalStateException(
                  String.format(Locale.ROOT, "Dynamic type check failed: %s not equal to %s", expectedType, ((Typed)newEntity.getSecond()).getType())
               );
            } else {
               return Pair.of((String)newEntity.getFirst(), ((Typed)newEntity.getSecond()).getValue());
            }
         }
      );
   }

   private <A> Typed<A> getEntity(final Object input, final DynamicOps<?> ops, final Type<A> oldEntityType) {
      return new Typed(oldEntityType, ops, input);
   }

   protected abstract Pair<String, Typed<?>> fix(final String name, final Typed<?> entity);
}
