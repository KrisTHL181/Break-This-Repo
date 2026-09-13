package net.minecraft.client.gui.screens.inventory.tooltip;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.math.Fraction;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ClientBundleTooltip implements ClientTooltipComponent {
   private static final Identifier PROGRESSBAR_BORDER_SPRITE = Identifier.withDefaultNamespace("container/bundle/bundle_progressbar_border");
   private static final Identifier PROGRESSBAR_FILL_SPRITE = Identifier.withDefaultNamespace("container/bundle/bundle_progressbar_fill");
   private static final Identifier PROGRESSBAR_FULL_SPRITE = Identifier.withDefaultNamespace("container/bundle/bundle_progressbar_full");
   private static final Identifier SLOT_HIGHLIGHT_BACK_SPRITE = Identifier.withDefaultNamespace("container/bundle/slot_highlight_back");
   private static final Identifier SLOT_HIGHLIGHT_FRONT_SPRITE = Identifier.withDefaultNamespace("container/bundle/slot_highlight_front");
   private static final Identifier SLOT_BACKGROUND_SPRITE = Identifier.withDefaultNamespace("container/bundle/slot_background");
   private static final int SLOT_MARGIN = 4;
   private static final int SLOT_SIZE = 24;
   private static final int GRID_WIDTH = 96;
   private static final int PROGRESSBAR_HEIGHT = 13;
   private static final int PROGRESSBAR_WIDTH = 96;
   private static final int PROGRESSBAR_BORDER = 1;
   private static final int PROGRESSBAR_FILL_MAX = 94;
   private static final int PROGRESSBAR_MARGIN_Y = 4;
   private static final Component BUNDLE_FULL_TEXT = Component.translatable("item.minecraft.bundle.full");
   private static final Component BUNDLE_EMPTY_TEXT = Component.translatable("item.minecraft.bundle.empty");
   private static final Component BUNDLE_EMPTY_DESCRIPTION = Component.translatable("item.minecraft.bundle.empty.description");
   private final BundleContents contents;

   public ClientBundleTooltip(BundleContents p_335644_) {
      this.contents = p_335644_;
   }

   @Override
   public int getHeight(Font p_362861_) {
      return this.contents.isEmpty() ? getEmptyBundleBackgroundHeight(p_362861_) : this.backgroundHeight();
   }

   @Override
   public int getWidth(Font p_169901_) {
      return 96;
   }

   @Override
   public boolean showTooltipWithItemInHand() {
      return true;
   }

   private static int getEmptyBundleBackgroundHeight(Font p_361809_) {
      return getEmptyBundleDescriptionTextHeight(p_361809_) + 13 + 8;
   }

   private int backgroundHeight() {
      return this.itemGridHeight() + 13 + 8;
   }

   private int itemGridHeight() {
      return this.gridSizeY() * 24;
   }

   private int getContentXOffset(int p_364093_) {
      return (p_364093_ - 96) / 2;
   }

   private int gridSizeY() {
      return Mth.positiveCeilDiv(this.slotCount(), 4);
   }

   private int slotCount() {
      return Math.min(12, this.contents.size());
   }

   @Override
   public void renderImage(Font p_194042_, int p_194043_, int p_194044_, int p_369638_, int p_364312_, GuiGraphics p_282522_) {
      if (this.contents.isEmpty()) {
         this.renderEmptyBundleTooltip(p_194042_, p_194043_, p_194044_, p_369638_, p_364312_, p_282522_);
      } else {
         this.renderBundleWithItemsTooltip(p_194042_, p_194043_, p_194044_, p_369638_, p_364312_, p_282522_);
      }
   }

   private void renderEmptyBundleTooltip(Font p_364480_, int p_360881_, int p_369772_, int p_361857_, int p_364153_, GuiGraphics p_365898_) {
      drawEmptyBundleDescriptionText(p_360881_ + this.getContentXOffset(p_361857_), p_369772_, p_364480_, p_365898_);
      this.drawProgressbar(p_360881_ + this.getContentXOffset(p_361857_), p_369772_ + getEmptyBundleDescriptionTextHeight(p_364480_) + 4, p_364480_, p_365898_);
   }

   private void renderBundleWithItemsTooltip(Font p_368943_, int p_367976_, int p_363502_, int p_368727_, int p_363888_, GuiGraphics p_368494_) {
      boolean flag = this.contents.size() > 12;
      List<ItemStack> list = this.getShownItems(this.contents.getNumberOfItemsToShow());
      int i = p_367976_ + this.getContentXOffset(p_368727_) + 96;
      int j = p_363502_ + this.gridSizeY() * 24;
      int k = 1;

      for (int l = 1; l <= this.gridSizeY(); l++) {
         for (int i1 = 1; i1 <= 4; i1++) {
            int j1 = i - i1 * 24;
            int k1 = j - l * 24;
            if (shouldRenderSurplusText(flag, i1, l)) {
               renderCount(j1, k1, this.getAmountOfHiddenItems(list), p_368943_, p_368494_);
            } else if (shouldRenderItemSlot(list, k)) {
               this.renderSlot(k, j1, k1, list, k, p_368943_, p_368494_);
               k++;
            }
         }
      }

      this.drawSelectedItemTooltip(p_368943_, p_368494_, p_367976_, p_363502_, p_368727_);
      this.drawProgressbar(p_367976_ + this.getContentXOffset(p_368727_), p_363502_ + this.itemGridHeight() + 4, p_368943_, p_368494_);
   }

   private List<ItemStack> getShownItems(int p_369856_) {
      int i = Math.min(this.contents.size(), p_369856_);
      return this.contents.itemCopyStream().toList().subList(0, i);
   }

   private static boolean shouldRenderSurplusText(boolean p_362669_, int p_365579_, int p_364239_) {
      return p_362669_ && p_365579_ * p_364239_ == 1;
   }

   private static boolean shouldRenderItemSlot(List<ItemStack> p_361001_, int p_368142_) {
      return p_361001_.size() >= p_368142_;
   }

   private int getAmountOfHiddenItems(List<ItemStack> p_361494_) {
      return this.contents.itemCopyStream().skip(p_361494_.size()).mapToInt(ItemStack::getCount).sum();
   }

   private void renderSlot(int p_283180_, int p_282972_, int p_282547_, List<ItemStack> p_363643_, int p_368225_, Font p_281863_, GuiGraphics p_283625_) {
      int i = p_363643_.size() - p_283180_;
      boolean flag = i == this.contents.getSelectedItem();
      ItemStack itemstack = p_363643_.get(i);
      if (flag) {
         p_283625_.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_BACK_SPRITE, p_282972_, p_282547_, 24, 24);
      } else {
         p_283625_.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_BACKGROUND_SPRITE, p_282972_, p_282547_, 24, 24);
      }

      p_283625_.renderItem(itemstack, p_282972_ + 4, p_282547_ + 4, p_368225_);
      p_283625_.renderItemDecorations(p_281863_, itemstack, p_282972_ + 4, p_282547_ + 4);
      if (flag) {
         p_283625_.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_FRONT_SPRITE, p_282972_, p_282547_, 24, 24);
      }
   }

   private static void renderCount(int p_367494_, int p_360787_, int p_366039_, Font p_369606_, GuiGraphics p_369155_) {
      p_369155_.drawCenteredString(p_369606_, "+" + p_366039_, p_367494_ + 12, p_360787_ + 10, -1);
   }

   private void drawSelectedItemTooltip(Font p_362719_, GuiGraphics p_366113_, int p_365302_, int p_368361_, int p_367077_) {
      if (this.contents.hasSelectedItem()) {
         ItemStack itemstack = this.contents.getItemUnsafe(this.contents.getSelectedItem());
         Component component = itemstack.getStyledHoverName();
         int i = p_362719_.width(component.getVisualOrderText());
         int j = p_365302_ + p_367077_ / 2 - 12;
         ClientTooltipComponent clienttooltipcomponent = ClientTooltipComponent.create(component.getVisualOrderText());
         p_366113_.renderTooltip(
            p_362719_,
            List.of(clienttooltipcomponent),
            j - i / 2,
            p_368361_ - 15,
            DefaultTooltipPositioner.INSTANCE,
            itemstack.get(DataComponents.TOOLTIP_STYLE)
         );
      }
   }

   private void drawProgressbar(int p_362560_, int p_367617_, Font p_361416_, GuiGraphics p_363358_) {
      p_363358_.blitSprite(RenderPipelines.GUI_TEXTURED, this.getProgressBarTexture(), p_362560_ + 1, p_367617_, this.getProgressBarFill(), 13);
      p_363358_.blitSprite(RenderPipelines.GUI_TEXTURED, PROGRESSBAR_BORDER_SPRITE, p_362560_, p_367617_, 96, 13);
      Component component = this.getProgressBarFillText();
      if (component != null) {
         p_363358_.drawCenteredString(p_361416_, component, p_362560_ + 48, p_367617_ + 3, -1);
      }
   }

   private static void drawEmptyBundleDescriptionText(int p_361101_, int p_362507_, Font p_361285_, GuiGraphics p_364539_) {
      p_364539_.drawWordWrap(p_361285_, BUNDLE_EMPTY_DESCRIPTION, p_361101_, p_362507_, 96, -5592406);
   }

   private static int getEmptyBundleDescriptionTextHeight(Font p_361354_) {
      return p_361354_.split(BUNDLE_EMPTY_DESCRIPTION, 96).size() * 9;
   }

   private int getProgressBarFill() {
      return Mth.clamp(Mth.mulAndTruncate(this.contents.weight(), 94), 0, 94);
   }

   private Identifier getProgressBarTexture() {
      return this.contents.weight().compareTo(Fraction.ONE) >= 0 ? PROGRESSBAR_FULL_SPRITE : PROGRESSBAR_FILL_SPRITE;
   }

   private @Nullable Component getProgressBarFillText() {
      if (this.contents.isEmpty()) {
         return BUNDLE_EMPTY_TEXT;
      } else {
         return this.contents.weight().compareTo(Fraction.ONE) >= 0 ? BUNDLE_FULL_TEXT : null;
      }
   }
}
