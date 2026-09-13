package net.minecraft.client.gui.components;

import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public abstract class ImageWidget extends AbstractWidget {
   private ImageWidget(final int x, final int y, final int width, final int height) {
      super(x, y, width, height, CommonComponents.EMPTY);
   }

   public static ImageWidget texture(final int width, final int height, final Identifier texture, final int textureWidth, final int textureHeight) {
      return new ImageWidget.Texture(0, 0, width, height, texture, textureWidth, textureHeight);
   }

   public static ImageWidget sprite(final int width, final int height, final Identifier sprite) {
      return new ImageWidget.Sprite(0, 0, width, height, sprite);
   }

   @Override
   protected void updateWidgetNarration(final NarrationElementOutput output) {
   }

   @Override
   public void playDownSound(final SoundManager soundManager) {
   }

   @Override
   public boolean isActive() {
      return false;
   }

   public abstract void updateResource(Identifier identifier);

   @Override
   public @Nullable ComponentPath nextFocusPath(final FocusNavigationEvent navigationEvent) {
      return null;
   }

   private static class Sprite extends ImageWidget {
      private Identifier sprite;

      public Sprite(final int x, final int y, final int width, final int height, final Identifier sprite) {
         super(x, y, width, height);
         this.sprite = sprite;
      }

      @Override
      public void extractWidgetRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
         graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.sprite, this.getX(), this.getY(), this.getWidth(), this.getHeight());
      }

      @Override
      public void updateResource(final Identifier identifier) {
         this.sprite = identifier;
      }
   }

   private static class Texture extends ImageWidget {
      private Identifier texture;
      private final int textureWidth;
      private final int textureHeight;

      public Texture(final int x, final int y, final int width, final int height, final Identifier texture, final int textureWidth, final int textureHeight) {
         super(x, y, width, height);
         this.texture = texture;
         this.textureWidth = textureWidth;
         this.textureHeight = textureHeight;
      }

      @Override
      protected void extractWidgetRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
         graphics.blit(
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
      public void updateResource(final Identifier identifier) {
         this.texture = identifier;
      }
   }
}
