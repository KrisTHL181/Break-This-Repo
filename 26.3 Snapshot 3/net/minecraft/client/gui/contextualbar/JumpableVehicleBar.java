package net.minecraft.client.gui.contextualbar;

import java.util.Objects;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.PlayerRideableJumping;

public class JumpableVehicleBar implements ContextualBar {
   private static final Identifier JUMP_BAR_BACKGROUND_SPRITE = Identifier.withDefaultNamespace("hud/jump_bar_background");
   private static final Identifier JUMP_BAR_COOLDOWN_SPRITE = Identifier.withDefaultNamespace("hud/jump_bar_cooldown");
   private static final Identifier JUMP_BAR_PROGRESS_SPRITE = Identifier.withDefaultNamespace("hud/jump_bar_progress");
   private final Minecraft minecraft;
   private final PlayerRideableJumping playerJumpableVehicle;

   public JumpableVehicleBar(final Minecraft minecraft) {
      this.minecraft = minecraft;
      this.playerJumpableVehicle = Objects.requireNonNull(Objects.requireNonNull(minecraft.player).jumpableVehicle());
   }

   @Override
   public void extractBackground(final GuiGraphicsExtractor graphics, final DeltaTracker deltaTracker) {
      int left = this.left(this.minecraft.getWindow());
      int top = this.top(this.minecraft.getWindow());
      graphics.blitSprite(RenderPipelines.GUI_TEXTURED, JUMP_BAR_BACKGROUND_SPRITE, left, top, 182, 5);
      if (this.playerJumpableVehicle.getJumpCooldown() > 0) {
         graphics.blitSprite(RenderPipelines.GUI_TEXTURED, JUMP_BAR_COOLDOWN_SPRITE, left, top, 182, 5);
      } else {
         int progress = Mth.lerpDiscrete(this.minecraft.player.getJumpRidingScale(), 0, 182);
         if (progress > 0) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, JUMP_BAR_PROGRESS_SPRITE, 182, 5, 0, 0, left, top, progress, 5);
         }
      }
   }

   @Override
   public void extractRenderState(final GuiGraphicsExtractor graphics, final DeltaTracker deltaTracker) {
   }
}
