package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Objects;
import net.minecraft.util.Util;

public class EntityHorseSplitFix extends EntityRenameFix {
   public EntityHorseSplitFix(final Schema outputSchema, final boolean changesType) {
      super("EntityHorseSplitFix", outputSchema, changesType);
   }

   @Override
   protected Pair<String, Typed<?>> fix(final String name, final Typed<?> entity) {
      if (Objects.equals("EntityHorse", name)) {
         Dynamic<?> tag = (Dynamic<?>)entity.get(DSL.remainderFinder());
         int type = tag.get("Type").asInt(0);

         String newName = switch (type) {
            case 1 -> "Donkey";
            case 2 -> "Mule";
            case 3 -> "ZombieHorse";
            case 4 -> "SkeletonHorse";
            default -> "Horse";
         };
         Type<?> newType = (Type<?>)this.getOutputSchema().findChoiceType(References.ENTITY).types().get(newName);
         return Pair.of(newName, Util.writeAndReadTypedOrThrow(entity, newType, dynamic -> dynamic.remove("Type")));
      } else {
         return Pair.of(name, entity);
      }
   }
}
