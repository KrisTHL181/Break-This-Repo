package dev.miru.gui.screens.config;

import dev.miru.gui.screens.utils.QuickLinkScreen;
import dev.miru.helper.KitUtil;
import dev.miru.main.ModMain;
import dev.miru.options.TppSettings;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class AdvancedOptionsScreen extends Screen {
   private Screen parent;
   private TppSettings tppSettings;

   public AdvancedOptionsScreen(Screen parent, TppSettings tppSettings) {
      super(Component.empty());
      this.parent = parent;
      this.tppSettings = tppSettings;
   }

   @Override
   public void init() {
      Button btnDebugHud = KitUtil.button(
         Component.literal(this.tppSettings.getExpd_debugOverlay().toString()),
         Component.literal(ModMain.getI18N("screen.advanced_options.toggle_debug_hud")),
         btn -> {
            this.tppSettings.getExpd_debugOverlay().toggle();
            btn.setMessage(Component.literal(this.tppSettings.getExpd_debugOverlay().toString()));
         },
         200,
         20,
         this.width / 2 - 100,
         40
      );
      Button btnReportLevelAsDebug = KitUtil.button(
         Component.literal(this.tppSettings.getExpd_reportWorldAsDebugWorld().toString()),
         Component.literal(this.tppSettings.getExpd_reportWorldAsDebugWorldValue().getHint()),
         btn -> {
            this.tppSettings.getExpd_reportWorldAsDebugWorld().toggle();
            btn.setMessage(Component.literal(this.tppSettings.getExpd_reportWorldAsDebugWorld().toString()));
            btn.setTooltip(KitUtil.tooltip(this.tppSettings.getExpd_reportWorldAsDebugWorldValue().getHint()));
         },
         200,
         20,
         this.width / 2 - 100,
         60
      );
      Button btnEnableEnchantedPauseScreen = KitUtil.button(
         this.tppSettings.getExpd_enableEnhancedPauseScreen().toComponent(),
         Component.literal(ModMain.getI18N("screen.advanced_options.toggle_enchanted_pause")),
         btn -> {
            this.tppSettings.getExpd_enableEnhancedPauseScreen().toggle();
            btn.setMessage(this.tppSettings.getExpd_enableEnhancedPauseScreen().toComponent());
         },
         200,
         20,
         this.width / 2 - 100,
         80
      );
      Button btnDemoMode = KitUtil.button(
         Component.literal(this.tppSettings.getExpd_demoMode().toString()),
         Component.literal(ModMain.getI18N("screen.advanced_options.toggle_demo_mode")),
         btn -> {
            this.tppSettings.getExpd_demoMode().toggle();
            btn.setMessage(Component.literal(this.tppSettings.getExpd_demoMode().toString()));
         },
         200,
         20,
         this.width / 2 - 100,
         100
      );
      Button btnToolBar = KitUtil.button(
         this.tppSettings.getExpd_enableToolBar().toComponent(), Component.literal(ModMain.getI18N("screen.advanced_options.toggle_toolbar")), btn -> {
            this.tppSettings.getExpd_enableToolBar().toggle();
            btn.setMessage(this.tppSettings.getExpd_enableToolBar().toComponent());
         }, 200, 20, this.width / 2 - 100, 120
      );
      Button btnOperateMenu = KitUtil.button(
         this.tppSettings.getEnpd_enableOperateMenu().toComponent(),
         Component.literal(ModMain.getI18N("screen.advanced_options.toggle_operate_menu")),
         btn -> {
            this.tppSettings.getEnpd_enableOperateMenu().toggle();
            btn.setMessage(this.tppSettings.getEnpd_enableOperateMenu().toComponent());
         },
         200,
         20,
         this.width / 2 - 100,
         140
      );
      Button btnNoRealmsErrScreen = KitUtil.button(
         this.tppSettings.getExpd_noRealmsErrorScreen().toComponent(),
         Component.literal(ModMain.getI18N("screen.advanced_options.disable_realms_error")),
         btn -> {
            this.tppSettings.getExpd_noRealmsErrorScreen().toggle();
            btn.setMessage(this.tppSettings.getExpd_noRealmsErrorScreen().toComponent());
         },
         200,
         20,
         this.width / 2 - 100,
         160
      );
      Button btnQuickLink = KitUtil.button(
         Component.literal(ModMain.getI18N("screen.advanced_options.quick_link")),
         Component.literal(ModMain.getI18N("screen.advanced_options.quick_link.hint")),
         btn -> this.minecraft.setScreen(new QuickLinkScreen(this)),
         200,
         20,
         this.width / 2 - 100,
         this.height - 60
      );
      Button btnBack = KitUtil.button(
         Component.translatable("gui.back"),
         Component.literal(ModMain.getI18N("screen.advanced_options.back_last_screen")),
         btn -> this.minecraft.setScreen(this.parent),
         200,
         20,
         this.width / 2 - 100,
         this.height - 30
      );
      this.addRenderableWidget(btnDebugHud);
      this.addRenderableWidget(btnReportLevelAsDebug);
      this.addRenderableWidget(btnEnableEnchantedPauseScreen);
      this.addRenderableWidget(btnDemoMode);
      this.addRenderableWidget(btnToolBar);
      this.addRenderableWidget(btnOperateMenu);
      this.addRenderableWidget(btnNoRealmsErrScreen);
      this.addRenderableWidget(btnQuickLink);
      this.addRenderableWidget(btnBack);
   }
}
