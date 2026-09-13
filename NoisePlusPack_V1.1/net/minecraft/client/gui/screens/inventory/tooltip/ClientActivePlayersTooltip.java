package net.minecraft.client.gui.screens.inventory.tooltip;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientActivePlayersTooltip implements ClientTooltipComponent {
   private static final int SKIN_SIZE = 10;
   private static final int PADDING = 2;
   private final List<PlayerSkinRenderCache.RenderInfo> activePlayers;

   public ClientActivePlayersTooltip(ClientActivePlayersTooltip.ActivePlayersTooltip p_344514_) {
      this.activePlayers = p_344514_.profiles();
   }

   @Override
   public int getHeight(Font p_367830_) {
      return this.activePlayers.size() * 12 + 2;
   }

   private static String getName(PlayerSkinRenderCache.RenderInfo p_429323_) {
      return p_429323_.gameProfile().name();
   }

   @Override
   public int getWidth(Font p_345139_) {
      int i = 0;

      for (PlayerSkinRenderCache.RenderInfo playerskinrendercache$renderinfo : this.activePlayers) {
         int j = p_345139_.width(getName(playerskinrendercache$renderinfo));
         if (j > i) {
            i = j;
         }
      }

      return i + 10 + 6;
   }

   @Override
   public void renderImage(Font p_342274_, int p_345290_, int p_342557_, int p_361924_, int p_360967_, GuiGraphics p_345309_) {
      for (int i = 0; i < this.activePlayers.size(); i++) {
         PlayerSkinRenderCache.RenderInfo playerskinrendercache$renderinfo = this.activePlayers.get(i);
         int j = p_342557_ + 2 + i * 12;
         PlayerFaceRenderer.draw(p_345309_, playerskinrendercache$renderinfo.playerSkin(), p_345290_ + 2, j, 10);
         p_345309_.drawString(p_342274_, getName(playerskinrendercache$renderinfo), p_345290_ + 10 + 4, j + 2, -1);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record ActivePlayersTooltip(List<PlayerSkinRenderCache.RenderInfo> profiles) implements TooltipComponent {
   }
}
