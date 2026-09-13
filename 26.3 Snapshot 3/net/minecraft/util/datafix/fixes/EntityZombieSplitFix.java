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

   public EntityZombieSplitFix(final Schema outputSchema) {
      super("EntityZombieSplitFix", outputSchema, true);
   }

   @Override
   protected Pair<String, Typed<?>> fix(final String name, final Typed<?> entity) {
      if (!name.equals("Zombie")) {
         return Pair.of(name, entity);
      }

      Dynamic<?> tag = (Dynamic<?>)entity.getOptional(DSL.remainderFinder()).orElseThrow();
      int type = tag.get("ZombieType").asInt(0);
      String newName;
      Typed<?> newEntity;
      switch (type) {
         case 1:
         case 2:
         case 3:
         case 4:
         case 5:
            newName = "ZombieVillager";
            newEntity = this.changeSchemaToZombieVillager(entity, type - 1);
            break;
         case 6:
            newName = "Husk";
            newEntity = entity;
            break;
         default:
            newName = "Zombie";
            newEntity = entity;
      }

      return Pair.of(newName, newEntity.update(DSL.remainderFinder(), e -> e.remove("ZombieType")));
   }

   private Typed<?> changeSchemaToZombieVillager(final Typed<?> entity, final int profession) {
      return Util.writeAndReadTypedOrThrow(
         entity, this.zombieVillagerType.get(), serializedEntity -> serializedEntity.set("Profession", serializedEntity.createInt(profession))
      );
   }
}
