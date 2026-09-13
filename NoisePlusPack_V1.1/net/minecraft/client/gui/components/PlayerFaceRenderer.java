package net.minecraft.client.gui.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PlayerFaceRenderer {
   public static final int SKIN_HEAD_U = 8;
   public static final int SKIN_HEAD_V = 8;
   public static final int SKIN_HEAD_WIDTH = 8;
   public static final int SKIN_HEAD_HEIGHT = 8;
   public static final int SKIN_HAT_U = 40;
   public static final int SKIN_HAT_V = 8;
   public static final int SKIN_HAT_WIDTH = 8;
   public static final int SKIN_HAT_HEIGHT = 8;
   public static final int SKIN_TEX_WIDTH = 64;
   public static final int SKIN_TEX_HEIGHT = 64;

   public static void draw(GuiGraphics p_281827_, PlayerSkin p_428752_, int p_282126_, int p_281693_, int p_281565_) {
      draw(p_281827_, p_428752_, p_282126_, p_281693_, p_281565_, -1);
   }

   public static void draw(GuiGraphics p_298949_, PlayerSkin p_430864_, int p_299931_, int p_299437_, int p_301021_, int p_422727_) {
      draw(p_298949_, p_430864_.body().texturePath(), p_299931_, p_299437_, p_301021_, true, false, p_422727_);
   }

   public static void draw(
      GuiGraphics p_283244_, Identifier p_454057_, int p_282035_, int p_282441_, int p_281801_, boolean p_283149_, boolean p_283555_, int p_361984_
   ) {
      int i = 8 + (p_283555_ ? 8 : 0);
      int j = 8 * (p_283555_ ? -1 : 1);
      p_283244_.blit(RenderPipelines.GUI_TEXTURED, p_454057_, p_282035_, p_282441_, 8.0F, i, p_281801_, p_281801_, 8, j, 64, 64, p_361984_);
      if (p_283149_) {
         drawHat(p_283244_, p_454057_, p_282035_, p_282441_, p_281801_, p_283555_, p_361984_);
      }
   }

   private static void drawHat(GuiGraphics p_282228_, Identifier p_456308_, int p_282585_, int p_282234_, int p_282576_, boolean p_281523_, int p_364001_) {
      int i = 8 + (p_281523_ ? 8 : 0);
      int j = 8 * (p_281523_ ? -1 : 1);
      p_282228_.blit(RenderPipelines.GUI_TEXTURED, p_456308_, p_282585_, p_282234_, 40.0F, i, p_282576_, p_282576_, 8, j, 64, 64, p_364001_);
   }
}
