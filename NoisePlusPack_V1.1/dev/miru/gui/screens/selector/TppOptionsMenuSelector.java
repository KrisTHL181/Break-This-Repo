package dev.miru.gui.screens.selector;

import dev.miru.gui.screens.config.AdvancedOptionsScreen;
import dev.miru.helper.KitUtil;
import dev.miru.main.ModMain;
import dev.miru.options.TppSettings;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class TppOptionsMenuSelector extends Screen {
   private Screen parent;
   private TppSettings tppSettings;

   public TppOptionsMenuSelector(Screen parent, TppSettings tppSettings) {
      super(Component.empty());
      this.parent = parent;
      this.tppSettings = tppSettings;
   }

   @Override
   public void init() {
      Button btnWorldGenOptions = KitUtil.button(
         Component.literal(ModMain.getI18N("selector.world_generator_settings.title")),
         Component.literal(ModMain.getI18N("selector.world_generator_settings.hint")),
         btn -> this.minecraft.setScreen(new WorldGeneratorOptionsSelectScreen(this, this.tppSettings)),
         200,
         20,
         this.width / 2 - 100,
         20
      );
      Button btnAdvancedOptions = KitUtil.button(
         Component.literal(ModMain.getI18N("selector.advanced_options.title")),
         Component.literal(ModMain.getI18N("selector.advanced_options.hint")),
         btn -> this.minecraft.setScreen(new AdvancedOptionsScreen(this, this.tppSettings)),
         200,
         20,
         this.width / 2 - 100,
         50
      );
      Button btnBack = KitUtil.button(
         Component.translatable("gui.back"), Component.empty(), btn -> this.minecraft.setScreen(this.parent), 200, 20, this.width / 2 - 100, this.height - 40
      );
      this.addRenderableWidget(btnWorldGenOptions);
      this.addRenderableWidget(btnAdvancedOptions);
      this.addRenderableWidget(btnBack);
   }
}
