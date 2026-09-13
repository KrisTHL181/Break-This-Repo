package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;

public class RenameNameplateToNameTagFix extends AttributesRenameFix {
   public RenameNameplateToNameTagFix(final Schema outputSchema) {
      super(outputSchema, "RenameNameplateToNameTag", RenameNameplateToNameTagFix::rename);
   }

   private static String rename(final String oldName) {
      return oldName.equals("minecraft:nameplate_distance") ? "minecraft:name_tag_distance" : oldName;
   }
}
