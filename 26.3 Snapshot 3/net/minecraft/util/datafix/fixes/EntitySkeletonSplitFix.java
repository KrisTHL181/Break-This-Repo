package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Objects;

public class EntitySkeletonSplitFix extends SimpleEntityRenameFix {
   public EntitySkeletonSplitFix(final Schema outputSchema, final boolean changesType) {
      super("EntitySkeletonSplitFix", outputSchema, changesType);
   }

   @Override
   protected Pair<String, Dynamic<?>> getNewNameAndTag(String name, final Dynamic<?> tag) {
      if (Objects.equals(name, "Skeleton")) {
         int type = tag.get("SkeletonType").asInt(0);
         if (type == 1) {
            name = "WitherSkeleton";
         } else if (type == 2) {
            name = "Stray";
         }
      }

      return Pair.of(name, tag);
   }
}
