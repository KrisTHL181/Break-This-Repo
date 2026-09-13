package net.minecraft.util.datafix.fixes;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.function.Supplier;
import net.minecraft.util.Util;

public class EntityZombieSplitFix extends EntityRenameFix {
   private final Supplier<Type<?>> zombieVillagerType = Suppliers.memoize(() -> this.getOutputSchema().getChoiceType(References.ENTITY, "ZombieVillager"));

   public EntityZombieSplitFix(Schema p_15798_) {
      super("EntityZombieSplitFix", p_15798_, true);
   }

   @Override
   protected Pair<String, Typed<?>> fix(String p_331870_, Typed<?> p_331918_) {
      if (!p_331870_.equals("Zombie")) {
         return Pair.of(p_331870_, p_331918_);
      }

      Dynamic<?> dynamic = (Dynamic<?>)p_331918_.getOptional(DSL.remainderFinder()).orElseThrow();
      int i = dynamic.get("ZombieType").asInt(0);
      String s;
      Typed<?> typed;
      switch (i) {
         case 1:
         case 2:
         case 3:
         case 4:
         case 5:
            s = "ZombieVillager";
            typed = this.changeSchemaToZombieVillager(p_331918_, i - 1);
            break;
         case 6:
            s = "Husk";
            typed = p_331918_;
            break;
         default:
            s = "Zombie";
            typed = p_331918_;
      }

      return Pair.of(s, typed.update(DSL.remainderFinder(), p_333056_ -> p_333056_.remove("ZombieType")));
   }

   private Typed<?> changeSchemaToZombieVillager(Typed<?> p_336232_, int p_336308_) {
      return Util.writeAndReadTypedOrThrow(p_336232_, this.zombieVillagerType.get(), p_329329_ -> p_329329_.set("Profession", p_329329_.createInt(p_336308_)));
   }
}
