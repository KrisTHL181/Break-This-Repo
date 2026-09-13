package net.minecraft.client.gui.screens.inventory;

import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

public class CyclingSlotBackground {
   private static final int ICON_CHANGE_TICK_RATE = 30;
   private static final int ICON_SIZE = 16;
   private static final int ICON_TRANSITION_TICK_DURATION = 4;
   private final int slotIndex;
   private List<Identifier> icons = List.of();
   private int tick;
   private int iconIndex;

   public CyclingSlotBackground(final int slotIndex) {
      this.slotIndex = slotIndex;
   }

   public void tick(final List<Identifier> newIcons) {
      if (!this.icons.equals(newIcons)) {
         this.icons = newIcons;
         this.iconIndex = 0;
      }

      if (!this.icons.isEmpty() && ++this.tick % 30 == 0) {
         this.iconIndex = (this.iconIndex + 1) % this.icons.size();
      }
   }

   public void extractRenderState(final AbstractContainerMenu menu, final GuiGraphicsExtractor graphics, final float a, final int left, final int top) {
      Slot slot = menu.getSlot(this.slotIndex);
      if (!this.icons.isEmpty() && !slot.hasItem()) {
         boolean shouldTransition = this.icons.size() > 1 && this.tick >= 30;
         float alphaProgress = shouldTransition ? this.getIconTransitionTransparency(a) : 1.0F;
         if (alphaProgress < 1.0F) {
            int previousIconIndex = Math.floorMod(this.iconIndex - 1, this.icons.size());
            this.extractIcon(slot, this.icons.get(previousIconIndex), 1.0F - alphaProgress, graphics, left, top);
         }

         this.extractIcon(slot, this.icons.get(this.iconIndex), alphaProgress, graphics, left, top);
      }
   }

   private void extractIcon(
      final Slot slot, final Identifier iconIdentifier, final float alphaProgress, final GuiGraphicsExtractor graphics, final int left, final int top
   ) {
      graphics.blitSprite(RenderPipelines.GUI_TEXTURED, iconIdentifier, left + slot.x, top + slot.y, 16, 16, ARGB.white(alphaProgress));
   }

   private float getIconTransitionTransparency(final float a) {
      float elapsedTransitionTime = this.tick % 30 + a;
      return Math.min(elapsedTransitionTime, 4.0F) / 4.0F;
   }
}
