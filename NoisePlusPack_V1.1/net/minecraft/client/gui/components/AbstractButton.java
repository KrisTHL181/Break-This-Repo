package net.minecraft.client.gui.components;

import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractButton extends AbstractWidget.WithInactiveMessage {
   protected static final int TEXT_MARGIN = 2;
   private static final WidgetSprites SPRITES = new WidgetSprites(
      Identifier.withDefaultNamespace("widget/button"),
      Identifier.withDefaultNamespace("widget/button_disabled"),
      Identifier.withDefaultNamespace("widget/button_highlighted")
   );
   private @Nullable Supplier<Boolean> overrideRenderHighlightedSprite;

   public AbstractButton(int p_93365_, int p_93366_, int p_93367_, int p_93368_, Component p_93369_) {
      super(p_93365_, p_93366_, p_93367_, p_93368_, p_93369_);
   }

   public abstract void onPress(InputWithModifiers var1);

   @Override
   protected final void renderWidget(GuiGraphics p_281670_, int p_282682_, int p_281714_, float p_282542_) {
      this.renderContents(p_281670_, p_282682_, p_281714_, p_282542_);
      this.handleCursor(p_281670_);
   }

   protected abstract void renderContents(GuiGraphics var1, int var2, int var3, float var4);

   protected void renderDefaultLabel(ActiveTextCollector p_453248_) {
      this.renderScrollingStringOverContents(p_453248_, this.getMessage(), 2);
   }

   protected final void renderDefaultSprite(GuiGraphics p_455641_) {
      p_455641_.blitSprite(
         RenderPipelines.GUI_TEXTURED,
         SPRITES.get(this.active, this.overrideRenderHighlightedSprite != null ? this.overrideRenderHighlightedSprite.get() : this.isHoveredOrFocused()),
         this.getX(),
         this.getY(),
         this.getWidth(),
         this.getHeight(),
         ARGB.white(this.alpha)
      );
   }

   @Override
   public void onClick(MouseButtonEvent p_426095_, boolean p_428686_) {
      this.onPress(p_426095_);
   }

   @Override
   public boolean keyPressed(KeyEvent p_427564_) {
      if (!this.isActive()) {
         return false;
      } else if (p_427564_.isSelection()) {
         this.playDownSound(Minecraft.getInstance().getSoundManager());
         this.onPress(p_427564_);
         return true;
      } else {
         return false;
      }
   }

   public void setOverrideRenderHighlightedSprite(Supplier<Boolean> p_453637_) {
      this.overrideRenderHighlightedSprite = p_453637_;
   }
}
