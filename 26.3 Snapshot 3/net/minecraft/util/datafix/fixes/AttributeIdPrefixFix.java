package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import java.util.List;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class AttributeIdPrefixFix extends AttributesRenameFix {
   private static final List<String> PREFIXES = List.of("generic.", "horse.", "player.", "zombie.");

   public AttributeIdPrefixFix(final Schema outputSchema) {
      super(outputSchema, "AttributeIdPrefixFix", AttributeIdPrefixFix::replaceId, true);
   }

   private static String replaceId(final String id) {
      String namespacedId = NamespacedSchema.ensureNamespaced(id);

      for (String prefix : PREFIXES) {
         String namespacedPrefix = NamespacedSchema.ensureNamespaced(prefix);
         if (namespacedId.startsWith(namespacedPrefix)) {
            return "minecraft:" + namespacedId.substring(namespacedPrefix.length());
         }
      }

      return id;
   }
}
