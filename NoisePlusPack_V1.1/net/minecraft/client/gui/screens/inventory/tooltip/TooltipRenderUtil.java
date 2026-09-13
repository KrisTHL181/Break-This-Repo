package net.minecraft.client.gui.screens.inventory.tooltip;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class TooltipRenderUtil {
   private static final Identifier BACKGROUND_SPRITE = Identifier.withDefaultNamespace("tooltip/background");
   private static final Identifier FRAME_SPRITE = Identifier.withDefaultNamespace("tooltip/frame");
   public static final int MOUSE_OFFSET = 12;
   private static final int PADDING = 3;
   public static final int PADDING_LEFT = 3;
   public static final int PADDING_RIGHT = 3;
   public static final int PADDING_TOP = 3;
   public static final int PADDING_BOTTOM = 3;
   private static final int MARGIN = 9;

   public static void renderTooltipBackground(GuiGraphics p_282666_, int p_281901_, int p_281846_, int p_281559_, int p_283336_, @Nullable Identifier p_460335_) {
      int i = p_281901_ - 3 - 9;
      int j = p_281846_ - 3 - 9;
      int k = p_281559_ + 3 + 3 + 18;
      int l = p_283336_ + 3 + 3 + 18;
      p_282666_.blitSprite(RenderPipelines.GUI_TEXTURED, getBackgroundSprite(p_460335_), i, j, k, l);
      p_282666_.blitSprite(RenderPipelines.GUI_TEXTURED, getFrameSprite(p_460335_), i, j, k, l);
   }

   private static Identifier getBackgroundSprite(@Nullable Identifier p_451953_) {
      return p_451953_ == null ? BACKGROUND_SPRITE : p_451953_.withPath(p_362641_ -> "tooltip/" + p_362641_ + "_background");
   }

   private static Identifier getFrameSprite(@Nullable Identifier p_456901_) {
      return p_456901_ == null ? FRAME_SPRITE : p_456901_.withPath(p_364578_ -> "tooltip/" + p_364578_ + "_frame");
   }
}
