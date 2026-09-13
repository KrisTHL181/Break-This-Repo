package net.minecraft.client.gui.screens.advancements;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class AdvancementWidget {
   private static final Identifier TITLE_BOX_SPRITE = Identifier.withDefaultNamespace("advancements/title_box");
   private static final int HEIGHT = 26;
   private static final int BOX_X = 0;
   private static final int BOX_WIDTH = 200;
   private static final int FRAME_WIDTH = 26;
   private static final int ICON_X = 8;
   private static final int ICON_Y = 5;
   private static final int ICON_WIDTH = 26;
   private static final int TITLE_PADDING_LEFT = 3;
   private static final int TITLE_PADDING_RIGHT = 5;
   private static final int TITLE_X = 32;
   private static final int TITLE_PADDING_TOP = 9;
   private static final int TITLE_PADDING_BOTTOM = 8;
   private static final int TITLE_MAX_WIDTH = 163;
   private static final int TITLE_MIN_WIDTH = 80;
   private static final int[] TEST_SPLIT_OFFSETS = new int[]{0, 10, -10, 25, -25};
   private final AdvancementTab tab;
   private final AdvancementNode advancementNode;
   private final DisplayInfo display;
   private final ItemStack icon;
   private final List<FormattedCharSequence> titleLines;
   private final int width;
   private final List<FormattedCharSequence> description;
   private final Minecraft minecraft;
   private @Nullable AdvancementWidget parent;
   private final List<AdvancementWidget> children = Lists.newArrayList();
   private @Nullable AdvancementProgress progress;
   private final int x;
   private final int y;

   public AdvancementWidget(final AdvancementTab tab, final Minecraft minecraft, final AdvancementNode advancementNode, final DisplayInfo display) {
      this.tab = tab;
      this.advancementNode = advancementNode;
      this.display = display;
      this.minecraft = minecraft;
      this.titleLines = minecraft.font.split(display.getTitle(), 163);
      this.x = Mth.floor(display.getX() * 28.0F);
      this.y = Mth.floor(display.getY() * 27.0F);
      int titleWidth = Math.max(this.titleLines.stream().mapToInt(minecraft.font::width).max().orElse(0), 80);
      int maxProgressWidth = this.getMaxProgressWidth();
      int longestDescLine = 29 + titleWidth + maxProgressWidth;
      this.description = Language.getInstance()
         .getVisualOrder(
            this.findOptimalLines(
               ComponentUtils.mergeStyles(display.getDescription(), Style.EMPTY.withColor(display.getType().getChatColor())), longestDescLine
            )
         );

      for (FormattedCharSequence line : this.description) {
         longestDescLine = Math.max(longestDescLine, minecraft.font.width(line));
      }

      this.width = longestDescLine + 3 + 5;
      this.icon = display.getIcon().create();
   }

   private int getMaxProgressWidth() {
      int maxCriteraRequired = this.advancementNode.advancement().requirements().size();
      if (maxCriteraRequired <= 1) {
         return 0;
      }

      int spacing = 8;
      Component fakeMaxProgress = Component.translatable("advancements.progress", maxCriteraRequired, maxCriteraRequired);
      return this.minecraft.font.width(fakeMaxProgress) + 8;
   }

   private static float getMaxWidth(final StringSplitter splitter, final List<FormattedText> input) {
      return (float)input.stream().mapToDouble(splitter::stringWidth).max().orElse(0.0);
   }

   private List<FormattedText> findOptimalLines(final Component input, final int preferredWidth) {
      StringSplitter splitter = this.minecraft.font.getSplitter();
      List<FormattedText> bestSplit = null;
      float bestDistance = Float.MAX_VALUE;

      for (int testMargin : TEST_SPLIT_OFFSETS) {
         List<FormattedText> testSplit = splitter.splitLines(input, preferredWidth - testMargin, Style.EMPTY);
         float distance = Math.abs(getMaxWidth(splitter, testSplit) - preferredWidth);
         if (distance <= 10.0F) {
            return testSplit;
         }

         if (distance < bestDistance) {
            bestDistance = distance;
            bestSplit = testSplit;
         }
      }

      return bestSplit;
   }

   private @Nullable AdvancementWidget getFirstVisibleParent(AdvancementNode node) {
      do {
         node = node.parent();
      } while (node != null && node.advancement().display().isEmpty());

      return node != null && !node.advancement().display().isEmpty() ? this.tab.getWidget(node.holder()) : null;
   }

   public void extractConnectivity(final GuiGraphicsExtractor graphics, final int xo, final int yo, final boolean background) {
      if (this.parent != null) {
         int depX = xo + this.parent.x + 13;
         int splitX = xo + this.parent.x + 26 + 4;
         int depY = yo + this.parent.y + 13;
         int myX = xo + this.x + 13;
         int myY = yo + this.y + 13;
         int col = background ? -16777216 : -1;
         if (background) {
            graphics.horizontalLine(splitX, depX, depY - 1, col);
            graphics.horizontalLine(splitX + 1, depX, depY, col);
            graphics.horizontalLine(splitX, depX, depY + 1, col);
            graphics.horizontalLine(myX, splitX - 1, myY - 1, col);
            graphics.horizontalLine(myX, splitX - 1, myY, col);
            graphics.horizontalLine(myX, splitX - 1, myY + 1, col);
            graphics.verticalLine(splitX - 1, myY, depY, col);
            graphics.verticalLine(splitX + 1, myY, depY, col);
         } else {
            graphics.horizontalLine(splitX, depX, depY, col);
            graphics.horizontalLine(myX, splitX, myY, col);
            graphics.verticalLine(splitX, myY, depY, col);
         }
      }

      for (AdvancementWidget child : this.children) {
         child.extractConnectivity(graphics, xo, yo, background);
      }
   }

   public void extractRenderState(final GuiGraphicsExtractor graphics, final int xo, final int yo) {
      if (!this.display.isHidden() || this.progress != null && this.progress.isDone()) {
         float amount = this.progress == null ? 0.0F : this.progress.getPercent();
         AdvancementWidgetType iconFrame;
         if (amount >= 1.0F) {
            iconFrame = AdvancementWidgetType.OBTAINED;
         } else {
            iconFrame = AdvancementWidgetType.UNOBTAINED;
         }

         graphics.blitSprite(RenderPipelines.GUI_TEXTURED, iconFrame.frameSprite(this.display.getType()), xo + this.x + 3, yo + this.y, 26, 26);
         graphics.fakeItem(this.icon, xo + this.x + 8, yo + this.y + 5);
      }

      for (AdvancementWidget child : this.children) {
         child.extractRenderState(graphics, xo, yo);
      }
   }

   public int getWidth() {
      return this.width;
   }

   public void setProgress(final AdvancementProgress progress) {
      this.progress = progress;
   }

   public void addChild(final AdvancementWidget widget) {
      this.children.add(widget);
   }

   public void extractHover(final GuiGraphicsExtractor graphics, final int xo, final int yo, final float fade, final int screenxo, final int screenyo) {
      Font font = this.minecraft.font;
      int titleBarHeight = 9 * this.titleLines.size() + 9 + 8;
      int titleTop = yo + this.y + (26 - titleBarHeight) / 2;
      int titleBarBottom = titleTop + titleBarHeight;
      int descriptionTextHeight = this.description.size() * 9;
      int descriptionHeight = 6 + descriptionTextHeight;
      boolean leftSide = screenxo + xo + this.x + this.width + 26 >= this.tab.getScreen().width;
      Component progressText = this.progress == null ? null : this.progress.getProgressText();
      int progressWidth = progressText == null ? 0 : font.width(progressText);
      boolean topSide = titleBarBottom + descriptionHeight >= 113;
      float amount = this.progress == null ? 0.0F : this.progress.getPercent();
      int firstHalfWidth = Mth.floor(amount * this.width);
      AdvancementWidgetType firstHalf;
      AdvancementWidgetType secondHalf;
      AdvancementWidgetType iconFrame;
      if (amount >= 1.0F) {
         firstHalfWidth = this.width / 2;
         firstHalf = AdvancementWidgetType.OBTAINED;
         secondHalf = AdvancementWidgetType.OBTAINED;
         iconFrame = AdvancementWidgetType.OBTAINED;
      } else if (firstHalfWidth < 2) {
         firstHalfWidth = this.width / 2;
         firstHalf = AdvancementWidgetType.UNOBTAINED;
         secondHalf = AdvancementWidgetType.UNOBTAINED;
         iconFrame = AdvancementWidgetType.UNOBTAINED;
      } else if (firstHalfWidth > this.width - 2) {
         firstHalfWidth = this.width / 2;
         firstHalf = AdvancementWidgetType.OBTAINED;
         secondHalf = AdvancementWidgetType.OBTAINED;
         iconFrame = AdvancementWidgetType.UNOBTAINED;
      } else {
         firstHalf = AdvancementWidgetType.OBTAINED;
         secondHalf = AdvancementWidgetType.UNOBTAINED;
         iconFrame = AdvancementWidgetType.UNOBTAINED;
      }

      int secondBarWidth = this.width - firstHalfWidth;
      int titleLeft;
      if (leftSide) {
         titleLeft = xo + this.x - this.width + 26 + 6;
      } else {
         titleLeft = xo + this.x;
      }

      int backgroundHeight = titleBarHeight + descriptionHeight;
      if (!this.description.isEmpty()) {
         if (topSide) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, TITLE_BOX_SPRITE, titleLeft, titleBarBottom - backgroundHeight, this.width, backgroundHeight);
         } else {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, TITLE_BOX_SPRITE, titleLeft, titleTop, this.width, backgroundHeight);
         }
      }

      if (firstHalf != secondHalf) {
         graphics.blitSprite(
            RenderPipelines.GUI_TEXTURED, firstHalf.boxSprite(), 200, titleBarHeight, 0, 0, titleLeft, titleTop, firstHalfWidth, titleBarHeight
         );
         graphics.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            secondHalf.boxSprite(),
            200,
            titleBarHeight,
            200 - secondBarWidth,
            0,
            titleLeft + firstHalfWidth,
            titleTop,
            secondBarWidth,
            titleBarHeight
         );
      } else {
         graphics.blitSprite(RenderPipelines.GUI_TEXTURED, firstHalf.boxSprite(), titleLeft, titleTop, this.width, titleBarHeight);
      }

      graphics.blitSprite(RenderPipelines.GUI_TEXTURED, iconFrame.frameSprite(this.display.getType()), xo + this.x + 3, yo + this.y, 26, 26);
      int descriptionLeft = titleLeft + 5;
      if (leftSide) {
         this.extractMultilineText(graphics, this.titleLines, descriptionLeft, titleTop + 9, -1);
         if (progressText != null) {
            graphics.text(font, progressText, xo + this.x - progressWidth, titleTop + 9, -1);
         }
      } else {
         this.extractMultilineText(graphics, this.titleLines, xo + this.x + 32, titleTop + 9, -1);
         if (progressText != null) {
            graphics.text(font, progressText, xo + this.x + this.width - progressWidth - 5, titleTop + 9, -1);
         }
      }

      if (topSide) {
         this.extractMultilineText(graphics, this.description, descriptionLeft, titleTop - descriptionTextHeight + 1, -16711936);
      } else {
         this.extractMultilineText(graphics, this.description, descriptionLeft, titleBarBottom, -16711936);
      }

      graphics.fakeItem(this.icon, xo + this.x + 8, yo + this.y + 5);
   }

   private void extractMultilineText(final GuiGraphicsExtractor graphics, final List<FormattedCharSequence> lines, final int x, final int y, final int color) {
      Font font = this.minecraft.font;

      for (int i = 0; i < lines.size(); i++) {
         graphics.text(font, lines.get(i), x, y + i * 9, color);
      }
   }

   public boolean isMouseOver(final int xo, final int yo, final int mouseX, final int mouseY) {
      if (!this.display.isHidden() || this.progress != null && this.progress.isDone()) {
         int x0 = xo + this.x;
         int x1 = x0 + 26;
         int y0 = yo + this.y;
         int y1 = y0 + 26;
         return mouseX >= x0 && mouseX <= x1 && mouseY >= y0 && mouseY <= y1;
      } else {
         return false;
      }
   }

   public void attachToParent() {
      if (this.parent == null && this.advancementNode.parent() != null) {
         this.parent = this.getFirstVisibleParent(this.advancementNode);
         if (this.parent != null) {
            this.parent.addChild(this);
         }
      }
   }

   public int getY() {
      return this.y;
   }

   public int getX() {
      return this.x;
   }
}
