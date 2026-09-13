package net.minecraft.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.BlockModelFeatureRenderer;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.renderer.feature.FlameFeatureRenderer;
import net.minecraft.client.renderer.feature.GizmoFeatureRenderer;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.feature.LeashFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.feature.MovingBlockFeatureRenderer;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
import net.minecraft.client.renderer.feature.QuadParticleFeatureRenderer;
import net.minecraft.client.renderer.feature.ShadowFeatureRenderer;
import net.minecraft.client.renderer.feature.ShapeOutlineFeatureRenderer;
import net.minecraft.client.renderer.feature.TextFeatureRenderer;
import net.minecraft.client.renderer.feature.phase.FeatureRenderPhase;
import net.minecraft.client.renderer.feature.phase.SimpleFeatureRenderPhase;
import net.minecraft.client.renderer.feature.phase.TranslucentFeatureRenderPhase;
import net.minecraft.client.renderer.feature.submit.TranslucentSubmit;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

public class SubmitNodeCollection implements OrderedSubmitNodeCollector {
   public final SimpleFeatureRenderPhase solid = new SimpleFeatureRenderPhase();
   public final SimpleFeatureRenderPhase waterMask = new SimpleFeatureRenderPhase();
   public final SimpleFeatureRenderPhase outline = new SimpleFeatureRenderPhase();
   public final SimpleFeatureRenderPhase alwaysOnTopGizmos = new SimpleFeatureRenderPhase();
   public final SimpleFeatureRenderPhase oitTranslucent = new SimpleFeatureRenderPhase();
   public final SimpleFeatureRenderPhase shadows;
   public final SimpleFeatureRenderPhase nameTags;
   public final FeatureRenderPhase<? super TranslucentSubmit> seeThroughNameTags;
   public final SimpleFeatureRenderPhase texts;
   public final SimpleFeatureRenderPhase shapeOutlines;
   public final FeatureRenderPhase<? super TranslucentSubmit> translucentBlocksAndItems;
   public final FeatureRenderPhase<? super TranslucentSubmit> translucentModels;
   public final SimpleFeatureRenderPhase translucentCustomGeometry;
   public final SimpleFeatureRenderPhase breakingOverlay;
   public final SimpleFeatureRenderPhase afterTerrain;
   public final SimpleFeatureRenderPhase translucentGizmos;
   private final List<FeatureRenderPhase<?>> allPhases;

   public SubmitNodeCollection(final boolean useImprovedTransparency) {
      if (useImprovedTransparency) {
         this.shadows = this.oitTranslucent;
         this.nameTags = this.oitTranslucent;
         this.seeThroughNameTags = this.oitTranslucent;
         this.texts = this.oitTranslucent;
         this.shapeOutlines = this.oitTranslucent;
         this.translucentBlocksAndItems = this.oitTranslucent;
         this.translucentModels = this.oitTranslucent;
         this.translucentCustomGeometry = this.oitTranslucent;
         this.breakingOverlay = this.oitTranslucent;
         this.afterTerrain = this.oitTranslucent;
         this.translucentGizmos = this.oitTranslucent;
         this.allPhases = List.of(this.solid, this.waterMask, this.outline, this.alwaysOnTopGizmos, this.oitTranslucent);
      } else {
         this.shadows = new SimpleFeatureRenderPhase();
         this.nameTags = new SimpleFeatureRenderPhase();
         this.seeThroughNameTags = new TranslucentFeatureRenderPhase();
         this.texts = new SimpleFeatureRenderPhase();
         this.shapeOutlines = new SimpleFeatureRenderPhase();
         this.translucentBlocksAndItems = new TranslucentFeatureRenderPhase();
         this.translucentModels = new TranslucentFeatureRenderPhase();
         this.translucentCustomGeometry = new SimpleFeatureRenderPhase();
         this.breakingOverlay = new SimpleFeatureRenderPhase();
         this.afterTerrain = new SimpleFeatureRenderPhase();
         this.translucentGizmos = new SimpleFeatureRenderPhase();
         this.allPhases = List.of(
            this.solid,
            this.shadows,
            this.nameTags,
            this.seeThroughNameTags,
            this.texts,
            this.shapeOutlines,
            this.translucentBlocksAndItems,
            this.translucentModels,
            this.translucentCustomGeometry,
            this.translucentGizmos,
            this.breakingOverlay,
            this.waterMask,
            this.afterTerrain,
            this.alwaysOnTopGizmos,
            this.outline
         );
      }
   }

