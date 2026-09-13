package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Dynamic;
import java.util.Optional;

public class EmptyItemInHotbarFix extends DataFix {
   public EmptyItemInHotbarFix(Schema p_327820_) {
      super(p_327820_, false);
   }

   public TypeRewriteRule makeRule() {
      OpticFinder<Pair<String, Pair<Either<Pair<String, String>, Unit>, Pair<Either<?, Unit>, Dynamic<?>>>>> opticfinder = DSL.typeFinder(
         this.getInputSchema().getType(References.ITEM_STACK)
      );
      return this.fixTypeEverywhereTyped(
         "EmptyItemInHotbarFix",
         this.getInputSchema().getType(References.HOTBAR),
         p_332820_ -> p_332820_.update(opticfinder, p_336352_ -> p_336352_.mapSecond(p_333926_ -> {
            Optional<String> optional = ((Either)p_333926_.getFirst()).left().map(Pair::getSecond);
            Dynamic<?> dynamic = (Dynamic<?>)((Pair)p_333926_.getSecond()).getSecond();
            boolean flag = optional.isEmpty() || optional.get().equals("minecraft:air");
            boolean flag1 = dynamic.get("Count").asInt(0) <= 0;
            return !flag && !flag1 ? p_333926_ : Pair.of(Either.right(Unit.INSTANCE), Pair.of(Either.right(Unit.INSTANCE), dynamic.emptyMap()));
         }))
      );
   }
}
