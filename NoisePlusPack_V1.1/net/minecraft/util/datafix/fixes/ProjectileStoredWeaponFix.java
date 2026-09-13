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
   public ProjectileStoredWeaponFix(Schema p_343496_) {
      super(p_343496_, true);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(References.ENTITY);
      Type<?> type1 = this.getOutputSchema().getType(References.ENTITY);
      return this.fixTypeEverywhereTyped(
         "Fix Arrow stored weapon",
         type,
         type1,
         ExtraDataFixUtils.chainAllFilters(this.fixChoice("minecraft:arrow"), this.fixChoice("minecraft:spectral_arrow"))
      );
   }

   private Function<Typed<?>, Typed<?>> fixChoice(String p_344208_) {
      Type<?> type = this.getInputSchema().getChoiceType(References.ENTITY, p_344208_);
      Type<?> type1 = this.getOutputSchema().getChoiceType(References.ENTITY, p_344208_);
      return fixChoiceCap(p_344208_, type, type1);
   }

   private static <T> Function<Typed<?>, Typed<?>> fixChoiceCap(String p_344095_, Type<?> p_343078_, Type<T> p_343868_) {
      OpticFinder<?> opticfinder = DSL.namedChoice(p_344095_, p_343078_);
      return p_344956_ -> p_344956_.updateTyped(
         opticfinder, p_343868_, p_449316_ -> Util.writeAndReadTypedOrThrow(p_449316_, p_343868_, UnaryOperator.identity())
      );
   }
}
