package dev.miru.gui.screens.utils;

import dev.miru.helper.KitUtil;
import dev.miru.main.ModMain;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.debug.DebugOptionsScreen;
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen;
import net.minecraft.network.chat.Component;

public class QuickLinkScreen extends Screen {
   private Screen parent;

   public QuickLinkScreen(Screen parent) {
      super(Component.literal(ModMain.getI18N("screen.quick_link.title")));
      this.parent = parent;
   }

   @Override
   public void init() {
      Button btnF3Editor = KitUtil.button(
         Component.literal(ModMain.getI18N("screen.quick_link.open_debug_overlay_editor")),
         Component.literal(ModMain.getI18N("screen.quick_link.access_key")),
         btn -> this.minecraft.setScreen(new DebugOptionsScreen()),
         300,
         20,
         this.width / 2 - 150,
         30
      );
      Button btnSafetyScreen = KitUtil.button(
         Component.literal(ModMain.getI18N("screen.quick_link.open_multiplayer_safety")),
         Component.literal(ModMain.getI18N("screen.quick_link.hide_after_trigger")),
         btn -> this.minecraft.setScreen(new SafetyScreen(this)),
         300,
         20,
         this.width / 2 - 150,
         60
      );
      Button btnBack = KitUtil.button(
         Component.translatable("gui.back"), Component.empty(), btn -> this.minecraft.setScreen(this.parent), 300, 20, this.width / 2 - 150, this.height - 30
      );
      this.addRenderableWidget(btnF3Editor);
      this.addRenderableWidget(btnSafetyScreen);
      this.addRenderableWidget(btnBack);
   }
}
