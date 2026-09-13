package net.minecraft.client.gui.components;

import com.google.common.collect.Lists;
import java.util.AbstractList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractSelectionList<E extends AbstractSelectionList.Entry<E>> extends AbstractContainerWidget {
   private static final Identifier MENU_LIST_BACKGROUND = Identifier.withDefaultNamespace("textures/gui/menu_list_background.png");
   private static final Identifier INWORLD_MENU_LIST_BACKGROUND = Identifier.withDefaultNamespace("textures/gui/inworld_menu_list_background.png");
   private static final int SEPARATOR_HEIGHT = 2;
   protected final Minecraft minecraft;
   protected final int defaultEntryHeight;
   private final List<E> children = new AbstractSelectionList.TrackedList();
   protected boolean centerListVertically = true;
   private @Nullable E selected;
   private @Nullable E hovered;

   public AbstractSelectionList(Minecraft p_93404_, int p_93405_, int p_93406_, int p_93407_, int p_93408_) {
      super(0, p_93407_, p_93405_, p_93406_, CommonComponents.EMPTY);
      this.minecraft = p_93404_;
      this.defaultEntryHeight = p_93408_;
   }

   public @Nullable E getSelected() {
      return this.selected;
   }

   public void setSelected(@Nullable E p_93462_) {
      this.selected = p_93462_;
      if (p_93462_ != null) {
         boolean flag = p_93462_.getContentY() < this.getY();
         boolean flag1 = p_93462_.getContentBottom() > this.getBottom();
         if (this.minecraft.getLastInputType().isKeyboard() || flag || flag1) {
            this.scrollToEntry(p_93462_);
         }
      }
   }

   public @Nullable E getFocused() {
      return (E)super.getFocused();
   }

   @Override
   public final List<E> children() {
      return Collections.unmodifiableList(this.children);
   }

   protected void sort(Comparator<E> p_430158_) {
      this.children.sort(p_430158_);
      this.repositionEntries();
   }

   protected void swap(int p_424339_, int p_425931_) {
      Collections.swap(this.children, p_424339_, p_425931_);
      this.repositionEntries();
      this.scrollToEntry(this.children.get(p_425931_));
   }

   protected void clearEntries() {
      this.children.clear();
      this.selected = null;
   }

   protected void clearEntriesExcept(E p_430434_) {
      this.children.removeIf(p_420703_ -> p_420703_ != p_430434_);
      if (this.selected != p_430434_) {
         this.setSelected(null);
      }
   }

   public void replaceEntries(Collection<E> p_93470_) {
      this.clearEntries();

      for (E e : p_93470_) {
         this.addEntry(e);
      }
   }

   private int getFirstEntryY() {
      return this.getY() + 2;
   }

   public int getNextY() {
      int i = this.getFirstEntryY() - (int)this.scrollAmount();

      for (E e : this.children) {
         i += e.getHeight();
      }

      return i;
   }

   protected int addEntry(E p_93487_) {
      return this.addEntry(p_93487_, this.defaultEntryHeight);
   }

   protected int addEntry(E p_430130_, int p_430091_) {
      p_430130_.setX(this.getRowLeft());
      p_430130_.setWidth(this.getRowWidth());
      p_430130_.setY(this.getNextY());
      p_430130_.setHeight(p_430091_);
      this.children.add(p_430130_);
      return this.children.size() - 1;
   }

   protected void addEntryToTop(E p_239858_) {
      this.addEntryToTop(p_239858_, this.defaultEntryHeight);
   }

   protected void addEntryToTop(E p_425294_, int p_431108_) {
      double d0 = this.maxScrollAmount() - this.scrollAmount();
      p_425294_.setHeight(p_431108_);
      this.children.addFirst(p_425294_);
      this.repositionEntries();
      this.setScrollAmount(this.maxScrollAmount() - d0);
   }

   private void repositionEntries() {
      int i = this.getFirstEntryY() - (int)this.scrollAmount();

      for (E e : this.children) {
         e.setY(i);
         i += e.getHeight();
         e.setX(this.getRowLeft());
         e.setWidth(this.getRowWidth());
      }
   }

   protected void removeEntryFromTop(E p_239046_) {
      double d0 = this.maxScrollAmount() - this.scrollAmount();
      this.removeEntry(p_239046_);
      this.setScrollAmount(this.maxScrollAmount() - d0);
   }

   protected int getItemCount() {
      return this.children().size();
   }

   protected boolean entriesCanBeSelected() {
      return true;
   }

   protected final @Nullable E getEntryAtPosition(double p_93413_, double p_93414_) {
      for (E e : this.children) {
         if (e.isMouseOver(p_93413_, p_93414_)) {
            return e;
         }
      }

      return null;
   }

   public void updateSize(int p_336225_, HeaderAndFooterLayout p_331081_) {
      this.updateSizeAndPosition(p_336225_, p_331081_.getContentHeight(), p_331081_.getHeaderHeight());
   }

   public void updateSizeAndPosition(int p_334988_, int p_333730_, int p_328806_) {
      this.updateSizeAndPosition(p_334988_, p_333730_, 0, p_328806_);
   }

   public void updateSizeAndPosition(int p_429081_, int p_430079_, int p_426198_, int p_428192_) {
      this.setSize(p_429081_, p_430079_);
      this.setPosition(p_426198_, p_428192_);
      this.repositionEntries();
      if (this.getSelected() != null) {
         this.scrollToEntry(this.getSelected());
      }

      this.refreshScrollAmount();
   }

   @Override
   protected int contentHeight() {
      int i = 0;

      for (E e : this.children) {
         i += e.getHeight();
      }

      return i + 4;
   }

   @Override
   public void renderWidget(GuiGraphics p_282708_, int p_283242_, int p_282891_, float p_283683_) {
      this.hovered = this.isMouseOver(p_283242_, p_282891_) ? this.getEntryAtPosition(p_283242_, p_282891_) : null;
      this.renderListBackground(p_282708_);
      this.enableScissor(p_282708_);
      this.renderListItems(p_282708_, p_283242_, p_282891_, p_283683_);
      p_282708_.disableScissor();
      this.renderListSeparators(p_282708_);
      this.renderScrollbar(p_282708_, p_283242_, p_282891_);
   }

   protected void renderListSeparators(GuiGraphics p_331270_) {
      Identifier identifier = this.minecraft.level == null ? Screen.HEADER_SEPARATOR : Screen.INWORLD_HEADER_SEPARATOR;
      Identifier identifier1 = this.minecraft.level == null ? Screen.FOOTER_SEPARATOR : Screen.INWORLD_FOOTER_SEPARATOR;
      p_331270_.blit(RenderPipelines.GUI_TEXTURED, identifier, this.getX(), this.getY() - 2, 0.0F, 0.0F, this.getWidth(), 2, 32, 2);
      p_331270_.blit(RenderPipelines.GUI_TEXTURED, identifier1, this.getX(), this.getBottom(), 0.0F, 0.0F, this.getWidth(), 2, 32, 2);
   }

   protected void renderListBackground(GuiGraphics p_333412_) {
      Identifier identifier = this.minecraft.level == null ? MENU_LIST_BACKGROUND : INWORLD_MENU_LIST_BACKGROUND;
      p_333412_.blit(
         RenderPipelines.GUI_TEXTURED,
         identifier,
         this.getX(),
         this.getY(),
         this.getRight(),
         this.getBottom() + (int)this.scrollAmount(),
         this.getWidth(),
         this.getHeight(),
         32,
         32
      );
   }

   protected void enableScissor(GuiGraphics p_282811_) {
      p_282811_.enableScissor(this.getX(), this.getY(), this.getRight(), this.getBottom());
   }

   protected void scrollToEntry(E p_429571_) {
      int i = p_429571_.getY() - this.getY() - 2;
      if (i < 0) {
         this.scroll(i);
      }

      int j = this.getBottom() - p_429571_.getY() - p_429571_.getHeight() - 2;
      if (j < 0) {
         this.scroll(-j);
      }
   }

   protected void centerScrollOn(E p_93495_) {
      int i = 0;

      for (E e : this.children) {
         if (e == p_93495_) {
            i += e.getHeight() / 2;
            break;
         }

         i += e.getHeight();
      }

      this.setScrollAmount(i - this.height / 2.0);
   }

   private void scroll(int p_93430_) {
      this.setScrollAmount(this.scrollAmount() + p_93430_);
   }

   @Override
   public void setScrollAmount(double p_429779_) {
      super.setScrollAmount(p_429779_);
      this.repositionEntries();
   }

   @Override
   protected double scrollRate() {
      return this.defaultEntryHeight / 2.0;
   }

   @Override
   protected int scrollBarX() {
      return this.getRowRight() + 6 + 2;
   }

   @Override
   public Optional<GuiEventListener> getChildAt(double p_376745_, double p_377088_) {
      return Optional.ofNullable(this.getEntryAtPosition(p_376745_, p_377088_));
   }

   @Override
   public void setFocused(boolean p_428475_) {
      super.setFocused(p_428475_);
      if (!p_428475_) {
         this.setFocused(null);
      }
   }

   @Override
   public void setFocused(@Nullable GuiEventListener p_265738_) {
      E e = this.getFocused();
      if (e != p_265738_ && e instanceof ContainerEventHandler containereventhandler) {
         containereventhandler.setFocused(null);
      }

      super.setFocused(p_265738_);
      int i = this.children.indexOf(p_265738_);
      if (i >= 0) {
         E e1 = this.children.get(i);
         this.setSelected(e1);
      }
   }

   protected @Nullable E nextEntry(ScreenDirection p_265160_) {
      return this.nextEntry(p_265160_, p_93510_ -> true);
   }

   protected @Nullable E nextEntry(ScreenDirection p_265210_, Predicate<E> p_265604_) {
      return this.nextEntry(p_265210_, p_265604_, this.getSelected());
   }

   protected @Nullable E nextEntry(ScreenDirection p_265159_, Predicate<E> p_265109_, @Nullable E p_265379_) {
      int i = switch (p_265159_) {
         case RIGHT, LEFT -> 0;
         case UP -> -1;
         case DOWN -> 1;
      };
      if (!this.children().isEmpty() && i != 0) {
         int j;
         if (p_265379_ == null) {
            j = i > 0 ? 0 : this.children().size() - 1;
         } else {
            j = this.children().indexOf(p_265379_) + i;
         }

         for (int k = j; k >= 0 && k < this.children.size(); k += i) {
            E e = this.children().get(k);
            if (p_265109_.test(e)) {
               return e;
            }
         }
      }

      return null;
   }

   protected void renderListItems(GuiGraphics p_282079_, int p_239229_, int p_239230_, float p_239231_) {
      for (E e : this.children) {
         if (e.getY() + e.getHeight() >= this.getY() && e.getY() <= this.getBottom()) {
            this.renderItem(p_282079_, p_239229_, p_239230_, p_239231_, e);
         }
      }
   }

   protected void renderItem(GuiGraphics p_282205_, int p_238966_, int p_238967_, float p_238968_, E p_423748_) {
      if (this.entriesCanBeSelected() && this.getSelected() == p_423748_) {
         int i = this.isFocused() ? -1 : -8355712;
         this.renderSelection(p_282205_, p_423748_, i);
      }

      p_423748_.renderContent(p_282205_, p_238966_, p_238967_, Objects.equals(this.hovered, p_423748_), p_238968_);
   }

   protected void renderSelection(GuiGraphics p_283589_, E p_427454_, int p_240142_) {
      int i = p_427454_.getX();
      int j = p_427454_.getY();
      int k = i + p_427454_.getWidth();
      int l = j + p_427454_.getHeight();
      p_283589_.fill(i, j, k, l, p_240142_);
      p_283589_.fill(i + 1, j + 1, k - 1, l - 1, -16777216);
   }

   public int getRowLeft() {
      return this.getX() + this.width / 2 - this.getRowWidth() / 2;
   }

   public int getRowRight() {
      return this.getRowLeft() + this.getRowWidth();
   }

   public int getRowTop(int p_93512_) {
      return this.children.get(p_93512_).getY();
   }

   public int getRowBottom(int p_93486_) {
      E e = this.children.get(p_93486_);
      return e.getY() + e.getHeight();
   }

   public int getRowWidth() {
      return 220;
   }

   @Override
   public NarratableEntry.NarrationPriority narrationPriority() {
      if (this.isFocused()) {
         return NarratableEntry.NarrationPriority.FOCUSED;
      } else {
         return this.hovered != null ? NarratableEntry.NarrationPriority.HOVERED : NarratableEntry.NarrationPriority.NONE;
      }
   }

   protected void removeEntries(List<E> p_427733_) {
      p_427733_.forEach(this::removeEntry);
   }

   protected void removeEntry(E p_93503_) {
      boolean flag = this.children.remove(p_93503_);
      if (flag) {
         this.repositionEntries();
         if (p_93503_ == this.getSelected()) {
            this.setSelected(null);
         }
      }
   }

   protected @Nullable E getHovered() {
      return this.hovered;
   }

   void bindEntryToSelf(AbstractSelectionList.Entry<E> p_93506_) {
      p_93506_.list = this;
   }

   protected void narrateListElementPosition(NarrationElementOutput p_168791_, E p_168792_) {
      List<E> list = this.children();
      if (list.size() > 1) {
         int i = list.indexOf(p_168792_);
         if (i != -1) {
            p_168791_.add(NarratedElementType.POSITION, Component.translatable("narrator.position.list", i + 1, list.size()));
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   protected abstract static class Entry<E extends AbstractSelectionList.Entry<E>> implements GuiEventListener, LayoutElement {
      public static final int CONTENT_PADDING = 2;
      private int x = 0;
      private int y = 0;
      private int width = 0;
      private int height;
      @Deprecated
      AbstractSelectionList<E> list;

      @Override
      public void setFocused(boolean p_265302_) {
      }

      @Override
      public boolean isFocused() {
         return this.list.getFocused() == this;
      }

      public abstract void renderContent(GuiGraphics var1, int var2, int var3, boolean var4, float var5);

      @Override
      public boolean isMouseOver(double p_93537_, double p_93538_) {
         return this.getRectangle().containsPoint((int)p_93537_, (int)p_93538_);
      }

      @Override
      public void setX(int p_426014_) {
         this.x = p_426014_;
      }

      @Override
      public void setY(int p_427849_) {
         this.y = p_427849_;
      }

      public void setWidth(int p_431442_) {
         this.width = p_431442_;
      }

      public void setHeight(int p_431103_) {
         this.height = p_431103_;
      }

      public int getContentX() {
         return this.getX() + 2;
      }

      public int getContentY() {
         return this.getY() + 2;
      }

      public int getContentHeight() {
         return this.getHeight() - 4;
      }

      public int getContentYMiddle() {
         return this.getContentY() + this.getContentHeight() / 2;
      }

      public int getContentBottom() {
         return this.getContentY() + this.getContentHeight();
      }

      public int getContentWidth() {
         return this.getWidth() - 4;
      }

      public int getContentXMiddle() {
         return this.getContentX() + this.getContentWidth() / 2;
      }

      public int getContentRight() {
         return this.getContentX() + this.getContentWidth();
      }

      @Override
      public int getX() {
         return this.x;
      }

      @Override
      public int getY() {
         return this.y;
      }

      @Override
      public int getWidth() {
         return this.width;
      }

      @Override
      public int getHeight() {
         return this.height;
      }

      @Override
      public void visitWidgets(Consumer<AbstractWidget> p_422685_) {
      }

      @Override
      public ScreenRectangle getRectangle() {
         return LayoutElement.super.getRectangle();
      }
   }

   @OnlyIn(Dist.CLIENT)
   class TrackedList extends AbstractList<E> {
      private final List<E> delegate = Lists.newArrayList();

      public E get(int p_93557_) {
         return this.delegate.get(p_93557_);
      }

      @Override
      public int size() {
         return this.delegate.size();
      }

      public E set(int p_93559_, E p_93560_) {
         E e = this.delegate.set(p_93559_, p_93560_);
         AbstractSelectionList.this.bindEntryToSelf(p_93560_);
         return e;
      }

      public void add(int p_93567_, E p_93568_) {
         this.delegate.add(p_93567_, p_93568_);
         AbstractSelectionList.this.bindEntryToSelf(p_93568_);
      }

      public E remove(int p_93565_) {
         return this.delegate.remove(p_93565_);
      }
   }
}
