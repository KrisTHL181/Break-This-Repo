package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.PaintingRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.world.level.Level;

public class PaintingRenderer extends EntityRenderer<Painting, PaintingRenderState> {
   private static final Identifier BACK_SPRITE_LOCATION = Identifier.withDefaultNamespace("back");
   private final TextureAtlas paintingsAtlas;

   public PaintingRenderer(final EntityRendererProvider.Context context) {
      super(context);
      this.paintingsAtlas = context.getAtlas(AtlasIds.PAINTINGS);
   }

   public void submit(final PaintingRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera) {
      PaintingVariant variant = state.variant;
      if (variant != null) {
         poseStack.pushPose();
         poseStack.mulPose(Axis.YP.rotationDegrees(180 - state.direction.get2DDataValue() * 90));
         TextureAtlasSprite frontSprite = this.paintingsAtlas.getSprite(variant.assetId());
         TextureAtlasSprite backSprite = this.paintingsAtlas.getSprite(BACK_SPRITE_LOCATION);
         this.renderPainting(
            poseStack,
            submitNodeCollector,
            RenderTypes.entitySolidZOffsetForward(backSprite.atlasLocation()),
            state.lightCoordsPerBlock,
            variant.width(),
            variant.height(),
            frontSprite,
            backSprite
         );
         poseStack.popPose();
         super.submit(state, poseStack, submitNodeCollector, camera);
      }
   }

   public PaintingRenderState createRenderState() {
      return new PaintingRenderState();
   }

   public void extractRenderState(final Painting entity, final PaintingRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      Direction direction = entity.getDirection();
      PaintingVariant variant = entity.getVariant().value();
      state.direction = direction;
      state.variant = variant;
      int width = variant.width();
      int height = variant.height();
      if (state.lightCoordsPerBlock.length != width * height) {
         state.lightCoordsPerBlock = new int[width * height];
      }

      float offsetX = -width / 2.0F;
      float offsetY = -height / 2.0F;
      Level level = entity.level();

      for (int segmentY = 0; segmentY < height; segmentY++) {
         for (int segmentX = 0; segmentX < width; segmentX++) {
            float segmentOffsetX = segmentX + offsetX + 0.5F;
            float segmentOffsetY = segmentY + offsetY + 0.5F;
            int x = entity.getBlockX();
            int y = Mth.floor(entity.getY() + segmentOffsetY);
            int z = entity.getBlockZ();
            switch (direction) {
               case NORTH:
                  x = Mth.floor(entity.getX() + segmentOffsetX);
                  break;
               case WEST:
                  z = Mth.floor(entity.getZ() - segmentOffsetX);
                  break;
               case SOUTH:
                  x = Mth.floor(entity.getX() - segmentOffsetX);
                  break;
               case EAST:
                  z = Mth.floor(entity.getZ() + segmentOffsetX);
            }

            state.lightCoordsPerBlock[segmentX + segmentY * width] = LightCoordsUtil.getLightCoords(level, new BlockPos(x, y, z));
         }
      }
   }

