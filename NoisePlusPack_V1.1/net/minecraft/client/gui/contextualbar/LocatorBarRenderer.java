package net.minecraft.client.gui.contextualbar;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.WaypointStyle;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.waypoints.PartialTickSupplier;
import net.minecraft.world.waypoints.TrackedWaypoint;
import net.minecraft.world.waypoints.Waypoint;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LocatorBarRenderer implements ContextualBarRenderer {
   private static final Identifier LOCATOR_BAR_BACKGROUND = Identifier.withDefaultNamespace("hud/locator_bar_background");
   private static final Identifier LOCATOR_BAR_ARROW_UP = Identifier.withDefaultNamespace("hud/locator_bar_arrow_up");
   private static final Identifier LOCATOR_BAR_ARROW_DOWN = Identifier.withDefaultNamespace("hud/locator_bar_arrow_down");
   private static final int DOT_SIZE = 9;
   private static final int VISIBLE_DEGREE_RANGE = 60;
   private static final int ARROW_WIDTH = 7;
   private static final int ARROW_HEIGHT = 5;
   private static final int ARROW_LEFT = 1;
   private static final int ARROW_PADDING = 1;
   private final Minecraft minecraft;

   public LocatorBarRenderer(Minecraft p_409053_) {
      this.minecraft = p_409053_;
   }

   @Override
   public void renderBackground(GuiGraphics p_410535_, DeltaTracker p_408703_) {
      p_410535_.blitSprite(
         RenderPipelines.GUI_TEXTURED, LOCATOR_BAR_BACKGROUND, this.left(this.minecraft.getWindow()), this.top(this.minecraft.getWindow()), 182, 5
      );
   }

   @Override
   public void render(GuiGraphics p_410559_, DeltaTracker p_405979_) {
      int i = this.top(this.minecraft.getWindow());
      Entity entity = this.minecraft.getCameraEntity();
      if (entity != null) {
         Level level = entity.level();
         TickRateManager tickratemanager = level.tickRateManager();
         PartialTickSupplier partialticksupplier = p_420720_ -> p_405979_.getGameTimeDeltaPartialTick(!tickratemanager.isEntityFrozen(p_420720_));
         this.minecraft
            .player
            .connection
            .getWaypointManager()
            .forEachWaypoint(
               entity,
               p_420715_ -> {
                  if (!p_420715_.id().left().map(p_420717_ -> p_420717_.equals(entity.getUUID())).orElse(false)) {
                     double d0 = p_420715_.yawAngleToCamera(level, this.minecraft.gameRenderer.getMainCamera(), partialticksupplier);
                     if (!(d0 <= -60.0) && !(d0 > 60.0)) {
                        int j = Mth.ceil((p_410559_.guiWidth() - 9) / 2.0F);
                        Waypoint.Icon waypoint$icon = p_420715_.icon();
                        WaypointStyle waypointstyle = this.minecraft.getWaypointStyles().get(waypoint$icon.style);
                        float f = Mth.sqrt((float)p_420715_.distanceSquared(entity));
                        Identifier identifier = waypointstyle.sprite(f);
                        int k = waypoint$icon.color
                           .orElseGet(
                              () -> (Integer)p_420715_.id()
                                 .map(
                                    p_406769_ -> ARGB.setBrightness(ARGB.color(255, p_406769_.hashCode()), 0.9F),
                                    p_407645_ -> ARGB.setBrightness(ARGB.color(255, p_407645_.hashCode()), 0.9F)
                                 )
                           );
                        int l = Mth.floor(d0 * 173.0 / 2.0 / 60.0);
                        p_410559_.blitSprite(RenderPipelines.GUI_TEXTURED, identifier, j + l, i - 2, 9, 9, k);
                        TrackedWaypoint.PitchDirection trackedwaypoint$pitchdirection = p_420715_.pitchDirectionToCamera(
                           level, this.minecraft.gameRenderer, partialticksupplier
                        );
                        if (trackedwaypoint$pitchdirection != TrackedWaypoint.PitchDirection.NONE) {
                           int i1;
                           Identifier identifier1;
                           if (trackedwaypoint$pitchdirection == TrackedWaypoint.PitchDirection.DOWN) {
                              i1 = 6;
                              identifier1 = LOCATOR_BAR_ARROW_DOWN;
                           } else {
                              i1 = -6;
                              identifier1 = LOCATOR_BAR_ARROW_UP;
                           }

                           p_410559_.blitSprite(RenderPipelines.GUI_TEXTURED, identifier1, j + l + 1, i + i1, 7, 5);
                        }
                     }
                  }
               }
            );
      }
   }
}
