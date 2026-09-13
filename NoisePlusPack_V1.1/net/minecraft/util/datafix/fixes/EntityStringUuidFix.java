package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import java.util.Optional;
import java.util.UUID;

public class EntityStringUuidFix extends DataFix {
   public EntityStringUuidFix(Schema p_15694_, boolean p_15695_) {
      super(p_15694_, p_15695_);
   }

   public TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "EntityStringUuidFix",
         this.getInputSchema().getType(References.ENTITY),
         p_15697_ -> p_15697_.update(
            DSL.remainderFinder(),
            p_326577_ -> {
               Optional<String> optional = p_326577_.get("UUID").asString().result();
               if (optional.isPresent()) {
                  UUID uuid = UUID.fromString(optional.get());
                  return p_326577_.remove("UUID")
                     .set("UUIDMost", p_326577_.createLong(uuid.getMostSignificantBits()))
                     .set("UUIDLeast", p_326577_.createLong(uuid.getLeastSignificantBits()));
               } else {
                  return p_326577_;
               }
            }
         )
      );
   }
}