   private void renderPainting(
      final PoseStack poseStack,
      final SubmitNodeCollector submitNodeCollector,
      final RenderType renderType,
      final int[] lightCoordsMap,
      final int width,
      final int height,
      final TextureAtlasSprite front,
      final TextureAtlasSprite back
   ) {
      submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
         float offsetX = -width / 2.0F;
         float offsetY = -height / 2.0F;
         float edgeHalfWidth = 0.03125F;
         float backU0 = back.getU0();
         float backU1 = back.getU1();
         float backV0 = back.getV0();
         float backV1 = back.getV1();
         float topBottomU0 = back.getU0();
         float topBottomU1 = back.getU1();
         float topBottomV0 = back.getV0();
         float topBottomV1 = back.getV(0.0625F);
         float leftRightU0 = back.getU0();
         float leftRightU1 = back.getU(0.0625F);
         float leftRightV0 = back.getV0();
         float leftRightV1 = back.getV1();
         double deltaU = 1.0 / width;
         double deltaV = 1.0 / height;

         for (int segmentX = 0; segmentX < width; segmentX++) {
            for (int segmentY = 0; segmentY < height; segmentY++) {
               float x0 = offsetX + (segmentX + 1);
               float x1 = offsetX + segmentX;
               float y0 = offsetY + (segmentY + 1);
               float y1 = offsetY + segmentY;
               int lightCoords = lightCoordsMap[segmentX + segmentY * width];
               float frontU0 = front.getU((float)(deltaU * (width - segmentX)));
               float frontU1 = front.getU((float)(deltaU * (width - (segmentX + 1))));
               float frontV0 = front.getV((float)(deltaV * (height - segmentY)));
               float frontV1 = front.getV((float)(deltaV * (height - (segmentY + 1))));
               vertex(pose, buffer, x0, y1, frontU1, frontV0, -0.03125F, 0, 0, -1, lightCoords);
               vertex(pose, buffer, x1, y1, frontU0, frontV0, -0.03125F, 0, 0, -1, lightCoords);
               vertex(pose, buffer, x1, y0, frontU0, frontV1, -0.03125F, 0, 0, -1, lightCoords);
               vertex(pose, buffer, x0, y0, frontU1, frontV1, -0.03125F, 0, 0, -1, lightCoords);
               vertex(pose, buffer, x0, y0, backU1, backV0, 0.03125F, 0, 0, 1, lightCoords);
               vertex(pose, buffer, x1, y0, backU0, backV0, 0.03125F, 0, 0, 1, lightCoords);
               vertex(pose, buffer, x1, y1, backU0, backV1, 0.03125F, 0, 0, 1, lightCoords);
               vertex(pose, buffer, x0, y1, backU1, backV1, 0.03125F, 0, 0, 1, lightCoords);
               if (segmentY == height - 1) {
                  vertex(pose, buffer, x0, y0, topBottomU0, topBottomV0, -0.03125F, 0, 1, 0, lightCoords);
                  vertex(pose, buffer, x1, y0, topBottomU1, topBottomV0, -0.03125F, 0, 1, 0, lightCoords);
                  vertex(pose, buffer, x1, y0, topBottomU1, topBottomV1, 0.03125F, 0, 1, 0, lightCoords);
                  vertex(pose, buffer, x0, y0, topBottomU0, topBottomV1, 0.03125F, 0, 1, 0, lightCoords);
               }

               if (segmentY == 0) {
                  vertex(pose, buffer, x0, y1, topBottomU0, topBottomV0, 0.03125F, 0, -1, 0, lightCoords);
                  vertex(pose, buffer, x1, y1, topBottomU1, topBottomV0, 0.03125F, 0, -1, 0, lightCoords);
                  vertex(pose, buffer, x1, y1, topBottomU1, topBottomV1, -0.03125F, 0, -1, 0, lightCoords);
                  vertex(pose, buffer, x0, y1, topBottomU0, topBottomV1, -0.03125F, 0, -1, 0, lightCoords);
               }

               if (segmentX == width - 1) {
                  vertex(pose, buffer, x0, y0, leftRightU1, leftRightV0, 0.03125F, -1, 0, 0, lightCoords);
                  vertex(pose, buffer, x0, y1, leftRightU1, leftRightV1, 0.03125F, -1, 0, 0, lightCoords);
                  vertex(pose, buffer, x0, y1, leftRightU0, leftRightV1, -0.03125F, -1, 0, 0, lightCoords);
                  vertex(pose, buffer, x0, y0, leftRightU0, leftRightV0, -0.03125F, -1, 0, 0, lightCoords);
               }

               if (segmentX == 0) {
                  vertex(pose, buffer, x1, y0, leftRightU1, leftRightV0, -0.03125F, 1, 0, 0, lightCoords);
                  vertex(pose, buffer, x1, y1, leftRightU1, leftRightV1, -0.03125F, 1, 0, 0, lightCoords);
                  vertex(pose, buffer, x1, y1, leftRightU0, leftRightV1, 0.03125F, 1, 0, 0, lightCoords);
                  vertex(pose, buffer, x1, y0, leftRightU0, leftRightV0, 0.03125F, 1, 0, 0, lightCoords);
               }
            }
         }
      });
   }

   private static void vertex(
      final PoseStack.Pose pose,
      final VertexConsumer buffer,
      final float x,
      final float y,
      final float u,
      final float v,
      final float z,
      final int nx,
      final int ny,
      final int nz,
      final int lightCoords
   ) {
      buffer.addVertex(pose, x, y, z).setColor(-1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(pose, nx, ny, nz);
   }
}
