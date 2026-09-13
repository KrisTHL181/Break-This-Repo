package net.minecraft.client.gui.screens.inventory.tooltip;

import com.mojang.serialization.DataResult;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.BundleContents;
import org.apache.commons.lang3.math.Fraction;
import org.jspecify.annotations.Nullable;

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

   public ClientBundleTooltip(final BundleContents contents) {
      this.contents = contents;
   }

   @Override
   public int getHeight(final Font font) {
      return this.contents.isEmpty() ? getEmptyBundleBackgroundHeight(font) : this.backgroundHeight();
   }

   @Override
   public int getWidth(final Font font) {
      return 96;
   }

   @Override
   public boolean showTooltipWithItemInHand() {
      return true;
   }

   private static int getEmptyBundleBackgroundHeight(final Font font) {
      return getEmptyBundleDescriptionTextHeight(font) + 13 + 8;
   }

   private int backgroundHeight() {
      return this.itemGridHeight() + 13 + 8;
   }

   private int itemGridHeight() {
      return this.gridSizeY() * 24;
   }

   private static int getContentXOffset(final int tooltipWidth) {
      return (tooltipWidth - 96) / 2;
   }

   private int gridSizeY() {
      return Mth.positiveCeilDiv(this.slotCount(), 4);
   }

   private int slotCount() {
      return Math.min(12, this.contents.size());
   }

   @Override
   public void extractImage(final Font font, final int x, final int y, final int w, final int h, final GuiGraphicsExtractor graphics) {
      DataResult<Fraction> weight = this.contents.weight();
      if (!weight.isError()) {
         if (this.contents.isEmpty()) {
            extractEmptyBundleTooltip(font, x, y, w, h, graphics);
         } else {
            this.extractBundleWithItemsTooltip(font, x, y, w, h, graphics, (Fraction)weight.getOrThrow());
         }
      }
   }

   private static void extractEmptyBundleTooltip(final Font font, final int x, final int y, final int w, final int h, final GuiGraphicsExtractor graphics) {
      int left = x + getContentXOffset(w);
      extractEmptyBundleDescriptionText(left, y, font, graphics);
      extractProgressbar(left, y + getEmptyBundleDescriptionTextHeight(font) + 4, font, graphics, Fraction.ZERO);
   }

   private void extractBundleWithItemsTooltip(
      final Font font, final int x, final int y, final int w, final int h, final GuiGraphicsExtractor graphics, final Fraction weight
   ) {
      boolean isOverflowing = this.contents.size() > 12;
      List<ItemStackTemplate> shownItems = this.getShownItems(this.contents.getNumberOfItemsToShow());
      int xStartPos = x + getContentXOffset(w) + 96;
      int yStartPos = y + this.gridSizeY() * 24;
      int slotNumber = 1;

      for (int rowNumber = 1; rowNumber <= this.gridSizeY(); rowNumber++) {
         for (int columnNumber = 1; columnNumber <= 4; columnNumber++) {
            int drawX = xStartPos - columnNumber * 24;
            int drawY = yStartPos - rowNumber * 24;
            if (shouldRenderSurplusText(isOverflowing, columnNumber, rowNumber)) {
               extractCount(drawX, drawY, this.getAmountOfHiddenItems(shownItems), font, graphics);
            } else if (shouldRenderItemSlot(shownItems, slotNumber)) {
               this.extractSlot(slotNumber, drawX, drawY, shownItems, slotNumber, font, graphics);
               slotNumber++;
            }
         }
      }

      this.extractSelectedItemTooltip(font, graphics, x, y, w);
      extractProgressbar(x + getContentXOffset(w), y + this.itemGridHeight() + 4, font, graphics, weight);
   }

   private List<ItemStackTemplate> getShownItems(final int amountOfItemsToShow) {
      int lastToDisplay = Math.min(this.contents.size(), amountOfItemsToShow);
      return this.contents.items().subList(0, lastToDisplay);
   }

   private static boolean shouldRenderSurplusText(final boolean isOverflowing, final int column, final int row) {
      return isOverflowing && column * row == 1;
   }

   private static boolean shouldRenderItemSlot(final List<? extends ItemInstance> shownItems, final int slotNumber) {
      return shownItems.size() >= slotNumber;
   }

   private int getAmountOfHiddenItems(final List<ItemStackTemplate> shownItems) {
      return this.contents.items().stream().skip(shownItems.size()).mapToInt(ItemInstance::count).sum();
   }

   private void extractSlot(
      final int slotNumber,
      final int drawX,
      final int drawY,
      final List<ItemStackTemplate> shownItems,
      final int slotIndex,
      final Font font,
      final GuiGraphicsExtractor graphics
   ) {
      int itemVisualOrderIndex = shownItems.size() - slotNumber;
      boolean hasHighlight = itemVisualOrderIndex == this.contents.getSelectedItemIndex();
      ItemStack item = shownItems.get(itemVisualOrderIndex).create();
      if (hasHighlight) {
         graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_BACK_SPRITE, drawX, drawY, 24, 24);
      } else {
         graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_BACKGROUND_SPRITE, drawX, drawY, 24, 24);
      }

      graphics.item(item, drawX + 4, drawY + 4, slotIndex);
      graphics.itemDecorations(font, item, drawX + 4, drawY + 4);
      if (hasHighlight) {
         graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_FRONT_SPRITE, drawX, drawY, 24, 24);
      }
   }

   private static void extractCount(final int drawX, final int drawY, final int hiddenItemCount, final Font font, final GuiGraphicsExtractor graphics) {
      graphics.centeredText(font, "+" + hiddenItemCount, drawX + 12, drawY + 10, -1);
   }

   private void extractSelectedItemTooltip(final Font font, final GuiGraphicsExtractor graphics, final int x, final int y, final int w) {
      ItemStackTemplate selectedItem = this.contents.getSelectedItem();
      if (selectedItem != null) {
         ItemStack itemStack = selectedItem.create();
         Component selectedItemName = itemStack.getStyledHoverName();
         int textWidth = font.width(selectedItemName.getVisualOrderText());
         int centerTooltip = x + w / 2 - 12;
         ClientTooltipComponent selectedItemNameTooltip = ClientTooltipComponent.create(selectedItemName.getVisualOrderText());
         graphics.tooltip(
            font,
            List.of(selectedItemNameTooltip),
            centerTooltip - textWidth / 2,
            y - 15,
            DefaultTooltipPositioner.INSTANCE,
            itemStack.get(DataComponents.TOOLTIP_STYLE),
            false
         );
      }
   }

   private static void extractProgressbar(final int x, final int y, final Font font, final GuiGraphicsExtractor graphics, final Fraction weight) {
      graphics.blitSprite(RenderPipelines.GUI_TEXTURED, getProgressBarTexture(weight), x + 1, y, getProgressBarFill(weight), 13);
      graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PROGRESSBAR_BORDER_SPRITE, x, y, 96, 13);
      Component progressBarFillText = getProgressBarFillText(weight);
      if (progressBarFillText != null) {
         graphics.centeredText(font, progressBarFillText, x + 48, y + 3, -1);
      }
   }

   private static void extractEmptyBundleDescriptionText(final int x, final int y, final Font font, final GuiGraphicsExtractor graphics) {
      graphics.textWithWordWrap(font, BUNDLE_EMPTY_DESCRIPTION, x, y, 96, -5592406);
   }

   private static int getEmptyBundleDescriptionTextHeight(final Font font) {
      return font.split(BUNDLE_EMPTY_DESCRIPTION, 96).size() * 9;
   }

   private static int getProgressBarFill(final Fraction weight) {
      return Mth.clamp(Mth.mulAndTruncate(weight, 94), 0, 94);
   }

   private static Identifier getProgressBarTexture(final Fraction weight) {
      return weight.compareTo(Fraction.ONE) >= 0 ? PROGRESSBAR_FULL_SPRITE : PROGRESSBAR_FILL_SPRITE;
   }

   private static @Nullable Component getProgressBarFillText(final Fraction weight) {
      if (weight.compareTo(Fraction.ZERO) == 0) {
         return BUNDLE_EMPTY_TEXT;
      } else {
         return weight.compareTo(Fraction.ONE) >= 0 ? BUNDLE_FULL_TEXT : null;
      }
   }
}
