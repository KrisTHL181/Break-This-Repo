package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BeaconRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityWithBoundingBoxRenderState;
import net.minecraft.client.renderer.blockentity.state.TestInstanceRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class TestInstanceRenderer implements BlockEntityRenderer<TestInstanceBlockEntity, TestInstanceRenderState> {
   private static final float ERROR_PADDING = 0.02F;
   private final BeaconRenderer<TestInstanceBlockEntity> beacon = new BeaconRenderer<>();
   private final BlockEntityWithBoundingBoxRenderer<TestInstanceBlockEntity> box = new BlockEntityWithBoundingBoxRenderer<>();

   public TestInstanceRenderState createRenderState() {
      return new TestInstanceRenderState();
   }

   public void extractRenderState(
      final TestInstanceBlockEntity blockEntity,
      final TestInstanceRenderState state,
      final float partialTicks,
      final Vec3 cameraPosition,
      final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
   ) {
      BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
      state.beaconRenderState = new BeaconRenderState();
      BlockEntityRenderState.extractBase(blockEntity, state.beaconRenderState, breakProgress);
      BeaconRenderer.extract(blockEntity, state.beaconRenderState, partialTicks, cameraPosition);
      state.blockEntityWithBoundingBoxRenderState = new BlockEntityWithBoundingBoxRenderState();
      BlockEntityRenderState.extractBase(blockEntity, state.blockEntityWithBoundingBoxRenderState, breakProgress);
      BlockEntityWithBoundingBoxRenderer.extract(blockEntity, state.blockEntityWithBoundingBoxRenderState);
      state.errorMarkers.clear();

      for (TestInstanceBlockEntity.ErrorMarker marker : blockEntity.getErrorMarkers()) {
         state.errorMarkers.add(new TestInstanceBlockEntity.ErrorMarker(marker.pos(), marker.text()));
      }
   }

   public void submit(
      final TestInstanceRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera
   ) {
      this.beacon.submit(state.beaconRenderState, poseStack, submitNodeCollector, camera);
      this.box.submit(state.blockEntityWithBoundingBoxRenderState, poseStack, submitNodeCollector, camera);

      for (TestInstanceBlockEntity.ErrorMarker error : state.errorMarkers) {
         this.submitErrorMarker(error);
      }
   }

   private void submitErrorMarker(final TestInstanceBlockEntity.ErrorMarker error) {
      BlockPos pos = error.pos();
      Gizmos.cuboid(new AABB(pos).inflate(0.02F), GizmoStyle.fill(ARGB.colorFromFloat(0.375F, 1.0F, 0.0F, 0.0F)));
      String text = error.text().getString();
      float scale = 0.16F;
      Gizmos.billboardText(text, Vec3.atLowerCornerWithOffset(pos, 0.5, 1.2, 0.5), TextGizmo.Style.whiteAndCentered().withScale(0.16F)).setAlwaysOnTop();
   }

   @Override
   public boolean shouldRenderOffScreen() {
      return this.beacon.shouldRenderOffScreen() || this.box.shouldRenderOffScreen();
   }

   @Override
   public int getViewDistance() {
      return Math.max(this.beacon.getViewDistance(), this.box.getViewDistance());
   }

   public boolean shouldRender(final TestInstanceBlockEntity blockEntity, final Vec3 cameraPosition) {
      return this.beacon.shouldRender(blockEntity, cameraPosition) || this.box.shouldRender(blockEntity, cameraPosition);
   }
}
