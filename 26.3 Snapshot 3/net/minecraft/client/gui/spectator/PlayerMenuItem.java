package net.minecraft.client.gui.spectator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundTeleportToEntityPacket;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.GameType;

public class PlayerMenuItem implements SpectatorMenuItem {
   private final PlayerInfo playerInfo;
   private final Component name;

   public PlayerMenuItem(final PlayerInfo playerInfo) {
      this.playerInfo = playerInfo;
      this.name = Component.literal(playerInfo.getProfile().name());
   }

   @Override
   public void selectItem(final SpectatorMenu menu) {
      if (this.isEnabled()) {
         Minecraft.getInstance().getConnection().send(new ServerboundTeleportToEntityPacket(this.playerInfo.getProfile().id()));
      }
   }

   @Override
   public Component getName() {
      return this.name;
   }

   @Override
   public void extractIcon(final GuiGraphicsExtractor graphics, final float brightness, final float alpha) {
      PlayerFaceExtractor.extractRenderState(graphics, this.playerInfo.getSkin(), 2, 2, 12, ARGB.white(alpha));
   }

   @Override
   public boolean isEnabled() {
      return this.playerInfo.getGameMode() != GameType.SPECTATOR;
   }
}
