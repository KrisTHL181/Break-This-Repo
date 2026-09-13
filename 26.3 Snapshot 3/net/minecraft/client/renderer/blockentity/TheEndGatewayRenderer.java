package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.EndGatewayRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class TheEndGatewayRenderer extends AbstractEndPortalRenderer<TheEndGatewayBlockEntity, EndGatewayRenderState> {
   private static final Identifier BEAM_LOCATION = Identifier.withDefaultNamespace("textures/entity/end_portal/end_gateway_beam.png");

   public EndGatewayRenderState createRenderState() {
      return new EndGatewayRenderState();
   }

   public void extractRenderState(
      final TheEndGatewayBlockEntity blockEntity,
      final EndGatewayRenderState state,
      final float partialTicks,
      final Vec3 cameraPosition,
      final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
   ) {
      super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
      Level level = blockEntity.getLevel();
      if (blockEntity.isSpawning() || blockEntity.isCoolingDown() && level != null) {
         state.scale = blockEntity.isSpawning() ? blockEntity.getSpawnPercent(partialTicks) : blockEntity.getCooldownPercent(partialTicks);
         double beamDistance = blockEntity.isSpawning() ? blockEntity.getLevel().getMaxY() : 50.0;
         state.scale = Mth.sin(state.scale * (float) Math.PI);
         state.height = Mth.floor(state.scale * beamDistance);
         state.color = blockEntity.isSpawning() ? DyeColor.MAGENTA.getTextureDiffuseColor() : DyeColor.PURPLE.getTextureDiffuseColor();
         state.animationTime = blockEntity.getLevel() != null ? Math.floorMod(blockEntity.getLevel().getGameTime(), 40) + partialTicks : 0.0F;
      } else {
         state.height = 0;
      }
   }

   public void submit(
      final EndGatewayRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera
   ) {
      if (state.height > 0) {
         BeaconRenderer.submitBeaconBeam(
            poseStack, submitNodeCollector, BEAM_LOCATION, state.scale, state.animationTime, -state.height, state.height * 2, state.color, 0.15F, 0.175F
         );
      }

      submitCube(state.facesToShow, RenderTypes.endGateway(), poseStack, submitNodeCollector);
   }

   @Override
   public int getViewDistance() {
      return 256;
   }
}
