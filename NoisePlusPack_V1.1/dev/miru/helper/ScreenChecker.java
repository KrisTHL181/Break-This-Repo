package dev.miru.helper;

import dev.miru.gui.screens.config.ImportExportScreen;
import dev.miru.gui.screens.config.TerrainArgumentTriggerScreen;
import dev.miru.gui.toolbar.ToolbarBuilder;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;

public class ScreenChecker {
   private final Screen screen;

   public ScreenChecker(Screen screen) {
      this.screen = screen;
   }

   public boolean isNullScreen() {
      return this.screen == null;
   }

   public boolean hasComponents() {
      return this.isNullScreen() ? true : !this.screen.renderables.isEmpty();
   }

   public boolean typeEquals(Class<?> screenClass) {
      return screenClass.equals(this.screen.getClass());
   }

   public boolean hasComponentInType(Class<?> type) {
      for (GuiEventListener component : this.screen.children()) {
         if (type.equals(component.getClass())) {
            return true;
         }
      }

      for (Renderable component : this.screen.renderables) {
         if (type.equals(component.getClass())) {
            return true;
         }
      }

      return false;
   }

   public boolean isTypeableScreen() {
      return this.typeEquals(ChatScreen.class)
         || this.typeEquals(CreateWorldScreen.class)
         || this.typeEquals(TerrainArgumentTriggerScreen.class)
         || this.typeEquals(ImportExportScreen.class)
         || this.typeEquals(TitleScreen.class)
         || this.hasComponentInType(EditBox.class)
         || this.hasComponentInType(MultiLineEditBox.class);
   }

   public boolean hasToolBarItems(ToolbarBuilder toolbarBuilder) {
      return !toolbarBuilder.toolBarButtonList.isEmpty();
   }
}
