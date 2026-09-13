package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class JigsawPropertiesFix extends NamedEntityFix {
   public JigsawPropertiesFix(final Schema schema, final boolean changesType) {
      super(schema, changesType, "JigsawPropertiesFix", References.BLOCK_ENTITY, "minecraft:jigsaw");
   }

   private static Dynamic<?> fixTag(final Dynamic<?> tag) {
      String oldName = tag.get("attachement_type").asString("minecraft:empty");
      String oldPool = tag.get("target_pool").asString("minecraft:empty");
      return tag.set("name", tag.createString(oldName))
         .set("target", tag.createString(oldName))
         .remove("attachement_type")
         .set("pool", tag.createString(oldPool))
         .remove("target_pool");
   }

   @Override
   protected Typed<?> fix(final Typed<?> entity) {
      return entity.update(DSL.remainderFinder(), JigsawPropertiesFix::fixTag);
   }
}
