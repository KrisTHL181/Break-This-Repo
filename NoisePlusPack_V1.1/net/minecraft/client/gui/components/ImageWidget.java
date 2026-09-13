package net.minecraft.client.gui.components;

import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class ImageWidget extends AbstractWidget {
   ImageWidget(int p_275550_, int p_275723_, int p_301266_, int p_297426_) {
      super(p_275550_, p_275723_, p_301266_, p_297426_, CommonComponents.EMPTY);
   }

   public static ImageWidget texture(int p_298293_, int p_301221_, Identifier p_459055_, int p_297694_, int p_300459_) {
      return new ImageWidget.Texture(0, 0, p_298293_, p_301221_, p_459055_, p_297694_, p_300459_);
   }

   public static ImageWidget sprite(int p_299633_, int p_299377_, Identifier p_457232_) {
      return new ImageWidget.Sprite(0, 0, p_299633_, p_299377_, p_457232_);
   }

   @Override
   protected void updateWidgetNarration(NarrationElementOutput p_275454_) {
   }

   @Override
   public void playDownSound(SoundManager p_297959_) {
   }

   @Override
   public boolean isActive() {
      return false;
   }

   public abstract void updateResource(Identifier var1);

   @Override
   public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent p_298071_) {
      return null;
   }

   @OnlyIn(Dist.CLIENT)
   static class Sprite extends ImageWidget {
      private Identifier sprite;

      public Sprite(int p_299930_, int p_297218_, int p_298462_, int p_297563_, Identifier p_456133_) {
         super(p_299930_, p_297218_, p_298462_, p_297563_);
         this.sprite = p_456133_;
      }

      @Override
      public void renderWidget(GuiGraphics p_298082_, int p_297761_, int p_298881_, float p_300382_) {
         p_298082_.blitSprite(RenderPipelines.GUI_TEXTURED, this.sprite, this.getX(), this.getY(), this.getWidth(), this.getHeight());
      }

      @Override
      public void updateResource(Identifier p_460809_) {
         this.sprite = p_460809_;
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class Texture extends ImageWidget {
      private Identifier texture;
      private final int textureWidth;
      private final int textureHeight;

      public Texture(int p_299083_, int p_301299_, int p_299901_, int p_299822_, Identifier p_455874_, int p_298841_, int p_297816_) {
         super(p_299083_, p_301299_, p_299901_, p_299822_);
         this.texture = p_455874_;
         this.textureWidth = p_298841_;
         this.textureHeight = p_297816_;
      }

      @Override
      protected void renderWidget(GuiGraphics p_301123_, int p_301197_, int p_299250_, float p_300781_) {
         p_301123_.blit(
            RenderPipelines.GUI_TEXTURED,
            this.texture,
            this.getX(),
            this.getY(),
            0.0F,
            0.0F,
            this.getWidth(),
            this.getHeight(),
            this.textureWidth,
            this.textureHeight
         );
      }

      @Override
      public void updateResource(Identifier p_450157_) {
         this.texture = p_450157_;
      }
   }
}
