package net.minecraft.client.gui.screens.inventory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class ItemCombinerScreen<T extends ItemCombinerMenu> extends AbstractContainerScreen<T> implements ContainerListener {
   private final Identifier menuResource;

   public ItemCombinerScreen(T p_98901_, Inventory p_98902_, Component p_98903_, Identifier p_460725_) {
      super(p_98901_, p_98902_, p_98903_);
      this.menuResource = p_460725_;
   }

   protected void subInit() {
   }

   @Override
   protected void init() {
      super.init();
      this.subInit();
      this.menu.addSlotListener(this);
   }

   @Override
   public void removed() {
      super.removed();
      this.menu.removeSlotListener(this);
   }

   @Override
   public void render(GuiGraphics p_281810_, int p_283312_, int p_283420_, float p_282956_) {
      super.render(p_281810_, p_283312_, p_283420_, p_282956_);
      this.renderTooltip(p_281810_, p_283312_, p_283420_);
   }

   @Override
   protected void renderBg(GuiGraphics p_282749_, float p_283494_, int p_283098_, int p_282054_) {
      p_282749_.blit(RenderPipelines.GUI_TEXTURED, this.menuResource, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
      this.renderErrorIcon(p_282749_, this.leftPos, this.topPos);
   }

   protected abstract void renderErrorIcon(GuiGraphics var1, int var2, int var3);

   @Override
   public void dataChanged(AbstractContainerMenu p_169759_, int p_169760_, int p_169761_) {
   }

   @Override
   public void slotChanged(AbstractContainerMenu p_98910_, int p_98911_, ItemStack p_98912_) {
   }
}
