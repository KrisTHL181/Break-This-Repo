package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import java.util.List;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class AttributeIdPrefixFix extends AttributesRenameFix {
   private static final List<String> PREFIXES = List.of("generic.", "horse.", "player.", "zombie.");

   public AttributeIdPrefixFix(Schema p_365079_) {
      super(p_365079_, "AttributeIdPrefixFix", AttributeIdPrefixFix::replaceId);
   }

   private static String replaceId(String p_361180_) {
      String s = NamespacedSchema.ensureNamespaced(p_361180_);

      for (String s1 : PREFIXES) {
         String s2 = NamespacedSchema.ensureNamespaced(s1);
         if (s.startsWith(s2)) {
            return "minecraft:" + s.substring(s2.length());
         }
      }

      return p_361180_;
   }
}
