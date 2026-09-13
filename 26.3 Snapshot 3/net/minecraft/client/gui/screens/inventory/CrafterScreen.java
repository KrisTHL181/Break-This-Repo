package net.minecraft.client.gui.screens.inventory;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.world.inventory.CrafterSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CrafterScreen extends AbstractContainerScreen<CrafterMenu> {
   private static final Identifier DISABLED_SLOT_LOCATION_SPRITE = Identifier.withDefaultNamespace("container/crafter/disabled_slot");
   private static final Identifier POWERED_REDSTONE_LOCATION_SPRITE = Identifier.withDefaultNamespace("container/crafter/powered_redstone");
   private static final Identifier UNPOWERED_REDSTONE_LOCATION_SPRITE = Identifier.withDefaultNamespace("container/crafter/unpowered_redstone");
   private static final Identifier CONTAINER_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/crafter.png");
   private static final Component DISABLED_SLOT_TOOLTIP = Component.translatable("gui.togglable_slot");
   private final Player player;

   public CrafterScreen(final CrafterMenu menu, final Inventory inventory, final Component title) {
      super(menu, inventory, title);
      this.player = inventory.player;
   }

   @Override
   protected void init() {
      super.init();
      this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
   }

   @Override
   protected void slotClicked(final Slot slot, final int slotId, final int buttonNum, final ContainerInput containerInput) {
      if (slot instanceof CrafterSlot && !slot.hasItem() && !this.player.isSpectator()) {
         switch (containerInput) {
            case PICKUP:
               if (this.menu.isSlotDisabled(slotId)) {
                  this.enableSlot(slotId);
               } else if (this.menu.getCarried().isEmpty()) {
                  this.disableSlot(slotId);
               }
               break;
            case SWAP:
               ItemStack playerInventoryItem = this.player.getInventory().getItem(buttonNum);
               if (this.menu.isSlotDisabled(slotId) && !playerInventoryItem.isEmpty()) {
                  this.enableSlot(slotId);
               }
         }
      }

      super.slotClicked(slot, slotId, buttonNum, containerInput);
   }

   private void enableSlot(final int slotId) {
      this.updateSlotState(slotId, true);
   }

   private void disableSlot(final int slotId) {
      this.updateSlotState(slotId, false);
   }

   private void updateSlotState(final int slotId, final boolean enabled) {
      this.menu.setSlotState(slotId, enabled);
      super.handleSlotStateChanged(slotId, this.menu.containerId, enabled);
      float pitch = enabled ? 1.0F : 0.75F;
      this.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.4F, pitch);
   }

   @Override
   public void extractSlot(final GuiGraphicsExtractor graphics, final Slot slot, final int mouseX, final int mouseY) {
      if (slot instanceof CrafterSlot crafterSlot) {
         if (this.menu.isSlotDisabled(slot.index)) {
            this.extractDisabledSlot(graphics, crafterSlot);
         } else {
            super.extractSlot(graphics, slot, mouseX, mouseY);
         }

         int x0 = this.leftPos + crafterSlot.x - 2;
         int y0 = this.topPos + crafterSlot.y - 2;
         if (mouseX > x0 && mouseY > y0 && mouseX < x0 + 19 && mouseY < y0 + 19) {
            graphics.requestCursor(CursorTypes.POINTING_HAND);
         }
      } else {
         super.extractSlot(graphics, slot, mouseX, mouseY);
      }
   }

   private void extractDisabledSlot(final GuiGraphicsExtractor graphics, final CrafterSlot cs) {
      graphics.blitSprite(RenderPipelines.GUI_TEXTURED, DISABLED_SLOT_LOCATION_SPRITE, cs.x - 1, cs.y - 1, 18, 18);
   }

   @Override
   public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      super.extractRenderState(graphics, mouseX, mouseY, a);
      this.extractRedstone(graphics);
      if (this.hoveredSlot instanceof CrafterSlot
         && !this.menu.isSlotDisabled(this.hoveredSlot.index)
         && this.menu.getCarried().isEmpty()
         && !this.hoveredSlot.hasItem()
         && !this.player.isSpectator()) {
         graphics.setTooltipForNextFrame(this.font, DISABLED_SLOT_TOOLTIP, mouseX, mouseY);
      }
   }

   private void extractRedstone(final GuiGraphicsExtractor graphics) {
      int xo = this.width / 2 + 9;
      int yo = this.height / 2 - 48;
      Identifier redstoneArrowTexture;
      if (this.menu.isPowered()) {
         redstoneArrowTexture = POWERED_REDSTONE_LOCATION_SPRITE;
      } else {
         redstoneArrowTexture = UNPOWERED_REDSTONE_LOCATION_SPRITE;
      }

      graphics.blitSprite(RenderPipelines.GUI_TEXTURED, redstoneArrowTexture, xo, yo, 16, 16);
   }

   @Override
   public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      super.extractBackground(graphics, mouseX, mouseY, a);
      int xo = (this.width - this.imageWidth) / 2;
      int yo = (this.height - this.imageHeight) / 2;
      graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_LOCATION, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
   }
}
