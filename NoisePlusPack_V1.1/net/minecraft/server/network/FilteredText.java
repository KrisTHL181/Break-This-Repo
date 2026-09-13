package net.minecraft.server.network;

import java.util.Objects;
import net.minecraft.network.chat.FilterMask;
import org.jspecify.annotations.Nullable;

public record FilteredText(String raw, FilterMask mask) {
   public static final FilteredText EMPTY = passThrough("");

   public static FilteredText passThrough(String p_243257_) {
      return new FilteredText(p_243257_, FilterMask.PASS_THROUGH);
   }

   public static FilteredText fullyFiltered(String p_243261_) {
      return new FilteredText(p_243261_, FilterMask.FULLY_FILTERED);
   }

   public @Nullable String filtered() {
      return this.mask.apply(this.raw);
   }

   public String filteredOrEmpty() {
      return Objects.requireNonNullElse(this.filtered(), "");
   }

   public boolean isFiltered() {
      return !this.mask.isEmpty();
   }
}
