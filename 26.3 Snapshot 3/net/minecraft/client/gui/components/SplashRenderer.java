package net.minecraft.client.gui.components;

import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.resources.SplashManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.joml.Matrix3x2f;

public class SplashRenderer {
   public static final SplashRenderer CHRISTMAS = new SplashRenderer(SplashManager.CHRISTMAS);
   public static final SplashRenderer NEW_YEAR = new SplashRenderer(SplashManager.NEW_YEAR);
   public static final SplashRenderer HALLOWEEN = new SplashRenderer(SplashManager.HALLOWEEN);
   private static final int WIDTH_OFFSET = 123;
   private static final int HEIGH_OFFSET = 69;
   private static final float TEXT_ANGLE = (float) (-Math.PI / 9);
   private final Component splash;

   public SplashRenderer(final Component splash) {
      this.splash = splash;
   }

   public void extractRenderState(final GuiGraphicsExtractor graphics, final int screenWidth, final Font font, final float alpha) {
      int textWidth = font.width(this.splash);
      ActiveTextCollector textRenderer = graphics.textRenderer();
      float textPhase = 1.8F - Mth.abs(Mth.sin((float)(Util.getMillis() % 1000L) / 1000.0F * (float) (Math.PI * 2)) * 0.1F);
      float textScale = textPhase * 100.0F / (textWidth + 32);
      Matrix3x2f transform = new Matrix3x2f(textRenderer.defaultParameters().pose())
         .translate(screenWidth / 2.0F + 123.0F, 69.0F)
         .rotate((float) (-Math.PI / 9))
         .scale(textScale);
      ActiveTextCollector.Parameters renderParameters = textRenderer.defaultParameters().withOpacity(alpha).withPose(transform);
      textRenderer.accept(TextAlignment.LEFT, -textWidth / 2, -8, renderParameters, this.splash);
   }
}
