package dev.miru.gui.screens.selector;

import dev.miru.gui.screens.config.ImportExportScreen;
import dev.miru.helper.KitUtil;
import dev.miru.main.ModMain;
import dev.miru.options.TppSettings;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class TppUtilSelectScreen extends Screen {
   private Screen parent;
   private TppSettings tppSettings;
   private Player player;

   public TppUtilSelectScreen(Screen parent, TppSettings tppSettings, Player player) {
      super(Component.empty());
      this.parent = parent;
      this.tppSettings = tppSettings;
      this.player = player;
   }

   @Override
   public void init() {
      Button btnPositionLocator = KitUtil.button(
         Component.literal(ModMain.getI18N("selector.position_and_teleport.title")),
         Component.literal(ModMain.getI18N("selector.position_and_teleport.hint")),
         btn -> this.minecraft.setScreen(new PositionAndTeleportSelectScreen(this, this.tppSettings, this.player)),
         200,
         20,
         this.width / 2 - 100,
         20
      );
      Button btnImportExport = KitUtil.button(
         Component.literal(ModMain.getI18N("screen.import_export.title")),
         Component.literal(ModMain.getI18N("screen.import_export.hint")),
         btn -> this.minecraft.setScreen(new ImportExportScreen(this)),
         200,
         20,
         this.width / 2 - 100,
         50
      );
      Button btnBack = KitUtil.button(
         CommonComponents.GUI_BACK, Component.empty(), btn -> this.minecraft.setScreen(this.parent), 200, 20, this.width / 2 - 100, this.height - 30
      );
      this.addRenderableWidget(btnPositionLocator);
      this.addRenderableWidget(btnImportExport);
      this.addRenderableWidget(btnBack);
   }
}
