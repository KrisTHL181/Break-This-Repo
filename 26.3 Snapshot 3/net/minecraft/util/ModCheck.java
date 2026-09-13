package net.minecraft.util;

import java.util.function.Supplier;
import org.apache.commons.lang3.ObjectUtils;

public record ModCheck(ModCheck.Confidence confidence, String description) {
   public static ModCheck identify(final String expectedBrand, final Supplier<String> actualBrand, final String component, final Class<?> canaryClass) {
      String mod = actualBrand.get();
      if (!expectedBrand.equals(mod)) {
         return new ModCheck(ModCheck.Confidence.DEFINITELY, component + " brand changed to '" + mod + "'");
      } else {
         return canaryClass.getSigners() == null
            ? new ModCheck(ModCheck.Confidence.VERY_LIKELY, component + " jar signature invalidated")
            : new ModCheck(ModCheck.Confidence.PROBABLY_NOT, component + " jar signature and brand is untouched");
      }
   }

   public boolean shouldReportAsModified() {
      return this.confidence.shouldReportAsModified;
   }

   public ModCheck merge(final ModCheck other) {
      return new ModCheck(
         (ModCheck.Confidence)ObjectUtils.max(new ModCheck.Confidence[]{this.confidence, other.confidence}), this.description + "; " + other.description
      );
   }

   public String fullDescription() {
      return this.confidence.description + " " + this.description;
   }

   public enum Confidence {
      PROBABLY_NOT("Probably not.", false),
      VERY_LIKELY("Very likely;", true),
      DEFINITELY("Definitely;", true);

      private final String description;
      private final boolean shouldReportAsModified;

      Confidence(final String description, final boolean shouldReportAsModified) {
         this.description = description;
         this.shouldReportAsModified = shouldReportAsModified;
      }
   }
}
