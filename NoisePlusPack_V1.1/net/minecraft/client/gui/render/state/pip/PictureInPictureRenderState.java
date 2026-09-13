package net.minecraft.client.gui.render.state.pip;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.ScreenArea;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface PictureInPictureRenderState extends ScreenArea {
   Matrix3x2f IDENTITY_POSE = new Matrix3x2f();

   int x0();

   int x1();

   int y0();

   int y1();

   float scale();

   default Matrix3x2f pose() {
      return IDENTITY_POSE;
   }

   @Nullable ScreenRectangle scissorArea();

   static @Nullable ScreenRectangle getBounds(int p_408957_, int p_407518_, int p_408507_, int p_409335_, @Nullable ScreenRectangle p_407955_) {
      ScreenRectangle screenrectangle = new ScreenRectangle(p_408957_, p_407518_, p_408507_ - p_408957_, p_409335_ - p_407518_);
      return p_407955_ != null ? p_407955_.intersection(screenrectangle) : screenrectangle;
   }
}
