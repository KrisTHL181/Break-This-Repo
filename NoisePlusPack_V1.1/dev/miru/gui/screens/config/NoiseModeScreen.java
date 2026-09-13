package dev.miru.gui.screens.config;

import dev.miru.helper.KitUtil;
import dev.miru.main.ModMain;
import dev.miru.options.TppSettings;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class NoiseModeScreen extends Screen {
   private Screen parent;
   private TppSettings tppSettings;

   public NoiseModeScreen(Screen parent, TppSettings tppSettings) {
      super(Component.literal(ModMain.getI18N("screen.noise_mode_trigger.title")));
      this.parent = parent;
      this.tppSettings = tppSettings;
   }

   @Override
   public void init() {
      Button btnModifyMode = KitUtil.button(
         this.tppSettings.getWgen_modifyMode().toComponent(), Component.literal(this.tppSettings.getWgen_modifyModeValue().getHint()), btn -> {
            this.tppSettings.getWgen_modifyMode().toggle();
            btn.setMessage(this.tppSettings.getWgen_modifyMode().toComponent());
            btn.setTooltip(Tooltip.create(Component.literal(this.tppSettings.getWgen_modifyModeValue().getHint())));
         }, 300, 20, this.width / 2 - 150, 40
      );
      Button btnWarpMode = KitUtil.button(
         this.tppSettings.getWgen_wrapMode().toComponent(), Component.literal(this.tppSettings.getWgen_wrapModeValue().getHint()), btn -> {
            this.tppSettings.getWgen_wrapMode().toggle();
            btn.setMessage(this.tppSettings.getWgen_wrapMode().toComponent());
            btn.setTooltip(Tooltip.create(Component.literal(this.tppSettings.getWgen_wrapModeValue().getHint())));
         }, 300, 20, this.width / 2 - 150, 70
      );
      Button btnLerpMode = KitUtil.button(
         this.tppSettings.getWgen_lerpMode().toComponent(), Component.literal(this.tppSettings.getWgen_lerpModeValue().getHint()), btn -> {
            this.tppSettings.getWgen_lerpMode().toggle();
            btn.setMessage(this.tppSettings.getWgen_lerpMode().toComponent());
            btn.setTooltip(Tooltip.create(Component.literal(this.tppSettings.getWgen_lerpModeValue().getHint())));
         }, 300, 20, this.width / 2 - 150, 100
      );
      Button btnBack = KitUtil.button(
         CommonComponents.GUI_BACK, Component.empty(), btn -> this.minecraft.setScreen(this.parent), 300, 20, this.width / 2 - 150, this.height - 30
      );
      this.addRenderableWidget(btnModifyMode);
      this.addRenderableWidget(btnWarpMode);
      this.addRenderableWidget(btnLerpMode);
      this.addRenderableWidget(btnBack);
   }
}
