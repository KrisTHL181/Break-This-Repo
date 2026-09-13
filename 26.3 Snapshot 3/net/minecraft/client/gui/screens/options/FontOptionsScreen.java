package net.minecraft.client.gui.screens.options;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class FontOptionsScreen extends OptionsSubScreen {
   private static OptionInstance<?>[] options(final Options options) {
      return new OptionInstance[]{options.forceUnicodeFont(), options.japaneseGlyphVariants()};
   }

   public FontOptionsScreen(final Screen lastScreen, final Options options) {
      super(lastScreen, options, Component.translatable("options.font.title"));
   }

   @Override
   protected void addOptions() {
      this.list.addSmall(options(this.options));
   }
}
