package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.feature.phase.FeatureRenderPhase;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

public class SubmitNodeStorage implements SubmitNodeCollector {
   private final Int2ObjectAVLTreeMap<SubmitNodeCollection> submitsPerOrder = new Int2ObjectAVLTreeMap();
   private boolean useImprovedTransparency;

   public void setUseImprovedTransparency(final boolean useImprovedTransparency) {
      if (this.useImprovedTransparency != useImprovedTransparency) {
         ObjectIterator var2 = this.submitsPerOrder.values().iterator();

         while (var2.hasNext()) {
            SubmitNodeCollection collection = (SubmitNodeCollection)var2.next();

            for (FeatureRenderPhase<?> phase : collection.allPhases()) {
               if (!phase.isEmpty()) {
                  throw new IllegalStateException("Storage is not empty. Improved transparency is likely toggled in a wrong place.");
               }
            }
         }

         this.submitsPerOrder.clear();
         this.useImprovedTransparency = useImprovedTransparency;
      }
   }

   public SubmitNodeCollection order(final int order) {
      return (SubmitNodeCollection)this.submitsPerOrder.computeIfAbsent(order, var1 -> new SubmitNodeCollection(this.useImprovedTransparency));
   }

   @Override
   public void submitShadow(final PoseStack poseStack, final float radius, final List<EntityRenderState.ShadowPiece> pieces) {
      this.order(0).submitShadow(poseStack, radius, pieces);
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
      this.order(0).submitNameTag(poseStack, nameTagAttachment, offset, name, seeThrough, lightCoords, camera);
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
      this.order(0).submitText(poseStack, x, y, string, dropShadow, displayMode, lightCoords, color, backgroundColor, outlineColor);
   }

   @Override
   public void submitFlame(final PoseStack poseStack, final EntityRenderState renderState, final Quaternionf rotation) {
      this.order(0).submitFlame(poseStack, renderState, rotation);
   }

   @Override
   public void submitLeash(final PoseStack poseStack, final EntityRenderState.LeashState leashState) {
      this.order(0).submitLeash(poseStack, leashState);
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
      this.order(0).submitModel(model, state, poseStack, renderType, lightCoords, overlayCoords, tintedColor, uvMapping, outlineColor);
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
      this.order(0).submitCrumblingOverlay(model, state, poseStack, renderType, lightCoords, overlayCoords, tintedColor, crumblingOverlay);
   }

   @Override
   public void submitMovingBlock(final PoseStack poseStack, final MovingBlockRenderState movingBlockRenderState, final int outlineColor) {
      this.order(0).submitMovingBlock(poseStack, movingBlockRenderState, outlineColor);
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
      this.order(0).submitBlockModel(poseStack, renderType, modelParts, tintLayers, lightCoords, overlayCoords, outlineColor);
   }

   @Override
   public void submitBreakingBlockModel(final PoseStack poseStack, final List<BlockStateModelPart> parts, final int progress, final boolean isBlockTranslucent) {
      this.order(0).submitBreakingBlockModel(poseStack, parts, progress, isBlockTranslucent);
   }

   @Override
   public void submitShapeOutline(
      final PoseStack poseStack, final VoxelShape shape, final RenderType renderType, final int color, final float width, final boolean afterTerrain
   ) {
      this.order(0).submitShapeOutline(poseStack, shape, renderType, color, width, afterTerrain);
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
      this.order(0).submitItem(poseStack, displayContext, lightCoords, overlayCoords, outlineColor, tintLayers, quads, foilType);
   }

   @Override
   public void submitCustomGeometry(
      final PoseStack poseStack, final RenderType renderType, final SubmitNodeCollector.CustomGeometryRenderer customGeometryRenderer
   ) {
      this.order(0).submitCustomGeometry(poseStack, renderType, customGeometryRenderer);
   }

   @Override
   public void submitQuadParticleGroup(final QuadParticleRenderState particles) {
      this.order(0).submitQuadParticleGroup(particles);
   }

   @Override
   public void submitGizmoPrimitives(final DrawableGizmoPrimitives.Group group, final CameraRenderState camera, final boolean onTop) {
      this.order(0).submitGizmoPrimitives(group, camera, onTop);
   }

   public Int2ObjectAVLTreeMap<SubmitNodeCollection> getSubmitsPerOrder() {
      return this.submitsPerOrder;
   }

   public void drainPhases(final Consumer<FeatureRenderPhase<?>> consumer) {
      this.submitsPerOrder.values().removeIf(collection -> {
         boolean empty = true;

         for (FeatureRenderPhase<?> phase : collection.allPhases()) {
            if (!phase.isEmpty()) {
               consumer.accept(phase);
               empty = false;
            }
         }

         return empty;
      });
   }
}
