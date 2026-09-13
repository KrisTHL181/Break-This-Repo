package net.minecraft.client;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.network.chat.FormattedText;
import org.jspecify.annotations.Nullable;

public class ComponentCollector {
   private final List<FormattedText> parts = Lists.newArrayList();

   public void append(final FormattedText component) {
      this.parts.add(component);
   }

   public @Nullable FormattedText getResult() {
      if (this.parts.isEmpty()) {
         return null;
      } else {
         return this.parts.size() == 1 ? this.parts.get(0) : FormattedText.composite(this.parts);
      }
   }

   public FormattedText getResultOrEmpty() {
      FormattedText result = this.getResult();
      return result != null ? result : FormattedText.EMPTY;
   }

   public void reset() {
      this.parts.clear();
   }
}
