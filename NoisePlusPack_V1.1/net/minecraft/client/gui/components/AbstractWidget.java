package net.minecraft.client.gui.components;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import java.time.Duration;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractWidget implements Renderable, GuiEventListener, LayoutElement, NarratableEntry {
   protected int width;
   protected int height;
   private int x;
   private int y;
   protected Component message;
   protected boolean isHovered;
   public boolean active = true;
   public boolean visible = true;
   protected float alpha = 1.0F;
   private int tabOrderGroup;
   private boolean focused;
   private final WidgetTooltipHolder tooltip = new WidgetTooltipHolder();

   public AbstractWidget(int p_93629_, int p_93630_, int p_93631_, int p_93632_, Component p_93633_) {
      this.x = p_93629_;
      this.y = p_93630_;
      this.width = p_93631_;
      this.height = p_93632_;
      this.message = p_93633_;
   }

   @Override
   public int getHeight() {
      return this.height;
   }

   @Override
   public final void render(GuiGraphics p_282421_, int p_93658_, int p_93659_, float p_93660_) {
      if (this.visible) {
         this.isHovered = p_282421_.containsPointInScissor(p_93658_, p_93659_) && this.areCoordinatesInRectangle(p_93658_, p_93659_);
         this.renderWidget(p_282421_, p_93658_, p_93659_, p_93660_);
         this.tooltip.refreshTooltipForNextRenderPass(p_282421_, p_93658_, p_93659_, this.isHovered(), this.isFocused(), this.getRectangle());
      }
   }

   protected void handleCursor(GuiGraphics p_454454_) {
      if (this.isHovered()) {
         p_454454_.requestCursor(this.isActive() ? CursorTypes.POINTING_HAND : CursorTypes.NOT_ALLOWED);
      }
   }

   public void setTooltip(@Nullable Tooltip p_259796_) {
      this.tooltip.set(p_259796_);
   }

   public void setTooltipDelay(Duration p_334848_) {
      this.tooltip.setDelay(p_334848_);
   }

   protected MutableComponent createNarrationMessage() {
      return wrapDefaultNarrationMessage(this.getMessage());
   }

   public static MutableComponent wrapDefaultNarrationMessage(Component p_168800_) {
      return Component.translatable("gui.narrate.button", p_168800_);
   }

   protected abstract void renderWidget(GuiGraphics var1, int var2, int var3, float var4);

   protected void renderScrollingStringOverContents(ActiveTextCollector p_457911_, Component p_455843_, int p_452191_) {
      int i = this.getX() + p_452191_;
      int j = this.getX() + this.getWidth() - p_452191_;
      int k = this.getY();
      int l = this.getY() + this.getHeight();
      p_457911_.acceptScrollingWithDefaultCenter(p_455843_, i, j, k, l);
   }

   public void onClick(MouseButtonEvent p_426483_, boolean p_429202_) {
   }

   public void onRelease(MouseButtonEvent p_429715_) {
   }

   protected void onDrag(MouseButtonEvent p_422629_, double p_93636_, double p_93637_) {
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent p_431414_, boolean p_430750_) {
      if (!this.isActive()) {
         return false;
      }

      if (this.isValidClickButton(p_431414_.buttonInfo())) {
         boolean flag = this.isMouseOver(p_431414_.x(), p_431414_.y());
         if (flag) {
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            this.onClick(p_431414_, p_430750_);
            return true;
         }
      }

      return false;
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent p_422526_) {
      if (this.isValidClickButton(p_422526_.buttonInfo())) {
         this.onRelease(p_422526_);
         return true;
      } else {
         return false;
      }
   }

   protected boolean isValidClickButton(MouseButtonInfo p_422530_) {
      return p_422530_.button() == 0;
   }

   @Override
   public boolean mouseDragged(MouseButtonEvent p_427872_, double p_93645_, double p_93646_) {
      if (this.isValidClickButton(p_427872_.buttonInfo())) {
         this.onDrag(p_427872_, p_93645_, p_93646_);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent p_265640_) {
      if (!this.isActive()) {
         return null;
      } else {
         return !this.isFocused() ? ComponentPath.leaf(this) : null;
      }
   }

   @Override
   public boolean isMouseOver(double p_93672_, double p_93673_) {
      return this.isActive() && this.areCoordinatesInRectangle(p_93672_, p_93673_);
   }

   public void playDownSound(SoundManager p_93665_) {
      playButtonClickSound(p_93665_);
   }

   public static void playButtonClickSound(SoundManager p_363924_) {
      p_363924_.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
   }

   @Override
   public int getWidth() {
      return this.width;
   }

   public void setWidth(int p_93675_) {
      this.width = p_93675_;
   }

   public void setHeight(int p_298443_) {
      this.height = p_298443_;
   }

   public void setAlpha(float p_93651_) {
      this.alpha = p_93651_;
   }

   public float getAlpha() {
      return this.alpha;
   }

   public void setMessage(Component p_93667_) {
      this.message = p_93667_;
   }

   public Component getMessage() {
      return this.message;
   }

   @Override
   public boolean isFocused() {
      return this.focused;
   }

   public boolean isHovered() {
      return this.isHovered;
   }

   public boolean isHoveredOrFocused() {
      return this.isHovered() || this.isFocused();
   }

   @Override
   public boolean isActive() {
      return this.visible && this.active;
   }

   @Override
   public void setFocused(boolean p_93693_) {
      this.focused = p_93693_;
   }

   @Override
   public NarratableEntry.NarrationPriority narrationPriority() {
      if (this.isFocused()) {
         return NarratableEntry.NarrationPriority.FOCUSED;
      } else {
         return this.isHovered ? NarratableEntry.NarrationPriority.HOVERED : NarratableEntry.NarrationPriority.NONE;
      }
   }

   @Override
   public final void updateNarration(NarrationElementOutput p_259921_) {
      this.updateWidgetNarration(p_259921_);
      this.tooltip.updateNarration(p_259921_);
   }

   protected abstract void updateWidgetNarration(NarrationElementOutput var1);

   protected void defaultButtonNarrationText(NarrationElementOutput p_168803_) {
      p_168803_.add(NarratedElementType.TITLE, this.createNarrationMessage());
      if (this.active) {
         if (this.isFocused()) {
            p_168803_.add(NarratedElementType.USAGE, Component.translatable("narration.button.usage.focused"));
         } else {
            p_168803_.add(NarratedElementType.USAGE, Component.translatable("narration.button.usage.hovered"));
         }
      }
   }

   @Override
   public int getX() {
      return this.x;
   }

   @Override
   public void setX(int p_254495_) {
      this.x = p_254495_;
   }

   @Override
   public int getY() {
      return this.y;
   }

   @Override
   public void setY(int p_253718_) {
      this.y = p_253718_;
   }

   public int getRight() {
      return this.getX() + this.getWidth();
   }

   public int getBottom() {
      return this.getY() + this.getHeight();
   }

   @Override
   public void visitWidgets(Consumer<AbstractWidget> p_265566_) {
      p_265566_.accept(this);
   }

   public void setSize(int p_312975_, int p_312301_) {
      this.width = p_312975_;
      this.height = p_312301_;
   }

   @Override
   public ScreenRectangle getRectangle() {
      return LayoutElement.super.getRectangle();
   }

   private boolean areCoordinatesInRectangle(double p_408770_, double p_408112_) {
      return p_408770_ >= this.getX() && p_408112_ >= this.getY() && p_408770_ < this.getRight() && p_408112_ < this.getBottom();
   }

   public void setRectangle(int p_309908_, int p_310169_, int p_312247_, int p_310380_) {
      this.setSize(p_309908_, p_310169_);
      this.setPosition(p_312247_, p_310380_);
   }

   @Override
   public int getTabOrderGroup() {
      return this.tabOrderGroup;
   }

   public void setTabOrderGroup(int p_268123_) {
      this.tabOrderGroup = p_268123_;
   }

   @OnlyIn(Dist.CLIENT)
   public abstract static class WithInactiveMessage extends AbstractWidget {
      private Component inactiveMessage;

      public static Component defaultInactiveMessage(Component p_459182_) {
         return ComponentUtils.mergeStyles(p_459182_, Style.EMPTY.withColor(-6250336));
      }

      public WithInactiveMessage(int p_458860_, int p_451498_, int p_451195_, int p_459115_, Component p_453063_) {
         super(p_458860_, p_451498_, p_451195_, p_459115_, p_453063_);
         this.inactiveMessage = defaultInactiveMessage(p_453063_);
      }

      @Override
      public Component getMessage() {
         return this.active ? super.getMessage() : this.inactiveMessage;
      }

      @Override
      public void setMessage(Component p_459365_) {
         super.setMessage(p_459365_);
         this.inactiveMessage = defaultInactiveMessage(p_459365_);
      }
   }
}
