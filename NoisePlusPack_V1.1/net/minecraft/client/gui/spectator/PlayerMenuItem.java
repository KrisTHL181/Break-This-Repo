package net.minecraft.client.gui.spectator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundTeleportToEntityPacket;
import net.minecraft.util.ARGB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PlayerMenuItem implements SpectatorMenuItem {
   private final PlayerInfo playerInfo;
   private final Component name;

   public PlayerMenuItem(PlayerInfo p_423312_) {
      this.playerInfo = p_423312_;
      this.name = Component.literal(p_423312_.getProfile().name());
   }

   @Override
   public void selectItem(SpectatorMenu p_101762_) {
      Minecraft.getInstance().getConnection().send(new ServerboundTeleportToEntityPacket(this.playerInfo.getProfile().id()));
   }

   @Override
   public Component getName() {
      return this.name;
   }

   @Override
   public void renderIcon(GuiGraphics p_282282_, float p_282686_, float p_368587_) {
      PlayerFaceRenderer.draw(p_282282_, this.playerInfo.getSkin(), 2, 2, 12, ARGB.white(p_368587_));
   }

   @Override
   public boolean isEnabled() {
      return true;
   }
}
