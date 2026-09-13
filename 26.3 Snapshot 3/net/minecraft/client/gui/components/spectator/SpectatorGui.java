package net.minecraft.client.gui.components.spectator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.spectator.SpectatorMenu;
import net.minecraft.client.gui.spectator.SpectatorMenuItem;
import net.minecraft.client.gui.spectator.SpectatorMenuListener;
import net.minecraft.client.gui.spectator.categories.SpectatorPage;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public class SpectatorGui implements SpectatorMenuListener {
   private static final Identifier HOTBAR_SPRITE = Identifier.withDefaultNamespace("hud/hotbar");
   private static final Identifier HOTBAR_SELECTION_SPRITE = Identifier.withDefaultNamespace("hud/hotbar_selection");
   private static final long FADE_OUT_DELAY = 5000L;
   private static final long FADE_OUT_TIME = 2000L;
   private final Minecraft minecraft;
   private long lastSelectionTime;
   private @Nullable SpectatorMenu menu;

   public SpectatorGui(final Minecraft minecraft) {
      this.minecraft = minecraft;
   }

   public void onHotbarSelected(final int slot) {
      this.lastSelectionTime = Util.getMillis();
      if (this.menu != null) {
         this.menu.selectSlot(slot);
      } else {
         this.menu = new SpectatorMenu(this);
      }
   }

   private float getHotbarAlpha() {
      long delta = this.lastSelectionTime - Util.getMillis() + 5000L;
      return Mth.clamp((float)delta / 2000.0F, 0.0F, 1.0F);
   }

   public void extractHotbar(final GuiGraphicsExtractor graphics) {
      if (this.menu != null) {
         float alpha = this.getHotbarAlpha();
         if (alpha <= 0.0F) {
            this.menu.exit();
         } else {
            int screenCenter = graphics.guiWidth() / 2;
            int y = Mth.floor(graphics.guiHeight() - 22.0F * alpha);
            SpectatorPage page = this.menu.getCurrentPage();
            this.extractPage(graphics, alpha, screenCenter, y, page);
         }
      }
   }

   protected void extractPage(final GuiGraphicsExtractor graphics, final float alpha, final int screenCenter, final int y, final SpectatorPage page) {
      int color = ARGB.white(alpha);
      graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, screenCenter - 91, y, 182, 22, color);
      if (page.getSelectedSlot() >= 0) {
         graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SELECTION_SPRITE, screenCenter - 91 - 1 + page.getSelectedSlot() * 20, y - 1, 24, 23, color);
      }

      for (int slot = 0; slot < 9; slot++) {
         this.extractSlot(graphics, slot, graphics.guiWidth() / 2 - 90 + slot * 20 + 2, y + 3, alpha, page.getItem(slot));
      }
   }

   private void extractSlot(final GuiGraphicsExtractor graphics, final int slot, final int x, final float y, final float alpha, final SpectatorMenuItem item) {
      if (item != SpectatorMenu.EMPTY_SLOT) {
         graphics.pose().pushMatrix();
         graphics.pose().translate(x, y);
         float brightness = item.isEnabled() ? 1.0F : 0.25F;
         item.extractIcon(graphics, brightness, alpha);
         graphics.pose().popMatrix();
         if (alpha > 0.0F && item.isEnabled()) {
            Component key = this.minecraft.options.keyHotbarSlots[slot].getTranslatedKeyMessage();
            graphics.text(this.minecraft.font, key, x + 19 - 2 - this.minecraft.font.width(key), (int)y + 6 + 3, ARGB.white(alpha));
         }
      }
   }

   public void extractAction(final GuiGraphicsExtractor graphics) {
      float alpha = this.getHotbarAlpha();
      if (alpha > 0.0F && this.menu != null) {
         SpectatorMenuItem item = this.menu.getSelectedItem();
         Component action = item == SpectatorMenu.EMPTY_SLOT ? this.menu.getSelectedCategory().getPrompt() : item.getName();
         int strWidth = this.minecraft.font.width(action);
         int x = (graphics.guiWidth() - strWidth) / 2;
         int y = graphics.guiHeight() - 35;
         graphics.textWithBackdrop(this.minecraft.font, action, x, y, strWidth, ARGB.white(alpha));
      }
   }

   @Override
   public void onSpectatorMenuClosed(final SpectatorMenu menu) {
      this.menu = null;
      this.lastSelectionTime = 0L;
   }

   public boolean isMenuActive() {
      return this.menu != null;
   }

   public void onMouseScrolled(final int wheel) {
      int newSlot = this.menu.getSelectedSlot() + wheel;

      while (newSlot >= 0 && newSlot <= 8 && (this.menu.getItem(newSlot) == SpectatorMenu.EMPTY_SLOT || !this.menu.getItem(newSlot).isEnabled())) {
         newSlot += wheel;
      }

      if (newSlot >= 0 && newSlot <= 8) {
         this.menu.selectSlot(newSlot);
         this.lastSelectionTime = Util.getMillis();
      }
   }

   public void onHotbarActionKeyPressed() {
      this.lastSelectionTime = Util.getMillis();
      if (this.isMenuActive()) {
         int selectedSlot = this.menu.getSelectedSlot();
         if (selectedSlot != -1) {
            this.menu.selectSlot(selectedSlot);
         }
      } else {
         this.menu = new SpectatorMenu(this);
      }
   }
}
