package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.bell.BellModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BellRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BellRenderer implements BlockEntityRenderer<BellBlockEntity, BellRenderState> {
   public static final SpriteId BELL_TEXTURE = Sheets.BLOCK_ENTITIES_MAPPER.defaultNamespaceApply("bell/bell_body");
   private final SpriteGetter sprites;
   private final BellModel model;

   public BellRenderer(final BlockEntityRendererProvider.Context context) {
      this.sprites = context.sprites();
      this.model = new BellModel(context.bakeLayer(ModelLayers.BELL));
   }

   public BellRenderState createRenderState() {
      return new BellRenderState();
   }

   public void extractRenderState(
      final BellBlockEntity blockEntity,
      final BellRenderState state,
      final float partialTicks,
      final Vec3 cameraPosition,
      final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
   ) {
      BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
      state.ticks = blockEntity.ticks + partialTicks;
      state.shakeDirection = blockEntity.shaking ? blockEntity.clickDirection : null;
   }

   public void submit(final BellRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera) {
      BellModel.State modelState = new BellModel.State(state.ticks, state.shakeDirection);
      this.model.setupAnim(modelState);
      submitNodeCollector.submitModel(this.model, modelState, poseStack, state.lightCoords, OverlayTexture.NO_OVERLAY, -1, BELL_TEXTURE, this.sprites, 0);
      if (state.breakProgress != null) {
         submitNodeCollector.order(1)
            .submitCrumblingOverlay(
               this.model,
               modelState,
               poseStack,
               BELL_TEXTURE.renderType(this.model.renderType()),
               state.lightCoords,
               OverlayTexture.NO_OVERLAY,
               -1,
               state.breakProgress
            );
      }
   }
}
