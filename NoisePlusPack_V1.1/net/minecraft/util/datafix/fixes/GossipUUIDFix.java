package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class GossipUUIDFix extends NamedEntityFix {
   public GossipUUIDFix(Schema p_15878_, String p_15879_) {
      super(p_15878_, false, "Gossip for for " + p_15879_, References.ENTITY, p_15879_);
   }

   @Override
   protected Typed<?> fix(Typed<?> p_15881_) {
      return p_15881_.update(
         DSL.remainderFinder(),
         p_15883_ -> p_15883_.update(
            "Gossips",
            p_326594_ -> (Dynamic)DataFixUtils.orElse(
               p_326594_.asStreamOpt()
                  .result()
                  .map(
                     p_145374_ -> p_145374_.map(
                        p_145378_ -> (Dynamic)AbstractUUIDFix.replaceUUIDLeastMost((Dynamic<?>)p_145378_, "Target", "Target").orElse((Dynamic<?>)p_145378_)
                     )
                  )
                  .map(p_326594_::createList),
               p_326594_
            )
         )
      );
   }
}
