package net.minecraft.client.gui.screens.inventory.tooltip;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public class ClientActivePlayersTooltip implements ClientTooltipComponent {
   private static final int SKIN_SIZE = 10;
   private static final int PADDING = 2;
   private final List<PlayerSkinRenderCache.RenderInfo> activePlayers;

   public ClientActivePlayersTooltip(final ClientActivePlayersTooltip.ActivePlayersTooltip activePlayersTooltip) {
      this.activePlayers = activePlayersTooltip.profiles();
   }

   @Override
   public int getHeight(final Font font) {
      return this.activePlayers.size() * 12 + 2;
   }

   private static String getName(final PlayerSkinRenderCache.RenderInfo activePlayer) {
      return activePlayer.gameProfile().name();
   }

   @Override
   public int getWidth(final Font font) {
      int widest = 0;

      for (PlayerSkinRenderCache.RenderInfo activePlayer : this.activePlayers) {
         int width = font.width(getName(activePlayer));
         if (width > widest) {
            widest = width;
         }
      }

      return widest + 10 + 6;
   }

   @Override
   public void extractImage(final Font font, final int x, final int y, final int w, final int h, final GuiGraphicsExtractor graphics) {
      for (int i = 0; i < this.activePlayers.size(); i++) {
         PlayerSkinRenderCache.RenderInfo activePlayer = this.activePlayers.get(i);
         int y1 = y + 2 + i * 12;
         PlayerFaceExtractor.extractRenderState(graphics, activePlayer.playerSkin(), x + 2, y1, 10);
         graphics.text(font, getName(activePlayer), x + 10 + 4, y1 + 2, -1);
      }
   }

   public record ActivePlayersTooltip(List<PlayerSkinRenderCache.RenderInfo> profiles) implements TooltipComponent {
   }
}
