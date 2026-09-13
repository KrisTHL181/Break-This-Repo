package net.minecraft.client.gui.screens.options;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ChatOptionsScreen extends OptionsSubScreen {
   private static final Component TITLE = Component.translatable("options.chat.title");

   private static OptionInstance<?>[] options(Options p_343220_) {
      return new OptionInstance[]{
         p_343220_.chatVisibility(),
         p_343220_.chatColors(),
         p_343220_.chatLinks(),
         p_343220_.chatLinksPrompt(),
         p_343220_.chatOpacity(),
         p_343220_.textBackgroundOpacity(),
         p_343220_.chatScale(),
         p_343220_.chatLineSpacing(),
         p_343220_.chatDelay(),
         p_343220_.chatWidth(),
         p_343220_.chatHeightFocused(),
         p_343220_.chatHeightUnfocused(),
         p_343220_.narrator(),
         p_343220_.autoSuggestions(),
         p_343220_.hideMatchedNames(),
         p_343220_.reducedDebugInfo(),
         p_343220_.onlyShowSecureChat(),
         p_343220_.saveChatDrafts()
      };
   }

   public ChatOptionsScreen(Screen p_343002_, Options p_342782_) {
      super(p_343002_, p_342782_, TITLE);
   }

   @Override
   protected void addOptions() {
      this.list.addSmall(options(this.options));
   }
}
