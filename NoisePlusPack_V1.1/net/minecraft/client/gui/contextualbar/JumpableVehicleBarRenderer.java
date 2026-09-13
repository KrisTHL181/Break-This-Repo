package net.minecraft.client.gui.contextualbar;

import java.util.Objects;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class JumpableVehicleBarRenderer implements ContextualBarRenderer {
   private static final Identifier JUMP_BAR_BACKGROUND_SPRITE = Identifier.withDefaultNamespace("hud/jump_bar_background");
   private static final Identifier JUMP_BAR_COOLDOWN_SPRITE = Identifier.withDefaultNamespace("hud/jump_bar_cooldown");
   private static final Identifier JUMP_BAR_PROGRESS_SPRITE = Identifier.withDefaultNamespace("hud/jump_bar_progress");
   private final Minecraft minecraft;
   private final PlayerRideableJumping playerJumpableVehicle;

   public JumpableVehicleBarRenderer(Minecraft p_407614_) {
      this.minecraft = p_407614_;
      this.playerJumpableVehicle = Objects.requireNonNull(Objects.requireNonNull(p_407614_.player).jumpableVehicle());
   }

   @Override
   public void renderBackground(GuiGraphics p_408568_, DeltaTracker p_409787_) {
      int i = this.left(this.minecraft.getWindow());
      int j = this.top(this.minecraft.getWindow());
      p_408568_.blitSprite(RenderPipelines.GUI_TEXTURED, JUMP_BAR_BACKGROUND_SPRITE, i, j, 182, 5);
      if (this.playerJumpableVehicle.getJumpCooldown() > 0) {
         p_408568_.blitSprite(RenderPipelines.GUI_TEXTURED, JUMP_BAR_COOLDOWN_SPRITE, i, j, 182, 5);
      } else {
         int k = Mth.lerpDiscrete(this.minecraft.player.getJumpRidingScale(), 0, 182);
         if (k > 0) {
            p_408568_.blitSprite(RenderPipelines.GUI_TEXTURED, JUMP_BAR_PROGRESS_SPRITE, 182, 5, 0, 0, i, j, k, 5);
         }
      }
   }

   @Override
   public void render(GuiGraphics p_406443_, DeltaTracker p_410030_) {
   }
}
