package net.minecraft.client.gui.screens.options;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FontOptionsScreen extends OptionsSubScreen {
   private static OptionInstance<?>[] options(Options p_342475_) {
      return new OptionInstance[]{p_342475_.forceUnicodeFont(), p_342475_.japaneseGlyphVariants()};
   }

   public FontOptionsScreen(Screen p_345386_, Options p_343880_) {
      super(p_345386_, p_343880_, Component.translatable("options.font.title"));
   }

   @Override
   protected void addOptions() {
      this.list.addSmall(options(this.options));
   }
}
