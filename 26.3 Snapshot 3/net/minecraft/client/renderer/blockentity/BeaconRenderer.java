package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BeaconRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BeaconBeamOwner;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BeaconRenderer<T extends BlockEntity & BeaconBeamOwner> implements BlockEntityRenderer<T, BeaconRenderState> {
   public static final Identifier BEAM_LOCATION = Identifier.withDefaultNamespace("textures/entity/beacon/beacon_beam.png");
   public static final int MAX_RENDER_Y = 2048;
   private static final float BEAM_SCALE_THRESHOLD = 96.0F;
   public static final float SOLID_BEAM_RADIUS = 0.2F;
   public static final float BEAM_GLOW_RADIUS = 0.25F;

   public BeaconRenderState createRenderState() {
      return new BeaconRenderState();
   }

   public void extractRenderState(
      final T blockEntity,
      final BeaconRenderState state,
      final float partialTicks,
      final Vec3 cameraPosition,
      final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
   ) {
      BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
      extract(blockEntity, state, partialTicks, cameraPosition);
   }

   public static <T extends BlockEntity & BeaconBeamOwner> void extract(
      final T blockEntity, final BeaconRenderState state, final float partialTicks, final Vec3 cameraPosition
   ) {
      state.animationTime = blockEntity.getLevel() != null ? Math.floorMod(blockEntity.getLevel().getGameTime(), 40) + partialTicks : 0.0F;
      state.sections = blockEntity.getBeamSections().stream().map(section -> new BeaconRenderState.Section(section.getColor(), section.getHeight())).toList();
      float distanceToBeacon = (float)cameraPosition.subtract(Vec3.atCenterOf(state.blockPos)).horizontalDistance();
      LocalPlayer player = Minecraft.getInstance().player;
      state.beamRadiusScale = player != null && player.isScoping() ? 1.0F : Math.max(1.0F, distanceToBeacon / 96.0F);
   }

   public void submit(final BeaconRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera) {
      int beamStart = 0;

      for (int i = 0; i < state.sections.size(); i++) {
         BeaconRenderState.Section beamSection = state.sections.get(i);
         submitBeaconBeam(
            poseStack,
            submitNodeCollector,
            state.beamRadiusScale,
            state.animationTime,
            beamStart,
            i == state.sections.size() - 1 ? 2048 : beamSection.height(),
            beamSection.color()
         );
         beamStart += beamSection.height();
      }
   }

   private static void submitBeaconBeam(
      final PoseStack poseStack,
      final SubmitNodeCollector submitNodeCollector,
      final float beamRadiusScale,
      final float animationTime,
      final int beamStart,
      final int height,
      final int color
   ) {
      submitBeaconBeam(
         poseStack, submitNodeCollector, BEAM_LOCATION, 1.0F, animationTime, beamStart, height, color, 0.2F * beamRadiusScale, 0.25F * beamRadiusScale
      );
   }

   public static void submitBeaconBeam(
      final PoseStack poseStack,
      final SubmitNodeCollector submitNodeCollector,
      final Identifier beamLocation,
      final float scale,
      final float animationTime,
      final int beamStart,
      final int height,
      final int color,
      final float solidBeamRadius,
      final float beamGlowRadius
   ) {
      int beamEnd = beamStart + height;
      poseStack.pushPose();
      poseStack.translate(0.5, 0.0, 0.5);
      float scroll = height < 0 ? animationTime : -animationTime;
      float texVOff = Mth.frac(scroll * 0.2F - Mth.floor(scroll * 0.1F));
      poseStack.pushPose();
      poseStack.mulPose(Axis.YP.rotationDegrees(animationTime * 2.25F - 45.0F));
      float wnx = 0.0F;
      float wnz = solidBeamRadius;
      float enx = solidBeamRadius;
      float enz = 0.0F;
      float wsx = -solidBeamRadius;
      float wsz = 0.0F;
      float esx = 0.0F;
      float esz = -solidBeamRadius;
      float uu1 = 0.0F;
      float uu2 = 1.0F;
      float vv2 = -1.0F + texVOff;
      float vv1 = height * scale * (0.5F / solidBeamRadius) + vv2;
      submitNodeCollector.submitCustomGeometry(
         poseStack,
         RenderTypes.beaconBeam(beamLocation, false),
         (pose, buffer) -> renderPart(pose, buffer, color, beamStart, beamEnd, 0.0F, wnz, enx, 0.0F, wsx, 0.0F, 0.0F, esz, 0.0F, 1.0F, vv1, vv2)
      );
      poseStack.popPose();
      wnx = -beamGlowRadius;
      wnz = -beamGlowRadius;
      enx = beamGlowRadius;
      enz = -beamGlowRadius;
      wsx = -beamGlowRadius;
      wsz = beamGlowRadius;
      esx = beamGlowRadius;
      esz = beamGlowRadius;
      uu1 = 0.0F;
      uu2 = 1.0F;
      vv2 = -1.0F + texVOff;
      vv1 = height * scale + vv2;
      submitNodeCollector.submitCustomGeometry(
         poseStack,
         RenderTypes.beaconBeam(beamLocation, true),
         (pose, buffer) -> renderPart(pose, buffer, ARGB.color(32, color), beamStart, beamEnd, wnx, wnz, enx, enz, wsx, wsz, esx, esz, 0.0F, 1.0F, vv1, vv2)
      );
      poseStack.popPose();
   }

   private static void renderPart(
      final PoseStack.Pose pose,
      final VertexConsumer builder,
      final int color,
      final int beamStart,
      final int beamEnd,
      final float wnx,
      final float wnz,
      final float enx,
      final float enz,
      final float wsx,
      final float wsz,
      final float esx,
      final float esz,
      final float uu1,
      final float uu2,
      final float vv1,
      final float vv2
   ) {
      renderQuad(pose, builder, color, beamStart, beamEnd, wnx, wnz, enx, enz, uu1, uu2, vv1, vv2);
      renderQuad(pose, builder, color, beamStart, beamEnd, esx, esz, wsx, wsz, uu1, uu2, vv1, vv2);
      renderQuad(pose, builder, color, beamStart, beamEnd, enx, enz, esx, esz, uu1, uu2, vv1, vv2);
      renderQuad(pose, builder, color, beamStart, beamEnd, wsx, wsz, wnx, wnz, uu1, uu2, vv1, vv2);
   }

   private static void renderQuad(
      final PoseStack.Pose pose,
      final VertexConsumer builder,
      final int color,
      final int beamStart,
      final int beamEnd,
      final float wnx,
      final float wnz,
      final float enx,
      final float enz,
      final float uu1,
      final float uu2,
      final float vv1,
      final float vv2
   ) {
      addVertex(pose, builder, color, beamEnd, wnx, wnz, uu2, vv1);
      addVertex(pose, builder, color, beamStart, wnx, wnz, uu2, vv2);
      addVertex(pose, builder, color, beamStart, enx, enz, uu1, vv2);
      addVertex(pose, builder, color, beamEnd, enx, enz, uu1, vv1);
   }

   private static void addVertex(
      final PoseStack.Pose pose, final VertexConsumer builder, final int color, final int y, final float x, final float z, final float u, final float v
   ) {
      builder.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0.0F, 1.0F, 0.0F);
   }

   @Override
   public boolean shouldRenderOffScreen() {
      return true;
   }

   @Override
   public int getViewDistance() {
      return Minecraft.getInstance().options.getEffectiveRenderDistance() * 16;
   }

   @Override
   public boolean shouldRender(final T blockEntity, final Vec3 cameraPosition) {
      return Vec3.atCenterOf(blockEntity.getBlockPos()).multiply(1.0, 0.0, 1.0).closerThan(cameraPosition.multiply(1.0, 0.0, 1.0), this.getViewDistance());
   }
}
