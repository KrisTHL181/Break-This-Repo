package net.minecraft.network.chat.contents;

import java.util.Locale;

public class TranslatableFormatException extends IllegalArgumentException {
   public TranslatableFormatException(TranslatableContents p_237533_, String p_237534_) {
      super(String.format(Locale.ROOT, "Error parsing: %s: %s", p_237533_, p_237534_));
   }

   public TranslatableFormatException(TranslatableContents p_237530_, int p_237531_) {
      super(String.format(Locale.ROOT, "Invalid index %d requested for %s", p_237531_, p_237530_));
   }

   public TranslatableFormatException(TranslatableContents p_237536_, Throwable p_237537_) {
      super(String.format(Locale.ROOT, "Error while parsing: %s", p_237536_), p_237537_);
   }
}