   @Override
   public void submitShadow(final PoseStack poseStack, final float radius, final List<EntityRenderState.ShadowPiece> pieces) {
      PoseStack.Pose pose = poseStack.last();
      this.shadows.submit(new ShadowFeatureRenderer.Submit(new Matrix4f(pose.pose()), radius, pieces));
   }

   @Override
   public void submitNameTag(
      final PoseStack poseStack,
      final @Nullable Vec3 nameTagAttachment,
      final int offset,
      final Component name,
      final boolean seeThrough,
      final int lightCoords,
      final CameraRenderState camera
   ) {
      if (nameTagAttachment != null) {
         Minecraft minecraft = Minecraft.getInstance();
         poseStack.pushPose();
         poseStack.translate(nameTagAttachment.x, nameTagAttachment.y + 0.5, nameTagAttachment.z);
         poseStack.mulPose(camera.orientation);
         poseStack.scale(0.025F, -0.025F, 0.025F);
         Matrix4f pose = new Matrix4f(poseStack.last().pose());
         float x = -minecraft.font.width(name) / 2.0F;
         int backgroundColor = ARGB.color(minecraft.gameRenderer.gameRenderState().optionsRenderState.getBackgroundOpacity(0.25F), -16777216);
         if (seeThrough) {
            this.submitNameTagPart(
               new NameTagFeatureRenderer.Submit(pose, x, offset, name, LightCoordsUtil.lightCoordsWithEmission(lightCoords, 2), -1, 0, Font.DisplayMode.NORMAL)
            );
            this.seeThroughNameTags
               .submit(new NameTagFeatureRenderer.Submit(pose, x, offset, name, lightCoords, -2130706433, backgroundColor, Font.DisplayMode.SEE_THROUGH));
         } else {
            this.submitNameTagPart(new NameTagFeatureRenderer.Submit(pose, x, offset, name, lightCoords, -2130706433, backgroundColor, Font.DisplayMode.NORMAL));
         }

         poseStack.popPose();
      }
   }

   private void submitNameTagPart(final NameTagFeatureRenderer.Submit nameTag) {
      if (shouldRenderTextAsSolid(nameTag.color(), nameTag.backgroundColor())) {
         this.solid.submit(nameTag);
      } else {
         this.nameTags.submit(nameTag);
      }
   }

   private static boolean shouldRenderTextAsSolid(final int color, final int backgroundColor) {
      return ARGB.alpha(color) == 255 && ARGB.alpha(backgroundColor) == 0 && RenderSystem.isRenderingLevel;
   }

   @Override
   public void submitText(
      final PoseStack poseStack,
      final float x,
      final float y,
      final FormattedCharSequence string,
      final boolean dropShadow,
      final Font.DisplayMode displayMode,
      final int lightCoords,
      final int color,
      final int backgroundColor,
      final int outlineColor
   ) {
      TextFeatureRenderer.Submit submit = new TextFeatureRenderer.Submit(
         new Matrix4f(poseStack.last().pose()), x, y, string, dropShadow, displayMode, lightCoords, color, backgroundColor, outlineColor
      );
      if (displayMode != Font.DisplayMode.SEE_THROUGH && shouldRenderTextAsSolid(color, backgroundColor)) {
         this.solid.submit(submit);
      } else {
         this.texts.submit(submit);
      }
   }

   @Override
   public void submitFlame(final PoseStack poseStack, final EntityRenderState renderState, final Quaternionf rotation) {
      this.solid.submit(new FlameFeatureRenderer.Submit(poseStack.last().copy(), renderState, rotation));
   }

   @Override
   public void submitLeash(final PoseStack poseStack, final EntityRenderState.LeashState leashState) {
      this.solid.submit(new LeashFeatureRenderer.Submit(new Matrix4f(poseStack.last().pose()), leashState));
   }

