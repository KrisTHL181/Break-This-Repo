package net.minecraft.client.gui.components.tabs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.TabButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

public class TabNavigationBar extends AbstractContainerWidget {
   private static final int NO_TAB = -1;
   private static final Component USAGE_NARRATION = Component.translatable("narration.tab_navigation.usage");
   protected final FrameLayout layout;
   private final TabManager tabManager;
   protected final ImmutableList<Tab> tabs;
   protected final ImmutableList<TabButton> tabButtons;

   protected TabNavigationBar(
      final int x,
      final int y,
      final int width,
      final int height,
      final TabManager tabManager,
      final ImmutableList<TabButton> tabButtons,
      final ImmutableList<Tab> tabs
   ) {
      super(x, y, width, height, CommonComponents.EMPTY);
      this.tabManager = tabManager;
      this.tabButtons = tabButtons;
      this.tabs = ImmutableList.copyOf(tabs);
      this.layout = new FrameLayout();
      this.layout.setPosition(x, y);
      LinearLayout linearLayout = this.layout.addChild(LinearLayout.horizontal());
      UnmodifiableIterator var9 = tabButtons.iterator();

      while (var9.hasNext()) {
         TabButton tabButton = (TabButton)var9.next();
         linearLayout.addChild(tabButton);
      }

      this.layout.arrangeElements();
   }

   @Override
   protected int contentHeight() {
      return this.height;
   }

   public static TabNavigationBar.Builder builder(final TabManager tabManager, final int x, final int y, final int width, final int height) {
      return new TabNavigationBar.Builder(tabManager, x, y, width, height);
   }

   public void arrangeElements(final int width) {
      this.layout.setPosition(this.getX(), this.getY());
      this.layout.arrangeElements();
   }

   @Override
   public boolean isMouseOver(final double mouseX, final double mouseY) {
      AtomicBoolean mouseOver = new AtomicBoolean();
      this.layout.visitChildren(child -> {
         if (child.getRectangle().containsPoint((int)mouseX, (int)mouseY)) {
            mouseOver.set(true);
         }
      });
      return mouseOver.get();
   }

   @Override
   public void setFocused(final boolean focused) {
      super.setFocused(focused);
      if (this.getFocused() != null) {
         this.setFocused(null);
      }
   }

   @Override
   public void setFocused(final @Nullable GuiEventListener focused) {
      super.setFocused(focused);
      if (focused instanceof TabButton button && button.isActive()) {
         this.tabManager.setCurrentTab(button.tab(), true);
      }
   }

   @Override
   public @Nullable ComponentPath nextFocusPath(final FocusNavigationEvent navigationEvent) {
      if (!this.isFocused()) {
         TabButton button = this.currentTabButton();
         if (button != null) {
            return ComponentPath.path(this, ComponentPath.leaf(button));
         }
      }

      return navigationEvent instanceof FocusNavigationEvent.TabNavigation ? null : super.nextFocusPath(navigationEvent);
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
   protected void updateWidgetNarration(final NarrationElementOutput output) {
      Optional<TabButton> selected = this.tabButtons
         .stream()
         .filter(AbstractWidget::isHovered)
         .findFirst()
         .or(() -> Optional.ofNullable(this.currentTabButton()));
      selected.ifPresent(button -> {
         this.narrateListElementPosition(output.nest(), button);
         button.updateNarration(output);
      });
      if (this.isFocused()) {
         output.add(NarratedElementType.USAGE, USAGE_NARRATION);
      }
   }

   protected void narrateListElementPosition(final NarrationElementOutput output, final TabButton widget) {
      if (this.tabs.size() > 1) {
         int index = this.tabButtons.indexOf(widget);
         if (index != -1) {
            output.add(NarratedElementType.POSITION, Component.translatable("narrator.position.tab", index + 1, this.tabs.size()));
         }
      }
   }

   @Override
   protected void extractWidgetRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      UnmodifiableIterator var5 = this.tabButtons.iterator();

      while (var5.hasNext()) {
         TabButton value = (TabButton)var5.next();
         value.extractRenderState(graphics, mouseX, mouseY, a);
      }
   }

   @Override
   public ScreenRectangle getRectangle() {
      return this.layout.getRectangle();
   }

   public void selectTab(final int index, final boolean playSound) {
      if (this.isFocused()) {
         this.setFocused((GuiEventListener)this.tabButtons.get(index));
      } else if (((TabButton)this.tabButtons.get(index)).isActive()) {
         this.tabManager.setCurrentTab((Tab)this.tabs.get(index), playSound);
      }
   }

   public void setTabActiveState(final int index, final boolean active) {
      if (index >= 0 && index < this.tabButtons.size()) {
         ((TabButton)this.tabButtons.get(index)).active = active;
      }
   }

   public void setTabTooltip(final int index, final @Nullable Tooltip hint) {
      if (index >= 0 && index < this.tabButtons.size()) {
         ((TabButton)this.tabButtons.get(index)).setTooltip(hint);
      }
   }

   @Override
   public boolean keyPressed(final KeyEvent event) {
      if (event.hasControlDownWithQuirk()) {
         int tabIndex = this.getNextTabIndex(event);
         if (tabIndex != -1) {
            this.selectTab(Mth.clamp(tabIndex, 0, this.tabs.size() - 1), true);
            return true;
         }
      }

      return false;
   }

   private int getNextTabIndex(final KeyEvent event) {
      return this.getNextTabIndex(this.currentTabIndex(), event);
   }

   private int getNextTabIndex(final int currentTab, final KeyEvent event) {
      int digit = event.getDigit();
      if (digit != -1) {
         return Math.floorMod(digit - 1, 10);
      } else if (event.isCycleFocus() && currentTab != -1) {
         int nextTabIndex = event.hasShiftDown() ? currentTab - 1 : currentTab + 1;
         int index = Math.floorMod(nextTabIndex, this.tabs.size());
         return ((TabButton)this.tabButtons.get(index)).active ? index : this.getNextTabIndex(index, event);
      } else {
         return -1;
      }
   }

   private int currentTabIndex() {
      Tab currentTab = this.tabManager.getCurrentTab();
      int index = this.tabs.indexOf(currentTab);
      return index != -1 ? index : -1;
   }

   private @Nullable TabButton currentTabButton() {
      int index = this.currentTabIndex();
      return index != -1 ? (TabButton)this.tabButtons.get(index) : null;
   }

   public static class Builder {
      protected final int x;
      protected final int y;
      protected final int width;
      protected final int height;
      protected final TabManager tabManager;
      protected final List<TabButton> tabButtons = new ArrayList<>();
      protected final List<Tab> tabs = new ArrayList<>();

      protected Builder(final TabManager tabManager, final int x, final int y, final int width, final int height) {
         this.tabManager = tabManager;
         this.x = x;
         this.y = y;
         this.width = width;
         this.height = height;
      }

      public TabNavigationBar.Builder addTab(final TabButton button, final Tab tab) {
         this.tabButtons.add(button);
         this.tabs.add(tab);
         return this;
      }

      public TabNavigationBar build() {
         return new TabNavigationBar(
            this.x, this.y, this.width, this.height, this.tabManager, ImmutableList.copyOf(this.tabButtons), ImmutableList.copyOf(this.tabs)
         );
      }
   }
}
