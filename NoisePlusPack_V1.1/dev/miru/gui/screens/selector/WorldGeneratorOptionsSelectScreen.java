package dev.miru.gui.screens.selector;

import dev.miru.gui.screens.config.NoiseModeScreen;
import dev.miru.gui.screens.config.TerrainArgumentTriggerScreen;
import dev.miru.helper.KitUtil;
import dev.miru.main.ModMain;
import dev.miru.options.TppSettings;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class WorldGeneratorOptionsSelectScreen extends Screen {
   private Screen parent;
   private TppSettings tppSettings;

   public WorldGeneratorOptionsSelectScreen(Screen parent, TppSettings tppSettings) {
      super(Component.empty());
      this.parent = parent;
      this.tppSettings = tppSettings;
   }

   @Override
   public void init() {
      Button btnTerrainArgs = KitUtil.button(
         Component.literal(ModMain.getI18N("selector.terrain_arguments.title")),
         Component.literal(ModMain.getI18N("selector.terrain_arguments.hint")),
         btn -> this.minecraft.setScreen(new TerrainArgumentTriggerScreen(this, this.tppSettings)),
         200,
         20,
         this.width / 2 - 100,
         20
      );
      Button btnModeSwitcher = KitUtil.button(
         Component.literal(ModMain.getI18N("selector.terrain_modes.title")),
         Component.literal(ModMain.getI18N("selector.terrain_modes.hint")),
         btn -> this.minecraft.setScreen(new NoiseModeScreen(this, this.tppSettings)),
         200,
         20,
         this.width / 2 - 100,
         50
      );
      Button btnBack = KitUtil.button(
         CommonComponents.GUI_BACK, Component.empty(), btn -> this.minecraft.setScreen(this.parent), 200, 20, this.width / 2 - 100, this.height - 30
      );
      this.addRenderableWidget(btnTerrainArgs);
      this.addRenderableWidget(btnModeSwitcher);
      this.addRenderableWidget(btnBack);
   }
}