   @Override
   public <S> void submitModel(
      final Model<? super S> model,
      final S state,
      final PoseStack poseStack,
      final RenderType renderType,
      final int lightCoords,
      final int overlayCoords,
      final int tintedColor,
      final @Nullable UvMapping uvMapping,
      final int outlineColor
   ) {
      PoseStack.Pose pose = poseStack.last().copy();
      if (!renderType.isOutline()) {
         ModelFeatureRenderer.Submit<S> submit = new ModelFeatureRenderer.Submit<>(
            renderType, pose, model, state, lightCoords, overlayCoords, tintedColor, uvMapping, null
         );
         if (renderType == RenderTypes.waterMask()) {
            this.waterMask.submit(submit);
         } else if (renderType.forceSolidModelPhase()) {
            this.solid.submit(submit);
         } else {
            if (!renderType.hasBlending() || renderType.bothSolidAndTranslucent()) {
               this.solid.submit(submit);
            }

            if (renderType.hasBlending()) {
               this.translucentModels.submit(submit);
            }
         }
      }

      if (outlineColor != 0) {
         RenderType outlineRenderType = getOutlineRenderType(renderType);
         if (outlineRenderType != null) {
            this.outline
               .submit(
                  new ModelFeatureRenderer.Submit<>(outlineRenderType, pose, model, state, 15728880, OverlayTexture.NO_OVERLAY, outlineColor, uvMapping, null)
               );
         }
      }
   }

   @Override
   public <S> void submitCrumblingOverlay(
      final Model<? super S> model,
      final S state,
      final PoseStack poseStack,
      final RenderType renderType,
      final int lightCoords,
      final int overlayCoords,
      final int tintedColor,
      final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
   ) {
      if (renderType.affectsCrumbling()) {
         boolean translucent = renderType.hasBlending();
         List<RenderType> destroyTypes;
         if (Minecraft.getInstance().gameRenderer.useImprovedTransparency() && translucent) {
            destroyTypes = ModelBakery.DESTROY_TYPES_OIT;
         } else {
            destroyTypes = ModelBakery.DESTROY_TYPES;
         }

         RenderType crumblingRenderType = destroyTypes.get(crumblingOverlay.progress());
         SimpleFeatureRenderPhase targetPhase = translucent ? this.breakingOverlay : this.solid;
         targetPhase.submit(
            new ModelFeatureRenderer.Submit<>(
               crumblingRenderType, poseStack.last().copy(), model, state, lightCoords, overlayCoords, tintedColor, null, crumblingOverlay.cameraPose()
            )
         );
      }
   }

   @Override
   public void submitMovingBlock(final PoseStack poseStack, final MovingBlockRenderState movingBlockRenderState, final int outlineColor) {
      MovingBlockFeatureRenderer.Submit submit = new MovingBlockFeatureRenderer.Submit(new Matrix4f(poseStack.last().pose()), movingBlockRenderState, 0);
      BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(movingBlockRenderState.blockState);
      if (model.hasMaterialFlag(1)) {
         this.translucentBlocksAndItems.submit(submit);
      } else {
         this.solid.submit(submit);
      }

      if (outlineColor != 0) {
         this.outline.submit(new MovingBlockFeatureRenderer.Submit(new Matrix4f(poseStack.last().pose()), movingBlockRenderState, outlineColor));
      }
   }

   @Override
   public void submitBlockModel(
      final PoseStack poseStack,
      final RenderType renderType,
      final List<BlockStateModelPart> modelParts,
      final int[] tintLayers,
      final int lightCoords,
      final int overlayCoords,
      final int outlineColor
   ) {
      PoseStack.Pose pose = poseStack.last().copy();
      if (!renderType.isOutline()) {
         BlockModelFeatureRenderer.Submit submit = new BlockModelFeatureRenderer.Submit(
            pose, renderType, modelParts, tintLayers, lightCoords, overlayCoords, -1, null
         );
         if (renderType.hasBlending()) {
            this.translucentBlocksAndItems.submit(submit);
         } else {
            this.solid.submit(submit);
         }
      }

      if (outlineColor != 0) {
         RenderType outlineRenderType = getOutlineRenderType(renderType);
         if (outlineRenderType != null) {
            this.outline
               .submit(
                  new BlockModelFeatureRenderer.Submit(
                     pose, outlineRenderType, modelParts, BlockModelRenderState.EMPTY_TINTS, 15728880, OverlayTexture.NO_OVERLAY, outlineColor, null
                  )
               );
         }
      }
   }

   private static @Nullable RenderType getOutlineRenderType(final RenderType renderType) {
      if (renderType.isOutline()) {
         return renderType;
      } else {
         return renderType.outline().isPresent() ? renderType.outline().get() : null;
      }
   }

