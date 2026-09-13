package net.minecraft.client.gui.components;

import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class SpriteIconButton extends Button {
   protected final WidgetSprites sprite;
   protected final int spriteWidth;
   protected final int spriteHeight;

   SpriteIconButton(
      int p_297620_,
      int p_300275_,
      Component p_297544_,
      int p_298263_,
      int p_299223_,
      WidgetSprites p_426556_,
      Button.OnPress p_297736_,
      @Nullable Component p_425844_,
      Button.@Nullable CreateNarration p_335316_
   ) {
      super(0, 0, p_297620_, p_300275_, p_297544_, p_297736_, p_335316_ == null ? DEFAULT_NARRATION : p_335316_);
      if (p_425844_ != null) {
         this.setTooltip(Tooltip.create(p_425844_));
      }

      this.spriteWidth = p_298263_;
      this.spriteHeight = p_299223_;
      this.sprite = p_426556_;
   }

   protected void renderSprite(GuiGraphics p_458840_, int p_452781_, int p_452704_) {
      p_458840_.blitSprite(
         RenderPipelines.GUI_TEXTURED,
         this.sprite.get(this.isActive(), this.isHoveredOrFocused()),
         p_452781_,
         p_452704_,
         this.spriteWidth,
         this.spriteHeight,
         this.alpha
      );
   }

   public static SpriteIconButton.Builder builder(Component p_299964_, Button.OnPress p_301369_, boolean p_298501_) {
      return new SpriteIconButton.Builder(p_299964_, p_301369_, p_298501_);
   }

   @OnlyIn(Dist.CLIENT)
   public static class Builder {
      private final Component message;
      private final Button.OnPress onPress;
      private final boolean iconOnly;
      private int width = 150;
      private int height = 20;
      private @Nullable WidgetSprites sprite;
      private int spriteWidth;
      private int spriteHeight;
      private @Nullable Component tooltip;
      private Button.@Nullable CreateNarration narration;

      public Builder(Component p_298778_, Button.OnPress p_297973_, boolean p_297512_) {
         this.message = p_298778_;
         this.onPress = p_297973_;
         this.iconOnly = p_297512_;
      }

      public SpriteIconButton.Builder width(int p_298805_) {
         this.width = p_298805_;
         return this;
      }

      public SpriteIconButton.Builder size(int p_301312_, int p_297726_) {
         this.width = p_301312_;
         this.height = p_297726_;
         return this;
      }

      public SpriteIconButton.Builder sprite(Identifier p_455976_, int p_301308_, int p_297593_) {
         this.sprite = new WidgetSprites(p_455976_);
         this.spriteWidth = p_301308_;
         this.spriteHeight = p_297593_;
         return this;
      }

      public SpriteIconButton.Builder sprite(WidgetSprites p_425122_, int p_423359_, int p_426123_) {
         this.sprite = p_425122_;
         this.spriteWidth = p_423359_;
         this.spriteHeight = p_426123_;
         return this;
      }

      public SpriteIconButton.Builder withTootip() {
         this.tooltip = this.message;
         return this;
      }

      public SpriteIconButton.Builder narration(Button.CreateNarration p_328959_) {
         this.narration = p_328959_;
         return this;
      }

      public SpriteIconButton build() {
         if (this.sprite == null) {
            throw new IllegalStateException("Sprite not set");
         } else {
            return this.iconOnly
               ? new SpriteIconButton.CenteredIcon(
                  this.width, this.height, this.message, this.spriteWidth, this.spriteHeight, this.sprite, this.onPress, this.tooltip, this.narration
               )
               : new SpriteIconButton.TextAndIcon(
                  this.width, this.height, this.message, this.spriteWidth, this.spriteHeight, this.sprite, this.onPress, this.tooltip, this.narration
               );
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class CenteredIcon extends SpriteIconButton {
      protected CenteredIcon(
         int p_300200_,
         int p_299056_,
         Component p_298209_,
         int p_300001_,
         int p_298255_,
         WidgetSprites p_427853_,
         Button.OnPress p_298485_,
         @Nullable Component p_423465_,
         Button.@Nullable CreateNarration p_328314_
      ) {
         super(p_300200_, p_299056_, p_298209_, p_300001_, p_298255_, p_427853_, p_298485_, p_423465_, p_328314_);
      }

      @Override
      public void renderContents(GuiGraphics p_456240_, int p_451405_, int p_450864_, float p_458591_) {
         this.renderDefaultSprite(p_456240_);
         int i = this.getX() + this.getWidth() / 2 - this.spriteWidth / 2;
         int j = this.getY() + this.getHeight() / 2 - this.spriteHeight / 2;
         this.renderSprite(p_456240_, i, j);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class TextAndIcon extends SpriteIconButton {
      protected TextAndIcon(
         int p_299028_,
         int p_300372_,
         Component p_297448_,
         int p_300274_,
         int p_301370_,
         WidgetSprites p_430624_,
         Button.OnPress p_298623_,
         @Nullable Component p_424546_,
         Button.@Nullable CreateNarration p_328187_
      ) {
         super(p_299028_, p_300372_, p_297448_, p_300274_, p_301370_, p_430624_, p_298623_, p_424546_, p_328187_);
      }

      @Override
      public void renderContents(GuiGraphics p_460455_, int p_452292_, int p_459576_, float p_451682_) {
         this.renderDefaultSprite(p_460455_);
         int i = this.getX() + 2;
         int j = this.getX() + this.getWidth() - this.spriteWidth - 4;
         int k = this.getX() + this.getWidth() / 2;
         ActiveTextCollector activetextcollector = p_460455_.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE);
         activetextcollector.acceptScrolling(this.getMessage(), k, i, j, this.getY(), this.getY() + this.getHeight());
         int l = this.getX() + this.getWidth() - this.spriteWidth - 2;
         int i1 = this.getY() + this.getHeight() / 2 - this.spriteHeight / 2;
         this.renderSprite(p_460455_, l, i1);
      }
   }
}
