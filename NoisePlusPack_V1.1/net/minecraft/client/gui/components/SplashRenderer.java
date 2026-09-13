package net.minecraft.client.gui.components;

import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.resources.SplashManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3x2f;

@OnlyIn(Dist.CLIENT)
public class SplashRenderer {
   public static final SplashRenderer CHRISTMAS = new SplashRenderer(SplashManager.CHRISTMAS);
   public static final SplashRenderer NEW_YEAR = new SplashRenderer(SplashManager.NEW_YEAR);
   public static final SplashRenderer HALLOWEEN = new SplashRenderer(SplashManager.HALLOWEEN);
   private static final int WIDTH_OFFSET = 123;
   private static final int HEIGH_OFFSET = 69;
   private static final float TEXT_ANGLE = (float) (-Math.PI / 9);
   private final Component splash;

   public SplashRenderer(Component p_460629_) {
      this.splash = p_460629_;
   }

   public void render(GuiGraphics p_282218_, int p_281824_, Font p_281962_, float p_410206_) {
      int i = p_281962_.width(this.splash);
      ActiveTextCollector activetextcollector = p_282218_.textRenderer();
      float f = 1.8F - Mth.abs(Mth.sin((float)(Util.getMillis() % 1000L) / 1000.0F * (float) (Math.PI * 2)) * 0.1F);
      float f1 = f * 100.0F / (i + 32);
      Matrix3x2f matrix3x2f = new Matrix3x2f(activetextcollector.defaultParameters().pose())
         .translate(p_281824_ / 2.0F + 123.0F, 69.0F)
         .rotate((float) (-Math.PI / 9))
         .scale(f1);
      ActiveTextCollector.Parameters activetextcollector$parameters = activetextcollector.defaultParameters().withOpacity(p_410206_).withPose(matrix3x2f);
      activetextcollector.accept(TextAlignment.LEFT, -i / 2, -8, activetextcollector$parameters, this.splash);
   }
}
