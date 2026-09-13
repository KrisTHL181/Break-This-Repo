package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.SpawnerRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class SpawnerRenderer implements BlockEntityRenderer<SpawnerBlockEntity, SpawnerRenderState> {
   private final EntityRenderDispatcher entityRenderer;

   public SpawnerRenderer(final BlockEntityRendererProvider.Context context) {
      this.entityRenderer = context.entityRenderer();
   }

   public SpawnerRenderState createRenderState() {
      return new SpawnerRenderState();
   }

   public void extractRenderState(
      final SpawnerBlockEntity blockEntity,
      final SpawnerRenderState state,
      final float partialTicks,
      final Vec3 cameraPosition,
      final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
   ) {
      BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
      if (blockEntity.getLevel() != null) {
         BaseSpawner spawner = blockEntity.getSpawner();
         Entity displayEntity = spawner.getOrCreateDisplayEntity(blockEntity.getLevel(), blockEntity.getBlockPos());
         TrialSpawnerRenderer.extractSpawnerData(state, partialTicks, displayEntity, this.entityRenderer, spawner.getOSpin(), spawner.getSpin());
      }
   }

   public void submit(final SpawnerRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera) {
      if (state.displayEntity != null) {
         submitEntityInSpawner(poseStack, submitNodeCollector, state.displayEntity, this.entityRenderer, state.spin, state.scale, camera);
      }
   }

   public static void submitEntityInSpawner(
      final PoseStack poseStack,
      final SubmitNodeCollector submitNodeCollector,
      final EntityRenderState displayEntity,
      final EntityRenderDispatcher entityRenderer,
      final float spin,
      final float scale,
      final CameraRenderState camera
   ) {
      poseStack.pushPose();
      poseStack.translate(0.5F, 0.4F, 0.5F);
      poseStack.mulPose(Axis.YP.rotationDegrees(spin));
      poseStack.translate(0.0F, -0.2F, 0.0F);
      poseStack.mulPose(Axis.XP.rotationDegrees(-30.0F));
      poseStack.scale(scale, scale, scale);
      entityRenderer.submit(displayEntity, camera, 0.0, 0.0, 0.0, poseStack, submitNodeCollector);
      poseStack.popPose();
   }
}
