package dev.miru.gui.screens.config;

import dev.miru.helper.KitUtil;
import dev.miru.main.ModMain;
import dev.miru.options.TppSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ImportExportScreen extends Screen {
   private final Screen parent;
   private MultiLineEditBox inputBox;
   private Button importBtn;

   public ImportExportScreen(Screen parent) {
      super(Component.literal(ModMain.getI18N("screen.import_export.title")));
      this.parent = parent;
   }

   @Override
   public void init() {
      super.init();
      MultiLineEditBox optionsInputBox = KitUtil.multilineeditBox(
         this.font, this.width / 2 - 150, 40, 300, 260, -1, -16711936, false, true, true, Component.empty(), Component.empty()
      );
      optionsInputBox.setValue(ModMain.getOptions().toString());
      this.inputBox = optionsInputBox;
      Button importButton;
      this.importBtn = importButton = KitUtil.button(
         Component.literal(ModMain.getI18N("screen.import_export.import")), Component.literal(ModMain.getI18N("screen.import_export.import.hint")), btn -> {
            String raw = optionsInputBox.getValue();
            ModMain.writeSettings(TppSettings.parseOptions(raw));
         }, 200, 20, this.width / 2 - 100, 305
      );
      Button exportButton = KitUtil.button(
         Component.literal(ModMain.getI18N("screen.import_export.export")),
         Component.literal(ModMain.getI18N("screen.import_export.export.hint")),
         btn -> optionsInputBox.setValue(ModMain.getOptions().toString()),
         200,
         20,
         this.width / 2 - 100,
         330
      );
      Button backButton = KitUtil.button(Component.literal(ModMain.getI18N("options.cancel")), null, btn -> this.onClose(), 200, 20, this.width / 2 - 100, 355);
      this.addRenderableWidget(optionsInputBox);
      this.addRenderableWidget(importButton);
      this.addRenderableWidget(exportButton);
      this.addRenderableWidget(backButton);
   }

   @Override
   public void render(GuiGraphics gg, int mx, int my, float f) {
      super.render(gg, mx, my, f);
      String title = ModMain.getI18N("screen.import_export.title");
      gg.drawString(this.font, title, this.width / 2 - this.font.width(title) / 2, 20, -1);
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.parent);
   }

   @Override
   public void tick() {
      if (this.importBtn != null) {
         this.importBtn.active = !this.inputBox.getValue().isBlank();
      }
   }
}
