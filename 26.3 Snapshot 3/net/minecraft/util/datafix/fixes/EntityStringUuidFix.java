package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import java.util.Optional;
import java.util.UUID;

public class EntityStringUuidFix extends DataFix {
   public EntityStringUuidFix(final Schema outputSchema, final boolean changesType) {
      super(outputSchema, changesType);
   }

   public TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "EntityStringUuidFix",
         this.getInputSchema().getType(References.ENTITY),
         input -> input.update(
            DSL.remainderFinder(),
            tag -> {
               Optional<String> uuidString = tag.get("UUID").asString().result();
               if (uuidString.isPresent()) {
                  UUID uuid = UUID.fromString(uuidString.get());
                  return tag.remove("UUID")
                     .set("UUIDMost", tag.createLong(uuid.getMostSignificantBits()))
                     .set("UUIDLeast", tag.createLong(uuid.getLeastSignificantBits()));
               } else {
                  return tag;
               }
            }
         )
      );
   }
}
