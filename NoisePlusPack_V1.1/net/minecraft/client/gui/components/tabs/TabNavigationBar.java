package net.minecraft.client.gui.components.tabs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.TabButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class TabNavigationBar extends AbstractContainerEventHandler implements Renderable, NarratableEntry {
   private static final int NO_TAB = -1;
   private static final int MAX_WIDTH = 400;
   private static final int HEIGHT = 24;
   private static final int MARGIN = 14;
   private static final Component USAGE_NARRATION = Component.translatable("narration.tab_navigation.usage");
   private final LinearLayout layout = LinearLayout.horizontal();
   private int width;
   private final TabManager tabManager;
   private final ImmutableList<Tab> tabs;
   private final ImmutableList<TabButton> tabButtons;

   TabNavigationBar(int p_275379_, TabManager p_275624_, Iterable<Tab> p_275279_) {
      this.width = p_275379_;
      this.tabManager = p_275624_;
      this.tabs = ImmutableList.copyOf(p_275279_);
      this.layout.defaultCellSetting().alignHorizontallyCenter();
      com.google.common.collect.ImmutableList.Builder<TabButton> builder = ImmutableList.builder();

      for (Tab tab : p_275279_) {
         builder.add(this.layout.addChild(new TabButton(p_275624_, tab, 0, 24)));
      }

      this.tabButtons = builder.build();
   }

   public static TabNavigationBar.Builder builder(TabManager p_268126_, int p_268070_) {
      return new TabNavigationBar.Builder(p_268126_, p_268070_);
   }

   public void setWidth(int p_268094_) {
      this.width = p_268094_;
   }

   @Override
   public boolean isMouseOver(double p_378802_, double p_376598_) {
      return p_378802_ >= this.layout.getX()
         && p_376598_ >= this.layout.getY()
         && p_378802_ < this.layout.getX() + this.layout.getWidth()
         && p_376598_ < this.layout.getY() + this.layout.getHeight();
   }

   @Override
   public void setFocused(boolean p_275488_) {
      super.setFocused(p_275488_);
      if (this.getFocused() != null) {
         this.setFocused(null);
      }
   }

   @Override
   public void setFocused(@Nullable GuiEventListener p_275675_) {
      super.setFocused(p_275675_);
      if (p_275675_ instanceof TabButton tabbutton && tabbutton.isActive()) {
         this.tabManager.setCurrentTab(tabbutton.tab(), true);
      }
   }

   @Override
   public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent p_275418_) {
      if (!this.isFocused()) {
         TabButton tabbutton = this.currentTabButton();
         if (tabbutton != null) {
            return ComponentPath.path(this, ComponentPath.leaf(tabbutton));
         }
      }

      return p_275418_ instanceof FocusNavigationEvent.TabNavigation ? null : super.nextFocusPath(p_275418_);
   }

   @Override
   public List<? extends GuiEventListener> children() {
      return this.tabButtons;
   }

   public List<Tab> getTabs() {
      return this.tabs;
   }

   @Override
   public NarratableEntry.NarrationPriority narrationPriority() {
      return this.tabButtons.stream().map(AbstractWidget::narrationPriority).max(Comparator.naturalOrder()).orElse(NarratableEntry.NarrationPriority.NONE);
   }

   @Override
   public void updateNarration(NarrationElementOutput p_275583_) {
      Optional<TabButton> optional = this.tabButtons
         .stream()
         .filter(AbstractWidget::isHovered)
         .findFirst()
         .or(() -> Optional.ofNullable(this.currentTabButton()));
      optional.ifPresent(p_274663_ -> {
         this.narrateListElementPosition(p_275583_.nest(), p_274663_);
         p_274663_.updateNarration(p_275583_);
      });
      if (this.isFocused()) {
         p_275583_.add(NarratedElementType.USAGE, USAGE_NARRATION);
      }
   }

   protected void narrateListElementPosition(NarrationElementOutput p_275386_, TabButton p_275397_) {
      if (this.tabs.size() > 1) {
         int i = this.tabButtons.indexOf(p_275397_);
         if (i != -1) {
            p_275386_.add(NarratedElementType.POSITION, Component.translatable("narrator.position.tab", i + 1, this.tabs.size()));
         }
      }
   }

   @Override
   public void render(GuiGraphics p_281720_, int p_282085_, int p_281687_, float p_283048_) {
      p_281720_.blit(
         RenderPipelines.GUI_TEXTURED,
         Screen.HEADER_SEPARATOR,
         0,
         this.layout.getY() + this.layout.getHeight() - 2,
         0.0F,
         0.0F,
         ((TabButton)this.tabButtons.get(0)).getX(),
         2,
         32,
         2
      );
      int i = ((TabButton)this.tabButtons.get(this.tabButtons.size() - 1)).getRight();
      p_281720_.blit(
         RenderPipelines.GUI_TEXTURED, Screen.HEADER_SEPARATOR, i, this.layout.getY() + this.layout.getHeight() - 2, 0.0F, 0.0F, this.width, 2, 32, 2
      );
      UnmodifiableIterator var6 = this.tabButtons.iterator();

      while (var6.hasNext()) {
         TabButton tabbutton = (TabButton)var6.next();
         tabbutton.render(p_281720_, p_282085_, p_281687_, p_283048_);
      }
   }

   @Override
   public ScreenRectangle getRectangle() {
      return this.layout.getRectangle();
   }

   public void arrangeElements() {
      int i = Math.min(400, this.width) - 28;
      int j = Mth.roundToward(i / this.tabs.size(), 2);
      UnmodifiableIterator var3 = this.tabButtons.iterator();

      while (var3.hasNext()) {
         TabButton tabbutton = (TabButton)var3.next();
         tabbutton.setWidth(j);
      }

      this.layout.arrangeElements();
      this.layout.setX(Mth.roundToward((this.width - i) / 2, 2));
      this.layout.setY(0);
   }

   public void selectTab(int p_276107_, boolean p_276125_) {
      if (this.isFocused()) {
         this.setFocused((GuiEventListener)this.tabButtons.get(p_276107_));
      } else if (((TabButton)this.tabButtons.get(p_276107_)).isActive()) {
         this.tabManager.setCurrentTab((Tab)this.tabs.get(p_276107_), p_276125_);
      }
   }

   public void setTabActiveState(int p_408007_, boolean p_408320_) {
      if (p_408007_ >= 0 && p_408007_ < this.tabButtons.size()) {
         ((TabButton)this.tabButtons.get(p_408007_)).active = p_408320_;
      }
   }

   public void setTabTooltip(int p_405954_, @Nullable Tooltip p_407356_) {
      if (p_405954_ >= 0 && p_405954_ < this.tabButtons.size()) {
         ((TabButton)this.tabButtons.get(p_405954_)).setTooltip(p_407356_);
      }
   }

   @Override
   public boolean keyPressed(KeyEvent p_424577_) {
      if (p_424577_.hasControlDownWithQuirk()) {
         int i = this.getNextTabIndex(p_424577_);
         if (i != -1) {
            this.selectTab(Mth.clamp(i, 0, this.tabs.size() - 1), true);
            return true;
         }
      }

      return false;
   }

   private int getNextTabIndex(KeyEvent p_427927_) {
      return this.getNextTabIndex(this.currentTabIndex(), p_427927_);
   }

   private int getNextTabIndex(int p_410346_, KeyEvent p_427765_) {
      int i = p_427765_.getDigit();
      if (i != -1) {
         return Math.floorMod(i - 1, 10);
      } else if (p_427765_.isCycleFocus() && p_410346_ != -1) {
         int j = p_427765_.hasShiftDown() ? p_410346_ - 1 : p_410346_ + 1;
         int k = Math.floorMod(j, this.tabs.size());
         return ((TabButton)this.tabButtons.get(k)).active ? k : this.getNextTabIndex(k, p_427765_);
      } else {
         return -1;
      }
   }

   private int currentTabIndex() {
      Tab tab = this.tabManager.getCurrentTab();
      int i = this.tabs.indexOf(tab);
      return i != -1 ? i : -1;
   }

   private @Nullable TabButton currentTabButton() {
      int i = this.currentTabIndex();
      return i != -1 ? (TabButton)this.tabButtons.get(i) : null;
   }

   @OnlyIn(Dist.CLIENT)
   public static class Builder {
      private final int width;
      private final TabManager tabManager;
      private final List<Tab> tabs = new ArrayList<>();

      Builder(TabManager p_268334_, int p_267986_) {
         this.tabManager = p_268334_;
         this.width = p_267986_;
      }

      public TabNavigationBar.Builder addTabs(Tab... p_268144_) {
         Collections.addAll(this.tabs, p_268144_);
         return this;
      }

      public TabNavigationBar build() {
         return new TabNavigationBar(this.width, this.tabManager, this.tabs);
      }
   }
}
