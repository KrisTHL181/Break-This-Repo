package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.book.BookModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.LecternRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class LecternRenderer implements BlockEntityRenderer<LecternBlockEntity, LecternRenderState> {
   private final SpriteGetter sprites;
   private final BookModel bookModel;
   private static final BookModel.State BOOK_STATE = BookModel.State.forAnimation(0.0F, 0.1F, 0.9F, 1.2F);

   public LecternRenderer(final BlockEntityRendererProvider.Context context) {
      this.sprites = context.sprites();
      this.bookModel = new BookModel(context.bakeLayer(ModelLayers.BOOK));
   }

   public LecternRenderState createRenderState() {
      return new LecternRenderState();
   }

   public void extractRenderState(
      final LecternBlockEntity blockEntity,
      final LecternRenderState state,
      final float partialTicks,
      final Vec3 cameraPosition,
      final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
   ) {
      BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
      state.hasBook = blockEntity.getBlockState().getValue(LecternBlock.HAS_BOOK);
      state.yRot = blockEntity.getBlockState().getValue(LecternBlock.FACING).getClockWise().toYRot();
   }

   public void submit(final LecternRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera) {
      if (state.hasBook) {
         poseStack.pushPose();
         poseStack.translate(0.5F, 1.0625F, 0.5F);
         poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
         poseStack.mulPose(Axis.ZP.rotationDegrees(67.5F));
         poseStack.translate(0.0F, -0.125F, 0.0F);
         submitNodeCollector.submitModel(
            this.bookModel, BOOK_STATE, poseStack, state.lightCoords, OverlayTexture.NO_OVERLAY, -1, EnchantTableRenderer.BOOK_TEXTURE, this.sprites, 0
         );
         if (state.breakProgress != null) {
            submitNodeCollector.order(1)
               .submitCrumblingOverlay(
                  this.bookModel,
                  BOOK_STATE,
                  poseStack,
                  EnchantTableRenderer.BOOK_TEXTURE.renderType(this.bookModel.renderType()),
                  state.lightCoords,
                  OverlayTexture.NO_OVERLAY,
                  -1,
                  state.breakProgress
               );
         }

         poseStack.popPose();
      }
   }
}
