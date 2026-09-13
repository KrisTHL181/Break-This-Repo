package dev.miru.gui.screens.config;

import dev.miru.helper.KitUtil;
import dev.miru.localization.TppTranslateManager;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class LanguageSwitchScreen extends Screen {
   private Screen parent;
   private TppTranslateManager translateManager;

   public LanguageSwitchScreen(Screen parent, TppTranslateManager translateManager) {
      super(Component.literal(translateManager.getI18N("screen.language_switcher.title")));
      this.parent = parent;
      this.translateManager = translateManager;
   }

   @Override
   public void init() {
      int index = 0;

      for (String code : this.translateManager.getDisplayNames().keySet()) {
         String displayName = this.translateManager.getDisplayNames().get(code);
         Button button = KitUtil.button(Component.literal(displayName), Component.empty(), btn -> {
            this.translateManager.setLanguageCode(code);
            this.minecraft.setScreen(this.parent);
         }, 200, 20, this.width / 2 - 100, 40 + 20 * index);
         this.addRenderableWidget(button);
         index++;
      }

      this.addRenderableWidget(
         KitUtil.button(Component.literal(this.translateManager.getI18N("screen.language_switcher.sync_game")), Component.empty(), btn -> {
            this.translateManager.syncGameLanguage(this.minecraft);
            this.minecraft.setScreen(this.parent);
         }, 200, 20, this.width / 2 - 100, 20)
      );
   }
}
