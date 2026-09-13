package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public class BlockDecorationLayer<S extends EntityRenderState, M extends EntityModel<S>> extends RenderLayer<S, M> {
   private static final Matrix4fc UNIT_CUBE_BOTTOM_CENTER_TO_ANTENNA_CENTER = new Matrix4f()
      .translation(-0.5F, 0.0F, -0.5F)
      .rotateAround(Axis.ZP.rotationDegrees(180.0F), 0.5F, 0.5F, 0.5F);
   private final Function<S, BlockModelRenderState> blockModel;
   private final Consumer<PoseStack> transform;

   public BlockDecorationLayer(final RenderLayerParent<S, M> renderer, final Function<S, BlockModelRenderState> blockModel, final Consumer<PoseStack> transform) {
      super(renderer);
      this.blockModel = blockModel;
      this.transform = transform;
   }

   @Override
   public void submit(
      final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final int lightCoords, final S state, final float yRot, final float xRot
   ) {
      BlockModelRenderState blockModel = this.blockModel.apply(state);
      if (!blockModel.isEmpty()) {
         poseStack.pushPose();
         this.transform.accept(poseStack);
         poseStack.mulPose(UNIT_CUBE_BOTTOM_CENTER_TO_ANTENNA_CENTER);
         blockModel.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
         poseStack.popPose();
      }
   }
}
