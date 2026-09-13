package dev.miru.gui.screens.utils;

import dev.miru.helper.KitUtil;
import dev.miru.main.ModMain;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class QuickTeleportScreen extends Screen {
   private Screen parent;
   private Player player;
   private EditBox tpX;
   private EditBox tpY;
   private EditBox tpZ;
   private Button btnTpX;
   private Button btnTpY;
   private Button btnTpZ;
   private Button btnTp;
   private Button btnBack;

   public QuickTeleportScreen(Screen parent, Player player) {
      super(Component.empty());
      this.parent = parent;
      this.player = player;
   }

   @Override
   public void init() {
      double playerX = this.player.getX();
      double playerY = this.player.getY();
      double playerZ = this.player.getZ();
      this.tpX = KitUtil.editbox(this.font, "", Component.literal(ModMain.getI18N("screen.quick_teleport.target_x")), 150, 20, this.width / 2 - 100, 50);
      this.tpY = KitUtil.editbox(this.font, "", Component.literal(ModMain.getI18N("screen.quick_teleport.target_y")), 150, 20, this.width / 2 - 100, 80);
      this.tpZ = KitUtil.editbox(this.font, "", Component.literal(ModMain.getI18N("screen.quick_teleport.target_z")), 150, 20, this.width / 2 - 100, 110);
      this.btnTpX = KitUtil.button(
         Component.literal(ModMain.getI18N("screen.quick_teleport.go")),
         Component.literal(ModMain.getI18N("screen.quick_teleport.teleport_x_only")),
         btn -> this.player.setPos(Double.parseDouble(this.tpX.getValue()), playerY, playerZ),
         40,
         20,
         this.width / 2 + 60,
         50
      );
      this.btnTpY = KitUtil.button(
         Component.literal(ModMain.getI18N("screen.quick_teleport.go")),
         Component.literal(ModMain.getI18N("screen.quick_teleport.teleport_y_only")),
         btn -> this.player.setPos(playerX, Double.parseDouble(this.tpY.getValue()), playerZ),
         40,
         20,
         this.width / 2 + 60,
         80
      );
      this.btnTpZ = KitUtil.button(
         Component.literal(ModMain.getI18N("screen.quick_teleport.go")),
         Component.literal(ModMain.getI18N("screen.quick_teleport.teleport_z_only")),
         btn -> this.player.setPos(playerX, playerY, Double.parseDouble(this.tpZ.getValue())),
         40,
         20,
         this.width / 2 + 60,
         110
      );
      this.btnTp = KitUtil.button(
         Component.literal(ModMain.getI18N("screen.quick_teleport.to_position")),
         Component.empty(),
         btn -> this.player.setPos(Double.parseDouble(this.tpX.getValue()), Double.parseDouble(this.tpY.getValue()), Double.parseDouble(this.tpZ.getValue())),
         98,
         20,
         this.width / 2 - 100,
         140
      );
      this.btnBack = KitUtil.button(CommonComponents.GUI_BACK, Component.empty(), btn -> this.minecraft.setScreen(this.parent), 98, 20, this.width / 2 + 2, 140);
      this.addRenderableWidget(this.tpX);
      this.addRenderableWidget(this.tpY);
      this.addRenderableWidget(this.tpZ);
      this.addRenderableWidget(this.btnTpX);
      this.addRenderableWidget(this.btnTpY);
      this.addRenderableWidget(this.btnTpZ);
      this.addRenderableWidget(this.btnTp);
      this.addRenderableWidget(this.btnBack);
   }

   @Override
   public void tick() {
      boolean xValid = false;
      boolean yValid = false;
      boolean zValid = false;
      boolean hasPlayer = this.player != null;
      if (hasPlayer && this.btnTpX != null && this.tpX != null) {
         xValid = this.btnTpX.active = isValidNumber(this.tpX.getValue());
      }

      if (hasPlayer && this.btnTpY != null && this.tpY != null) {
         yValid = this.btnTpY.active = isValidNumber(this.tpY.getValue());
      }

      if (hasPlayer && this.btnTpZ != null && this.tpZ != null) {
         zValid = this.btnTpZ.active = isValidNumber(this.tpZ.getValue());
      }

      this.btnTp.active = hasPlayer && xValid && yValid && zValid;
   }

   private static boolean isValidNumber(String string) {
      try {
         Double.parseDouble(string);
         return true;
      } catch (NumberFormatException e) {
         return false;
      }
   }
}
