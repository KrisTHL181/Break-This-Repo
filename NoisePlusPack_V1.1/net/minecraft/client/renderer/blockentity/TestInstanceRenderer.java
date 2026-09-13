package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BeaconRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityWithBoundingBoxRenderState;
import net.minecraft.client.renderer.blockentity.state.TestInstanceRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class TestInstanceRenderer implements BlockEntityRenderer<TestInstanceBlockEntity, TestInstanceRenderState> {
   private static final float ERROR_PADDING = 0.02F;
   private final BeaconRenderer<TestInstanceBlockEntity> beacon = new BeaconRenderer<>();
   private final BlockEntityWithBoundingBoxRenderer<TestInstanceBlockEntity> box = new BlockEntityWithBoundingBoxRenderer<>();

   public TestInstanceRenderState createRenderState() {
      return new TestInstanceRenderState();
   }

   public void extractRenderState(
      TestInstanceBlockEntity p_427666_,
      TestInstanceRenderState p_422610_,
      float p_423856_,
      Vec3 p_428165_,
      ModelFeatureRenderer.@Nullable CrumblingOverlay p_423288_
   ) {
      BlockEntityRenderer.super.extractRenderState(p_427666_, p_422610_, p_423856_, p_428165_, p_423288_);
      p_422610_.beaconRenderState = new BeaconRenderState();
      BlockEntityRenderState.extractBase(p_427666_, p_422610_.beaconRenderState, p_423288_);
      BeaconRenderer.extract(p_427666_, p_422610_.beaconRenderState, p_423856_, p_428165_);
      p_422610_.blockEntityWithBoundingBoxRenderState = new BlockEntityWithBoundingBoxRenderState();
      BlockEntityRenderState.extractBase(p_427666_, p_422610_.blockEntityWithBoundingBoxRenderState, p_423288_);
      BlockEntityWithBoundingBoxRenderer.extract(p_427666_, p_422610_.blockEntityWithBoundingBoxRenderState);
      p_422610_.errorMarkers.clear();

      for (TestInstanceBlockEntity.ErrorMarker testinstanceblockentity$errormarker : p_427666_.getErrorMarkers()) {
         p_422610_.errorMarkers
            .add(new TestInstanceBlockEntity.ErrorMarker(testinstanceblockentity$errormarker.pos(), testinstanceblockentity$errormarker.text()));
      }
   }

   public void submit(TestInstanceRenderState p_424985_, PoseStack p_427676_, SubmitNodeCollector p_423525_, CameraRenderState p_430847_) {
      this.beacon.submit(p_424985_.beaconRenderState, p_427676_, p_423525_, p_430847_);
      this.box.submit(p_424985_.blockEntityWithBoundingBoxRenderState, p_427676_, p_423525_, p_430847_);

      for (TestInstanceBlockEntity.ErrorMarker testinstanceblockentity$errormarker : p_424985_.errorMarkers) {
         this.submitErrorMarker(testinstanceblockentity$errormarker);
      }
   }

   private void submitErrorMarker(TestInstanceBlockEntity.ErrorMarker p_427897_) {
      BlockPos blockpos = p_427897_.pos();
      Gizmos.cuboid(new AABB(blockpos).inflate(0.02F), GizmoStyle.fill(ARGB.colorFromFloat(0.375F, 1.0F, 0.0F, 0.0F)));
      String s = p_427897_.text().getString();
      float f = 0.16F;
      Gizmos.billboardText(s, Vec3.atLowerCornerWithOffset(blockpos, 0.5, 1.2, 0.5), TextGizmo.Style.whiteAndCentered().withScale(0.16F)).setAlwaysOnTop();
   }

   @Override
   public boolean shouldRenderOffScreen() {
      return this.beacon.shouldRenderOffScreen() || this.box.shouldRenderOffScreen();
   }

   @Override
   public int getViewDistance() {
      return Math.max(this.beacon.getViewDistance(), this.box.getViewDistance());
   }

   public boolean shouldRender(TestInstanceBlockEntity p_393815_, Vec3 p_394435_) {
      return this.beacon.shouldRender(p_393815_, p_394435_) || this.box.shouldRender(p_393815_, p_394435_);
   }
}
