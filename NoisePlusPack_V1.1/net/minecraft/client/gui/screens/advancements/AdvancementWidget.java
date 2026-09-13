package net.minecraft.client.gui.screens.advancements;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
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
   private final List<FormattedCharSequence> titleLines;
   private final int width;
   private final List<FormattedCharSequence> description;
   private final Minecraft minecraft;
   private @Nullable AdvancementWidget parent;
   private final List<AdvancementWidget> children = Lists.newArrayList();
   private @Nullable AdvancementProgress progress;
   private final int x;
   private final int y;

   public AdvancementWidget(AdvancementTab p_97255_, Minecraft p_97256_, AdvancementNode p_298278_, DisplayInfo p_97258_) {
      this.tab = p_97255_;
      this.advancementNode = p_298278_;
      this.display = p_97258_;
      this.minecraft = p_97256_;
      this.titleLines = p_97256_.font.split(p_97258_.getTitle(), 163);
      this.x = Mth.floor(p_97258_.getX() * 28.0F);
      this.y = Mth.floor(p_97258_.getY() * 27.0F);
      int i = Math.max(this.titleLines.stream().mapToInt(p_97256_.font::width).max().orElse(0), 80);
      int j = this.getMaxProgressWidth();
      int k = 29 + i + j;
      this.description = Language.getInstance()
         .getVisualOrder(
            this.findOptimalLines(ComponentUtils.mergeStyles(p_97258_.getDescription(), Style.EMPTY.withColor(p_97258_.getType().getChatColor())), k)
         );

      for (FormattedCharSequence formattedcharsequence : this.description) {
         k = Math.max(k, p_97256_.font.width(formattedcharsequence));
      }

      this.width = k + 3 + 5;
   }

   private int getMaxProgressWidth() {
      int i = this.advancementNode.advancement().requirements().size();
      if (i <= 1) {
         return 0;
      }

      int j = 8;
      Component component = Component.translatable("advancements.progress", i, i);
      return this.minecraft.font.width(component) + 8;
   }

   private static float getMaxWidth(StringSplitter p_97304_, List<FormattedText> p_97305_) {
      return (float)p_97305_.stream().mapToDouble(p_97304_::stringWidth).max().orElse(0.0);
   }

   private List<FormattedText> findOptimalLines(Component p_97309_, int p_97310_) {
      StringSplitter stringsplitter = this.minecraft.font.getSplitter();
      List<FormattedText> list = null;
      float f = Float.MAX_VALUE;

      for (int i : TEST_SPLIT_OFFSETS) {
         List<FormattedText> list1 = stringsplitter.splitLines(p_97309_, p_97310_ - i, Style.EMPTY);
         float f1 = Math.abs(getMaxWidth(stringsplitter, list1) - p_97310_);
         if (f1 <= 10.0F) {
            return list1;
         }

         if (f1 < f) {
            f = f1;
            list = list1;
         }
      }

      return list;
   }

   private @Nullable AdvancementWidget getFirstVisibleParent(AdvancementNode p_297515_) {
      do {
         p_297515_ = p_297515_.parent();
      } while (p_297515_ != null && p_297515_.advancement().display().isEmpty());

      return p_297515_ != null && !p_297515_.advancement().display().isEmpty() ? this.tab.getWidget(p_297515_.holder()) : null;
   }

   public void drawConnectivity(GuiGraphics p_281947_, int p_97300_, int p_97301_, boolean p_97302_) {
      if (this.parent != null) {
         int i = p_97300_ + this.parent.x + 13;
         int j = p_97300_ + this.parent.x + 26 + 4;
         int k = p_97301_ + this.parent.y + 13;
         int l = p_97300_ + this.x + 13;
         int i1 = p_97301_ + this.y + 13;
         int j1 = p_97302_ ? -16777216 : -1;
         if (p_97302_) {
            p_281947_.hLine(j, i, k - 1, j1);
            p_281947_.hLine(j + 1, i, k, j1);
            p_281947_.hLine(j, i, k + 1, j1);
            p_281947_.hLine(l, j - 1, i1 - 1, j1);
            p_281947_.hLine(l, j - 1, i1, j1);
            p_281947_.hLine(l, j - 1, i1 + 1, j1);
            p_281947_.vLine(j - 1, i1, k, j1);
            p_281947_.vLine(j + 1, i1, k, j1);
         } else {
            p_281947_.hLine(j, i, k, j1);
            p_281947_.hLine(l, j, i1, j1);
            p_281947_.vLine(j, i1, k, j1);
         }
      }

      for (AdvancementWidget advancementwidget : this.children) {
         advancementwidget.drawConnectivity(p_281947_, p_97300_, p_97301_, p_97302_);
      }
   }

   public void draw(GuiGraphics p_281958_, int p_281323_, int p_283679_) {
      if (!this.display.isHidden() || this.progress != null && this.progress.isDone()) {
         float f = this.progress == null ? 0.0F : this.progress.getPercent();
         AdvancementWidgetType advancementwidgettype;
         if (f >= 1.0F) {
            advancementwidgettype = AdvancementWidgetType.OBTAINED;
         } else {
            advancementwidgettype = AdvancementWidgetType.UNOBTAINED;
         }

         p_281958_.blitSprite(
            RenderPipelines.GUI_TEXTURED, advancementwidgettype.frameSprite(this.display.getType()), p_281323_ + this.x + 3, p_283679_ + this.y, 26, 26
         );
         p_281958_.renderFakeItem(this.display.getIcon(), p_281323_ + this.x + 8, p_283679_ + this.y + 5);
      }

      for (AdvancementWidget advancementwidget : this.children) {
         advancementwidget.draw(p_281958_, p_281323_, p_283679_);
      }
   }

   public int getWidth() {
      return this.width;
   }

   public void setProgress(AdvancementProgress p_97265_) {
      this.progress = p_97265_;
   }

   public void addChild(AdvancementWidget p_97307_) {
      this.children.add(p_97307_);
   }

   public void drawHover(GuiGraphics p_283068_, int p_281304_, int p_281253_, float p_281848_, int p_282097_, int p_281537_) {
      Font font = this.minecraft.font;
      int i = 9 * this.titleLines.size() + 9 + 8;
      int j = p_281253_ + this.y + (26 - i) / 2;
      int k = j + i;
      int l = this.description.size() * 9;
      int i1 = 6 + l;
      boolean flag = p_282097_ + p_281304_ + this.x + this.width + 26 >= this.tab.getScreen().width;
      Component component = this.progress == null ? null : this.progress.getProgressText();
      int j1 = component == null ? 0 : font.width(component);
      boolean flag1 = k + i1 >= 113;
      float f = this.progress == null ? 0.0F : this.progress.getPercent();
      int k1 = Mth.floor(f * this.width);
      AdvancementWidgetType advancementwidgettype;
      AdvancementWidgetType advancementwidgettype1;
      AdvancementWidgetType advancementwidgettype2;
      if (f >= 1.0F) {
         k1 = this.width / 2;
         advancementwidgettype = AdvancementWidgetType.OBTAINED;
         advancementwidgettype1 = AdvancementWidgetType.OBTAINED;
         advancementwidgettype2 = AdvancementWidgetType.OBTAINED;
      } else if (k1 < 2) {
         k1 = this.width / 2;
         advancementwidgettype = AdvancementWidgetType.UNOBTAINED;
         advancementwidgettype1 = AdvancementWidgetType.UNOBTAINED;
         advancementwidgettype2 = AdvancementWidgetType.UNOBTAINED;
      } else if (k1 > this.width - 2) {
         k1 = this.width / 2;
         advancementwidgettype = AdvancementWidgetType.OBTAINED;
         advancementwidgettype1 = AdvancementWidgetType.OBTAINED;
         advancementwidgettype2 = AdvancementWidgetType.UNOBTAINED;
      } else {
         advancementwidgettype = AdvancementWidgetType.OBTAINED;
         advancementwidgettype1 = AdvancementWidgetType.UNOBTAINED;
         advancementwidgettype2 = AdvancementWidgetType.UNOBTAINED;
      }

      int l1 = this.width - k1;
      int i2;
      if (flag) {
         i2 = p_281304_ + this.x - this.width + 26 + 6;
      } else {
         i2 = p_281304_ + this.x;
      }

      int j2 = i + i1;
      if (!this.description.isEmpty()) {
         if (flag1) {
            p_283068_.blitSprite(RenderPipelines.GUI_TEXTURED, TITLE_BOX_SPRITE, i2, k - j2, this.width, j2);
         } else {
            p_283068_.blitSprite(RenderPipelines.GUI_TEXTURED, TITLE_BOX_SPRITE, i2, j, this.width, j2);
         }
      }

      if (advancementwidgettype != advancementwidgettype1) {
         p_283068_.blitSprite(RenderPipelines.GUI_TEXTURED, advancementwidgettype.boxSprite(), 200, i, 0, 0, i2, j, k1, i);
         p_283068_.blitSprite(RenderPipelines.GUI_TEXTURED, advancementwidgettype1.boxSprite(), 200, i, 200 - l1, 0, i2 + k1, j, l1, i);
      } else {
         p_283068_.blitSprite(RenderPipelines.GUI_TEXTURED, advancementwidgettype.boxSprite(), i2, j, this.width, i);
      }

      p_283068_.blitSprite(
         RenderPipelines.GUI_TEXTURED, advancementwidgettype2.frameSprite(this.display.getType()), p_281304_ + this.x + 3, p_281253_ + this.y, 26, 26
      );
      int k2 = i2 + 5;
      if (flag) {
         this.drawMultilineText(p_283068_, this.titleLines, k2, j + 9, -1);
         if (component != null) {
            p_283068_.drawString(font, component, p_281304_ + this.x - j1, j + 9, -1);
         }
      } else {
         this.drawMultilineText(p_283068_, this.titleLines, p_281304_ + this.x + 32, j + 9, -1);
         if (component != null) {
            p_283068_.drawString(font, component, p_281304_ + this.x + this.width - j1 - 5, j + 9, -1);
         }
      }

      if (flag1) {
         this.drawMultilineText(p_283068_, this.description, k2, j - l + 1, -16711936);
      } else {
         this.drawMultilineText(p_283068_, this.description, k2, k, -16711936);
      }

      p_283068_.renderFakeItem(this.display.getIcon(), p_281304_ + this.x + 8, p_281253_ + this.y + 5);
   }

   private void drawMultilineText(GuiGraphics p_377348_, List<FormattedCharSequence> p_376770_, int p_375430_, int p_378762_, int p_377335_) {
      Font font = this.minecraft.font;

      for (int i = 0; i < p_376770_.size(); i++) {
         p_377348_.drawString(font, p_376770_.get(i), p_375430_, p_378762_ + i * 9, p_377335_);
      }
   }

   public boolean isMouseOver(int p_97260_, int p_97261_, int p_97262_, int p_97263_) {
      if (!this.display.isHidden() || this.progress != null && this.progress.isDone()) {
         int i = p_97260_ + this.x;
         int j = i + 26;
         int k = p_97261_ + this.y;
         int l = k + 26;
         return p_97262_ >= i && p_97262_ <= j && p_97263_ >= k && p_97263_ <= l;
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
