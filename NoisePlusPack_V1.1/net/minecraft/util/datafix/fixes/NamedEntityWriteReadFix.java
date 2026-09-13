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

   public NamedEntityWriteReadFix(Schema p_310297_, boolean p_312818_, String p_313129_, TypeReference p_311108_, String p_313092_) {
      super(p_310297_, p_312818_);
      this.name = p_313129_;
      this.type = p_311108_;
      this.entityName = p_313092_;
   }

   public TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(this.type);
      Type<?> type1 = this.getInputSchema().getChoiceType(this.type, this.entityName);
      Type<?> type2 = this.getOutputSchema().getType(this.type);
      OpticFinder<?> opticfinder = DSL.namedChoice(this.entityName, type1);
      Type<?> type3 = ExtraDataFixUtils.patchSubType(type, type, type2);
      return this.fix(type, type2, type3, opticfinder);
   }

   private <S, T, A> TypeRewriteRule fix(Type<S> p_334263_, Type<T> p_329342_, Type<?> p_333979_, OpticFinder<A> p_329193_) {
      return this.fixTypeEverywhereTyped(this.name, p_334263_, p_329342_, p_449314_ -> {
         if (p_449314_.getOptional(p_329193_).isEmpty()) {
            return ExtraDataFixUtils.cast(p_329342_, p_449314_);
         }

         Typed<?> typed = ExtraDataFixUtils.cast(p_333979_, p_449314_);
         return Util.writeAndReadTypedOrThrow((Typed<A>)typed, p_329342_, this::fix);
      });
   }

   protected abstract <T> Dynamic<T> fix(Dynamic<T> var1);
}
