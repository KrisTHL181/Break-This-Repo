package dev.miru.gui.screens.config;

import dev.miru.gui.screens.selector.TppOptionsMenuSelector;
import dev.miru.gui.screens.selector.TppUtilSelectScreen;
import dev.miru.helper.KitUtil;
import dev.miru.main.ModMain;
import dev.miru.options.TppSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class GeneralSettingScreen extends Screen {
   private Screen parent;
   private TppSettings tppSettings;

   public GeneralSettingScreen(Screen parent, TppSettings tppSettings) {
      super(Component.empty());
      this.parent = parent;
      this.tppSettings = tppSettings;
   }

   @Override
   public void init() {
      Button btnModOptions = KitUtil.button(
         Component.literal(ModMain.getI18N("screen.general_settings.title")),
         Component.literal(ModMain.getI18N("screen.general_settings.hint")),
         btn -> this.minecraft.setScreen(new TppOptionsMenuSelector(this, this.tppSettings)),
         200,
         20,
         this.width / 2 - 100,
         60
      );
      Button btnOptionsUtils = KitUtil.button(
         Component.literal(ModMain.getI18N("screen.general_settings.tools.title")),
         Component.literal(ModMain.getI18N("screen.general_settings.tools.hint")),
         btn -> this.minecraft.setScreen(new TppUtilSelectScreen(this, this.tppSettings, this.minecraft.player)),
         200,
         20,
         this.width / 2 - 100,
         90
      );
      Button btnReset = KitUtil.button(
         Component.literal(ModMain.getI18N("options.reset")),
         Component.literal(ModMain.getI18N("options.reset.hint")),
         btn -> this.minecraft
            .setScreen(
               new ConfirmScreen(
                  select -> {
                     if (select) {
                        ModMain.resetSettings();
                     } else {
                        this.minecraft.setScreen(this);
                     }
                  },
                  Component.literal(ModMain.getI18N("screen.general_settings.reset.confirm")).withStyle(ChatFormatting.YELLOW),
                  Component.literal(ModMain.getI18N("screen.general_settings.reset.irreversible")).withColor(-65536),
                  CommonComponents.GUI_OK,
                  CommonComponents.GUI_NO
               )
            ),
         200,
         20,
         this.width / 2 - 100,
         120
      );
      Button btnLanguage = KitUtil.button(
         Component.literal(ModMain.getI18N("screen.language_switcher.title")),
         Component.empty(),
         btn -> this.minecraft.setScreen(new LanguageSwitchScreen(this, ModMain.getTranslateManager())),
         200,
         20,
         this.width / 2 - 100,
         150
      );
      Button btnBack = KitUtil.button(
         CommonComponents.GUI_BACK, Component.empty(), btn -> this.minecraft.setScreen(this.parent), 200, 20, this.width / 2 - 100, this.height - 30
      );
      this.addRenderableWidget(btnModOptions);
      this.addRenderableWidget(btnOptionsUtils);
      this.addRenderableWidget(btnReset);
      this.addRenderableWidget(btnLanguage);
      this.addRenderableWidget(btnBack);
   }

   @Override
   public void render(GuiGraphics guiGraphics, int mx, int my, float delta) {
      super.render(guiGraphics, mx, my, delta);
      guiGraphics.drawCenteredString(this.font, ModMain.getI18N("title.mod.settings.guide.title"), this.width / 2, 30, -1);
   }
}
