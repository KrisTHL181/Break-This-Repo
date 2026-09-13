package dev.miru.gui.screens.selector;

import dev.miru.gui.screens.utils.PositionLocateScreen;
import dev.miru.gui.screens.utils.QuickTeleportScreen;
import dev.miru.helper.KitUtil;
import dev.miru.main.ModMain;
import dev.miru.options.TppSettings;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class PositionAndTeleportSelectScreen extends Screen {
   private Screen parent;
   private Player player;
   private TppSettings tppSettings;
   private boolean isRevApply = true;

   public PositionAndTeleportSelectScreen(Screen parent, TppSettings tppSettings, Player player) {
      super(Component.empty());
      this.parent = parent;
      this.player = player;
      this.tppSettings = tppSettings;
   }

   @Override
   public void init() {
      Button btnPositionLocator = KitUtil.button(
         Component.literal(ModMain.getI18N("selector.position_locator.title")),
         Component.literal(ModMain.getI18N("selector.position_locator.hint")),
         btn -> this.minecraft.setScreen(new PositionLocateScreen(this, this.tppSettings)),
         200,
         20,
         this.width / 2 - 100,
         20
      );
      Button btnTpPlayer = KitUtil.button(
         Component.literal(ModMain.getI18N("selector.teleport.title")), Component.literal(ModMain.getI18N("selector.teleport.hint")), btn -> {
            if (this.player == null) {
               btn.setTooltip(Tooltip.create(Component.literal(ModMain.getI18N("selector.no_player_available"))));
               btn.active = false;
            } else {
               this.minecraft.setScreen(new QuickTeleportScreen(this, this.player));
            }
         }, 200, 20, this.width / 2 - 100, 50
      );
      Button btnBack = KitUtil.button(
         CommonComponents.GUI_BACK, Component.empty(), btn -> this.minecraft.setScreen(this.parent), 200, 20, this.width / 2 - 100, this.height - 30
      );
      this.addRenderableWidget(btnPositionLocator);
      this.addRenderableWidget(btnTpPlayer);
      this.addRenderableWidget(btnBack);
   }
}
