package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class EntityHorseSaddleFix extends NamedEntityFix {
   public EntityHorseSaddleFix(final Schema outputSchema, final boolean changesType) {
      super(outputSchema, changesType, "EntityHorseSaddleFix", References.ENTITY, "EntityHorse");
   }

   @Override
   protected Typed<?> fix(final Typed<?> entity) {
      OpticFinder<Pair<String, String>> idF = DSL.fieldFinder("id", DSL.named(References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
      Type<?> itemStackType = this.getInputSchema().getTypeRaw(References.ITEM_STACK);
      OpticFinder<?> saddleF = DSL.fieldFinder("SaddleItem", itemStackType);
      Optional<? extends Typed<?>> saddle = entity.getOptionalTyped(saddleF);
      Dynamic<?> tag = (Dynamic<?>)entity.get(DSL.remainderFinder());
      if (saddle.isEmpty() && tag.get("Saddle").asBoolean(false)) {
         Typed<?> newSaddle = (Typed<?>)itemStackType.pointTyped(entity.getOps()).orElseThrow(IllegalStateException::new);
         newSaddle = newSaddle.set(idF, Pair.of(References.ITEM_NAME.typeName(), "minecraft:saddle"));
         Dynamic<?> saddleTag = tag.emptyMap();
         saddleTag = saddleTag.set("Count", saddleTag.createByte((byte)1));
         saddleTag = saddleTag.set("Damage", saddleTag.createShort((short)0));
         newSaddle = newSaddle.set(DSL.remainderFinder(), saddleTag);
         tag.remove("Saddle");
         return entity.set(saddleF, newSaddle).set(DSL.remainderFinder(), tag);
      } else {
         return entity;
      }
   }
}
