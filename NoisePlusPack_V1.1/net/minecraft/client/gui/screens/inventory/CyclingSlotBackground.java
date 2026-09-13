package net.minecraft.client.gui.screens.inventory;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CyclingSlotBackground {
   private static final int ICON_CHANGE_TICK_RATE = 30;
   private static final int ICON_SIZE = 16;
   private static final int ICON_TRANSITION_TICK_DURATION = 4;
   private final int slotIndex;
   private List<Identifier> icons = List.of();
   private int tick;
   private int iconIndex;

   public CyclingSlotBackground(int p_267314_) {
      this.slotIndex = p_267314_;
   }

   public void tick(List<Identifier> p_267074_) {
      if (!this.icons.equals(p_267074_)) {
         this.icons = p_267074_;
         this.iconIndex = 0;
      }

      if (!this.icons.isEmpty() && ++this.tick % 30 == 0) {
         this.iconIndex = (this.iconIndex + 1) % this.icons.size();
      }
   }

   public void render(AbstractContainerMenu p_267293_, GuiGraphics p_282894_, float p_266785_, int p_266711_, int p_266841_) {
      Slot slot = p_267293_.getSlot(this.slotIndex);
      if (!this.icons.isEmpty() && !slot.hasItem()) {
         boolean flag = this.icons.size() > 1 && this.tick >= 30;
         float f = flag ? this.getIconTransitionTransparency(p_266785_) : 1.0F;
         if (f < 1.0F) {
            int i = Math.floorMod(this.iconIndex - 1, this.icons.size());
            this.renderIcon(slot, this.icons.get(i), 1.0F - f, p_282894_, p_266711_, p_266841_);
         }

         this.renderIcon(slot, this.icons.get(this.iconIndex), f, p_282894_, p_266711_, p_266841_);
      }
   }

   private void renderIcon(Slot p_283532_, Identifier p_451409_, float p_282627_, GuiGraphics p_282825_, int p_281375_, int p_283041_) {
      p_282825_.blitSprite(RenderPipelines.GUI_TEXTURED, p_451409_, p_281375_ + p_283532_.x, p_283041_ + p_283532_.y, 16, 16, ARGB.white(p_282627_));
   }

   private float getIconTransitionTransparency(float p_266904_) {
      float f = this.tick % 30 + p_266904_;
      return Math.min(f, 4.0F) / 4.0F;
   }
}
