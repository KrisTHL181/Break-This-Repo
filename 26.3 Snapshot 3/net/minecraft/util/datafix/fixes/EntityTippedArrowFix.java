package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import java.util.Objects;

public class EntityTippedArrowFix extends SimplestEntityRenameFix {
   public EntityTippedArrowFix(final Schema outputSchema, final boolean changesType) {
      super("EntityTippedArrowFix", outputSchema, changesType);
   }

   @Override
   protected String rename(final String name) {
      return Objects.equals(name, "TippedArrow") ? "Arrow" : name;
   }
}
