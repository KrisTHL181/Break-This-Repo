package net.minecraft.client.gui.contextualbar;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ExperienceBarRenderer implements ContextualBarRenderer {
   private static final Identifier EXPERIENCE_BAR_BACKGROUND_SPRITE = Identifier.withDefaultNamespace("hud/experience_bar_background");
   private static final Identifier EXPERIENCE_BAR_PROGRESS_SPRITE = Identifier.withDefaultNamespace("hud/experience_bar_progress");
   private final Minecraft minecraft;

   public ExperienceBarRenderer(Minecraft p_406616_) {
      this.minecraft = p_406616_;
   }

   @Override
   public void renderBackground(GuiGraphics p_408172_, DeltaTracker p_408158_) {
      LocalPlayer localplayer = this.minecraft.player;
      int i = this.left(this.minecraft.getWindow());
      int j = this.top(this.minecraft.getWindow());
      int k = localplayer.getXpNeededForNextLevel();
      if (k > 0) {
         int l = (int)(localplayer.experienceProgress * 183.0F);
         p_408172_.blitSprite(RenderPipelines.GUI_TEXTURED, EXPERIENCE_BAR_BACKGROUND_SPRITE, i, j, 182, 5);
         if (l > 0) {
            p_408172_.blitSprite(RenderPipelines.GUI_TEXTURED, EXPERIENCE_BAR_PROGRESS_SPRITE, 182, 5, 0, 0, i, j, l, 5);
         }
      }
   }

   @Override
   public void render(GuiGraphics p_410598_, DeltaTracker p_407263_) {
   }
}
