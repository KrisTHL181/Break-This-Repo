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

   public EntityRenameFix(String p_15618_, Schema p_15619_, boolean p_15620_) {
      super(p_15619_, p_15620_);
      this.name = p_15618_;
   }

   public TypeRewriteRule makeRule() {
      TaggedChoiceType<String> taggedchoicetype = this.getInputSchema().findChoiceType(References.ENTITY);
      TaggedChoiceType<String> taggedchoicetype1 = this.getOutputSchema().findChoiceType(References.ENTITY);
      Function<String, Type<?>> function = Util.memoize(p_358832_ -> {
         Type<?> type = (Type<?>)taggedchoicetype.types().get(p_358832_);
         return ExtraDataFixUtils.patchSubType(type, taggedchoicetype, taggedchoicetype1);
      });
      return this.fixTypeEverywhere(
         this.name,
         taggedchoicetype,
         taggedchoicetype1,
         p_15624_ -> p_358836_ -> {
            String s = (String)p_358836_.getFirst();
            Type<?> type = function.apply(s);
            Pair<String, Typed<?>> pair = this.fix(s, this.getEntity(p_358836_.getSecond(), p_15624_, type));
            Type<?> type1 = (Type<?>)taggedchoicetype1.types().get(pair.getFirst());
            if (!type1.equals(((Typed)pair.getSecond()).getType(), true, true)) {
               throw new IllegalStateException(
                  String.format(Locale.ROOT, "Dynamic type check failed: %s not equal to %s", type1, ((Typed)pair.getSecond()).getType())
               );
            } else {
               return Pair.of((String)pair.getFirst(), ((Typed)pair.getSecond()).getValue());
            }
         }
      );
   }

   private <A> Typed<A> getEntity(Object p_15631_, DynamicOps<?> p_15632_, Type<A> p_15633_) {
      return new Typed(p_15633_, p_15632_, p_15631_);
   }

   protected abstract Pair<String, Typed<?>> fix(String var1, Typed<?> var2);
}