   @Override
   public void submitBreakingBlockModel(final PoseStack poseStack, final List<BlockStateModelPart> parts, final int progress, final boolean isBlockTranslucent) {
      PoseStack.Pose pose = poseStack.last().copy();
      List<RenderType> destroyTypes = Minecraft.getInstance().gameRenderer.useImprovedTransparency() && isBlockTranslucent
         ? ModelBakery.DESTROY_TYPES_OIT
         : ModelBakery.DESTROY_TYPES;
      SimpleFeatureRenderPhase targetPhase = isBlockTranslucent ? this.breakingOverlay : this.solid;
      targetPhase.submit(
         new BlockModelFeatureRenderer.Submit(
            pose, destroyTypes.get(progress), List.copyOf(parts), BlockModelRenderState.EMPTY_TINTS, 15728880, OverlayTexture.NO_OVERLAY, 0, pose
         )
      );
   }

   @Override
   public void submitShapeOutline(
      final PoseStack poseStack, final VoxelShape shape, final RenderType renderType, final int color, final float width, final boolean afterTerrain
   ) {
      ShapeOutlineFeatureRenderer.Submit submit = new ShapeOutlineFeatureRenderer.Submit(poseStack.last().copy(), shape, renderType, color, width);
      if (ARGB.alpha(color) == 255) {
         this.solid.submit(submit);
      } else if (afterTerrain) {
         this.afterTerrain.submit(submit);
      } else {
         this.shapeOutlines.submit(submit);
      }
   }

   @Override
   public void submitItem(
      final PoseStack poseStack,
      final ItemDisplayContext displayContext,
      final int lightCoords,
      final int overlayCoords,
      final int outlineColor,
      final int[] tintLayers,
      final List<BakedQuad> quads,
      final ItemStackRenderState.FoilType foilType
   ) {
      PoseStack.Pose pose = poseStack.last().copy();
      ItemFeatureRenderer.Submit submit = new ItemFeatureRenderer.Submit(pose, displayContext, lightCoords, overlayCoords, 0, tintLayers, quads, foilType);
      if (submit.hasTranslucency()) {
         this.translucentBlocksAndItems.submit(submit);
      }

      if (shouldRenderAsSolid(submit)) {
         this.solid.submit(submit);
      }

      if (outlineColor != 0) {
         this.outline
            .submit(
               new ItemFeatureRenderer.Submit(
                  pose,
                  displayContext,
                  15728880,
                  OverlayTexture.NO_OVERLAY,
                  outlineColor,
                  ItemStackRenderState.LayerRenderState.EMPTY_TINTS,
                  quads,
                  ItemStackRenderState.FoilType.NONE
               )
            );
      }
   }

   private static boolean shouldRenderAsSolid(final ItemFeatureRenderer.Submit submit) {
      boolean hasBlending = false;

      for (BakedQuad quad : submit.quads()) {
         RenderType renderType = switch (submit.foilType()) {
            case NONE -> quad.materialInfo().itemRenderType();
            case STANDARD -> quad.materialInfo().itemGlintRenderType();
            case SPECIAL -> quad.materialInfo().itemGlintSpecialRenderType();
         };
         if (renderType.hasBlending()) {
            hasBlending = true;
         }

         if (renderType.bothSolidAndTranslucent()) {
            return true;
         }
      }

      return !hasBlending;
   }

   @Override
   public void submitCustomGeometry(
      final PoseStack poseStack, final RenderType renderType, final SubmitNodeCollector.CustomGeometryRenderer customGeometryRenderer
   ) {
      CustomFeatureRenderer.Submit submit = new CustomFeatureRenderer.Submit(poseStack.last().copy(), renderType, customGeometryRenderer);
      if (renderType.isOutline()) {
         this.outline.submit(submit);
      } else if (renderType.hasBlending()) {
         this.translucentCustomGeometry.submit(submit);
      } else {
         this.solid.submit(submit);
      }
   }

   @Override
   public void submitQuadParticleGroup(final QuadParticleRenderState particles) {
      this.solid.submit(new QuadParticleFeatureRenderer.Submit(particles, false));
      this.afterTerrain.submit(new QuadParticleFeatureRenderer.Submit(particles, true));
   }

   @Override
   public void submitGizmoPrimitives(final DrawableGizmoPrimitives.Group group, final CameraRenderState camera, final boolean onTop) {
      GizmoFeatureRenderer.Submit submit = new GizmoFeatureRenderer.Submit(group, camera);
      if (onTop) {
         this.alwaysOnTopGizmos.submit(submit);
      } else if (group.opaque()) {
         this.solid.submit(submit);
      } else {
         this.translucentGizmos.submit(submit);
      }
   }

   public List<FeatureRenderPhase<?>> allPhases() {
      return this.allPhases;
   }
}
