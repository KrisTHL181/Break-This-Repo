package net.minecraft.client.gui.contextualbar;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public class ExperienceBar implements ContextualBar {
   private static final Identifier EXPERIENCE_BAR_BACKGROUND_SPRITE = Identifier.withDefaultNamespace("hud/experience_bar_background");
   private static final Identifier EXPERIENCE_BAR_PROGRESS_SPRITE = Identifier.withDefaultNamespace("hud/experience_bar_progress");
   private final Minecraft minecraft;

   public ExperienceBar(final Minecraft minecraft) {
      this.minecraft = minecraft;
   }

   @Override
   public void extractBackground(final GuiGraphicsExtractor graphics, final DeltaTracker deltaTracker) {
      LocalPlayer player = this.minecraft.player;
      int left = this.left(this.minecraft.getWindow());
      int top = this.top(this.minecraft.getWindow());
      int xpNeededForNextLevel = player.getXpNeededForNextLevel();
      if (xpNeededForNextLevel > 0) {
         int progress = (int)(player.experienceProgress * 183.0F);
         graphics.blitSprite(RenderPipelines.GUI_TEXTURED, EXPERIENCE_BAR_BACKGROUND_SPRITE, left, top, 182, 5);
         if (progress > 0) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, EXPERIENCE_BAR_PROGRESS_SPRITE, 182, 5, 0, 0, left, top, progress, 5);
         }
      }
   }

   @Override
   public void extractRenderState(final GuiGraphicsExtractor graphics, final DeltaTracker deltaTracker) {
   }
}
