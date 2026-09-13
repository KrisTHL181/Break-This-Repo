package net.minecraft.client.gui.screens.inventory;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.world.inventory.CrafterSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CrafterScreen extends AbstractContainerScreen<CrafterMenu> {
   private static final Identifier DISABLED_SLOT_LOCATION_SPRITE = Identifier.withDefaultNamespace("container/crafter/disabled_slot");
   private static final Identifier POWERED_REDSTONE_LOCATION_SPRITE = Identifier.withDefaultNamespace("container/crafter/powered_redstone");
   private static final Identifier UNPOWERED_REDSTONE_LOCATION_SPRITE = Identifier.withDefaultNamespace("container/crafter/unpowered_redstone");
   private static final Identifier CONTAINER_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/crafter.png");
   private static final Component DISABLED_SLOT_TOOLTIP = Component.translatable("gui.togglable_slot");
   private final Player player;

   public CrafterScreen(CrafterMenu p_310211_, Inventory p_312788_, Component p_312962_) {
      super(p_310211_, p_312788_, p_312962_);
      this.player = p_312788_.player;
   }

   @Override
   protected void init() {
      super.init();
      this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
   }

   @Override
   protected void slotClicked(Slot p_310794_, int p_309597_, int p_311886_, ClickType p_312328_) {
      if (p_310794_ instanceof CrafterSlot && !p_310794_.hasItem() && !this.player.isSpectator()) {
         switch (p_312328_) {
            case PICKUP:
               if (this.menu.isSlotDisabled(p_309597_)) {
                  this.enableSlot(p_309597_);
               } else if (this.menu.getCarried().isEmpty()) {
                  this.disableSlot(p_309597_);
               }
               break;
            case SWAP:
               ItemStack itemstack = this.player.getInventory().getItem(p_311886_);
               if (this.menu.isSlotDisabled(p_309597_) && !itemstack.isEmpty()) {
                  this.enableSlot(p_309597_);
               }
         }
      }

      super.slotClicked(p_310794_, p_309597_, p_311886_, p_312328_);
   }

   private void enableSlot(int p_309894_) {
      this.updateSlotState(p_309894_, true);
   }

   private void disableSlot(int p_309649_) {
      this.updateSlotState(p_309649_, false);
   }

   private void updateSlotState(int p_309759_, boolean p_311308_) {
      this.menu.setSlotState(p_309759_, p_311308_);
      super.handleSlotStateChanged(p_309759_, this.menu.containerId, p_311308_);
      float f = p_311308_ ? 1.0F : 0.75F;
      this.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.4F, f);
   }

   @Override
   public void renderSlot(GuiGraphics p_310399_, Slot p_312178_, int p_455586_, int p_459479_) {
      if (p_312178_ instanceof CrafterSlot crafterslot) {
         if (this.menu.isSlotDisabled(p_312178_.index)) {
            this.renderDisabledSlot(p_310399_, crafterslot);
         } else {
            super.renderSlot(p_310399_, p_312178_, p_455586_, p_459479_);
         }

         int i = this.leftPos + crafterslot.x - 2;
         int j = this.topPos + crafterslot.y - 2;
         if (p_455586_ > i && p_459479_ > j && p_455586_ < i + 19 && p_459479_ < j + 19) {
            p_310399_.requestCursor(CursorTypes.POINTING_HAND);
         }
      } else {
         super.renderSlot(p_310399_, p_312178_, p_455586_, p_459479_);
      }
   }

   private void renderDisabledSlot(GuiGraphics p_310437_, CrafterSlot p_309818_) {
      p_310437_.blitSprite(RenderPipelines.GUI_TEXTURED, DISABLED_SLOT_LOCATION_SPRITE, p_309818_.x - 1, p_309818_.y - 1, 18, 18);
   }

   @Override
   public void render(GuiGraphics p_313170_, int p_311302_, int p_309565_, float p_311210_) {
      super.render(p_313170_, p_311302_, p_309565_, p_311210_);
      this.renderRedstone(p_313170_);
      this.renderTooltip(p_313170_, p_311302_, p_309565_);
      if (this.hoveredSlot instanceof CrafterSlot
         && !this.menu.isSlotDisabled(this.hoveredSlot.index)
         && this.menu.getCarried().isEmpty()
         && !this.hoveredSlot.hasItem()
         && !this.player.isSpectator()) {
         p_313170_.setTooltipForNextFrame(this.font, DISABLED_SLOT_TOOLTIP, p_311302_, p_309565_);
      }
   }

   private void renderRedstone(GuiGraphics p_311767_) {
      int i = this.width / 2 + 9;
      int j = this.height / 2 - 48;
      Identifier identifier;
      if (this.menu.isPowered()) {
         identifier = POWERED_REDSTONE_LOCATION_SPRITE;
      } else {
         identifier = UNPOWERED_REDSTONE_LOCATION_SPRITE;
      }

      p_311767_.blitSprite(RenderPipelines.GUI_TEXTURED, identifier, i, j, 16, 16);
   }

   @Override
   protected void renderBg(GuiGraphics p_309628_, float p_312032_, int p_310627_, int p_311751_) {
      int i = (this.width - this.imageWidth) / 2;
      int j = (this.height - this.imageHeight) / 2;
      p_309628_.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_LOCATION, i, j, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
   }
}
