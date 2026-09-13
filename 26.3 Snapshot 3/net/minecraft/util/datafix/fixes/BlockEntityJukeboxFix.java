package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;

public class BlockEntityJukeboxFix extends NamedEntityFix {
   public BlockEntityJukeboxFix(final Schema outputSchema, final boolean changesType) {
      super(outputSchema, changesType, "BlockEntityJukeboxFix", References.BLOCK_ENTITY, "minecraft:jukebox");
   }

   @Override
   protected Typed<?> fix(final Typed<?> entity) {
      Type<?> jukeboxType = this.getInputSchema().getChoiceType(References.BLOCK_ENTITY, "minecraft:jukebox");
      Type<?> itemStackType = jukeboxType.findFieldType("RecordItem");
      OpticFinder<?> recordItemF = DSL.fieldFinder("RecordItem", itemStackType);
      Dynamic<?> tag = (Dynamic<?>)entity.get(DSL.remainderFinder());
      int recordId = tag.get("Record").asInt(0);
      if (recordId > 0) {
         tag.remove("Record");
         String id = ItemStackTheFlatteningFix.updateItem(ItemIdFix.getItem(recordId), 0);
         if (id != null) {
            Dynamic<?> itemTag = tag.emptyMap();
            itemTag = itemTag.set("id", itemTag.createString(id));
            itemTag = itemTag.set("Count", itemTag.createByte((byte)1));
            return entity.set(
                  recordItemF,
                  (Typed)((Pair)itemStackType.readTyped(itemTag).result().orElseThrow(() -> new IllegalStateException("Could not create record item stack.")))
                     .getFirst()
               )
               .set(DSL.remainderFinder(), tag);
         }
      }

      return entity;
   }
}
