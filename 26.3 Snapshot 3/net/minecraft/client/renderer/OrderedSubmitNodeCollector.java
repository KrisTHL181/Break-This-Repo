package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

public interface OrderedSubmitNodeCollector {
   void submitShadow(PoseStack poseStack, float radius, List<EntityRenderState.ShadowPiece> pieces);

   void submitNameTag(
      PoseStack poseStack,
      @Nullable Vec3 nameTagAttachment,
      final int offset,
      Component name,
      boolean seeThrough,
      int lightCoords,
      final CameraRenderState camera
   );

   void submitText(
      PoseStack poseStack,
      float x,
      float y,
      FormattedCharSequence string,
      boolean dropShadow,
      Font.DisplayMode displayMode,
      int lightCoords,
      int color,
      int backgroundColor,
      int outlineColor
   );

   void submitFlame(PoseStack poseStack, EntityRenderState renderState, Quaternionf rotation);

   void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leashState);

   <S> void submitModel(
      Model<? super S> model,
      S state,
      PoseStack poseStack,
      RenderType renderType,
      int lightCoords,
      int overlayCoords,
      int tintedColor,
      @Nullable UvMapping uvMapping,
      int outlineColor
   );

   default <S> void submitModel(
      final Model<? super S> model,
      final S state,
      final PoseStack poseStack,
      final RenderType renderType,
      final int lightCoords,
      final int overlayCoords,
      final int outlineColor
   ) {
      this.submitModel(model, state, poseStack, renderType, lightCoords, overlayCoords, -1, null, outlineColor);
   }

   default <S> void submitModel(
      final Model<? super S> model,
      final S state,
      final PoseStack poseStack,
      final Identifier texture,
      final int lightCoords,
      final int overlayCoords,
      final int outlineColor
   ) {
      this.submitModel(model, state, poseStack, model.renderType(texture), lightCoords, overlayCoords, -1, null, outlineColor);
   }

   default <S> void submitModel(
      final Model<S> model,
      final S state,
      final PoseStack poseStack,
      final int lightCoords,
      final int overlayCoords,
      final int tintedColor,
      final SpriteId sprite,
      final SpriteGetter sprites,
      final int outlineColor
   ) {
      this.submitModel(
         model, state, poseStack, sprite.renderType(model.renderType()), lightCoords, overlayCoords, tintedColor, sprites.get(sprite), outlineColor
      );
   }

   <S> void submitCrumblingOverlay(
      Model<? super S> model,
      S state,
      PoseStack poseStack,
      RenderType renderType,
      int lightCoords,
      int overlayCoords,
      int tintedColor,
      ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
   );

   default void submitCrumblingOverlay(
      final ModelPart modelPart,
      final PoseStack poseStack,
      final RenderType renderType,
      final int lightCoords,
      final int overlayCoords,
      final int tintedColor,
      final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
   ) {
      this.submitCrumblingOverlay(
         new Model.Simple(modelPart, var1 -> renderType), Unit.INSTANCE, poseStack, renderType, lightCoords, overlayCoords, tintedColor, crumblingOverlay
      );
   }

   default void submitModelPart(
      final ModelPart modelPart,
      final PoseStack poseStack,
      final RenderType renderType,
      final int lightCoords,
      final int overlayCoords,
      final @Nullable UvMapping uvMapping
   ) {
      this.submitModelPart(modelPart, poseStack, renderType, lightCoords, overlayCoords, uvMapping, -1, 0);
   }

   default void submitModelPart(
      final ModelPart modelPart,
      final PoseStack poseStack,
      final RenderType renderType,
      final int lightCoords,
      final int overlayCoords,
      final @Nullable UvMapping uvMapping,
      final int tintedColor
   ) {
      this.submitModelPart(modelPart, poseStack, renderType, lightCoords, overlayCoords, uvMapping, tintedColor, 0);
   }

   default void submitModelPart(
      final ModelPart modelPart,
      final PoseStack poseStack,
      final RenderType renderType,
      final int lightCoords,
      final int overlayCoords,
      final @Nullable UvMapping uvMapping,
      final int tintedColor,
      final int outlineColor
   ) {
      Model.Simple model = new Model.Simple(modelPart, var1 -> renderType);
      this.submitModel(model, Unit.INSTANCE, poseStack, renderType, lightCoords, overlayCoords, tintedColor, uvMapping, outlineColor);
   }

   void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState movingBlockRenderState, int outlineColor);

   void submitBlockModel(
      PoseStack poseStack, RenderType renderType, List<BlockStateModelPart> parts, int[] tintLayers, int lightCoords, int overlayCoords, int outlineColor
   );

   void submitBreakingBlockModel(PoseStack poseStack, List<BlockStateModelPart> parts, int progress, final boolean isBlockTranslucent);

   void submitShapeOutline(PoseStack poseStack, VoxelShape shape, RenderType renderType, int color, float width, boolean afterTerrain);

   void submitItem(
      PoseStack poseStack,
      ItemDisplayContext displayContext,
      int lightCoords,
      int overlayCoords,
      int outlineColor,
      int[] tintLayers,
      List<BakedQuad> quads,
      ItemStackRenderState.FoilType foilType
   );

   void submitCustomGeometry(PoseStack poseStack, RenderType renderType, SubmitNodeCollector.CustomGeometryRenderer customGeometryRenderer);

   void submitQuadParticleGroup(QuadParticleRenderState particles);

   void submitGizmoPrimitives(DrawableGizmoPrimitives.Group group, CameraRenderState camera, boolean onTop);
}
