package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import net.minecraft.util.Util;

public class EntityMinecartIdentifiersFix extends EntityRenameFix {
   public EntityMinecartIdentifiersFix(final Schema outputSchema) {
      super("EntityMinecartIdentifiersFix", outputSchema, true);
   }

   @Override
   protected Pair<String, Typed<?>> fix(final String name, final Typed<?> entity) {
      if (!name.equals("Minecart")) {
         return Pair.of(name, entity);
      }

      int id = ((Dynamic)entity.getOrCreate(DSL.remainderFinder())).get("Type").asInt(0);

      String newName = switch (id) {
         case 1 -> "MinecartChest";
         case 2 -> "MinecartFurnace";
         default -> "MinecartRideable";
      };
      Type<?> newType = (Type<?>)this.getOutputSchema().findChoiceType(References.ENTITY).types().get(newName);
      return Pair.of(newName, Util.writeAndReadTypedOrThrow(entity, newType, dynamic -> dynamic.remove("Type")));
   }
}
