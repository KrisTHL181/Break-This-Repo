package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.math.MatrixUtil;
import java.util.List;
import net.minecraft.client.renderer.feature.submit.TranslucentSubmit;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;

public class ItemFeatureRenderer extends RenderTypeFeatureRenderer<ItemFeatureRenderer.Submit> {
   public static final FeatureRendererType<ItemFeatureRenderer.Submit> TYPE = FeatureRendererType.create("Item");
   public static final Identifier ENCHANTED_GLINT_ARMOR = Identifier.withDefaultNamespace("textures/misc/enchanted_glint_armor.png");
   public static final Identifier ENCHANTED_GLINT_ITEM = Identifier.withDefaultNamespace("textures/misc/enchanted_glint_item.png");
   private static final float SPECIAL_FOIL_UI_SCALE = 0.5F;
   private static final float SPECIAL_FOIL_FIRST_PERSON_SCALE = 0.75F;
   public static final float SPECIAL_FOIL_TEXTURE_SCALE = 0.0078125F;
   public static final int NO_TINT = -1;
   private final QuadInstance quadInstance = new QuadInstance();

   @Override
   protected void buildGroup(final FeatureFrameContext context, final List<ItemFeatureRenderer.Submit> submits) {
      for (ItemFeatureRenderer.Submit submit : submits) {
         this.prepareSubmit(submit);
      }
   }

   private void prepareSubmit(final ItemFeatureRenderer.Submit submit) {
      if (submit.outlineColor() != 0) {
         this.prepareOutlineSubmit(submit);
      } else {
         this.prepareMainSubmit(submit);
      }
   }

   private void prepareMainSubmit(final ItemFeatureRenderer.Submit submit) {
      this.quadInstance.setLightCoords(submit.lightCoords());
      this.quadInstance.setOverlayCoords(submit.overlayCoords());
      ItemStackRenderState.FoilType foilType = submit.foilType();
      PoseStack.Pose foilDecalPose = foilType == ItemStackRenderState.FoilType.SPECIAL ? computeFoilDecalPose(submit.displayContext(), submit.pose()) : null;

      for (BakedQuad quad : submit.quads()) {
         BakedQuad.MaterialInfo material = quad.materialInfo();

         RenderType renderType = switch (submit.foilType()) {
            case NONE -> material.itemRenderType();
            case STANDARD -> material.itemGlintRenderType();
            case SPECIAL -> material.itemGlintSpecialRenderType();
         };
         this.quadInstance.setColor(getLayerColorSafe(submit.tintLayers(), material));
         if (foilType == ItemStackRenderState.FoilType.SPECIAL) {
            this.getVertexBuilder(renderType).putBakedQuadWithGlint(submit.pose(), quad, this.quadInstance, foilDecalPose);
         } else {
            this.getVertexBuilder(renderType).putBakedQuad(submit.pose(), quad, this.quadInstance);
         }
      }
   }

   private void prepareOutlineSubmit(final ItemFeatureRenderer.Submit submit) {
      for (BakedQuad quad : submit.quads()) {
         BakedQuad.MaterialInfo material = quad.materialInfo();
         RenderType renderType = material.itemRenderType().outline().orElse(null);
         if (renderType != null) {
            this.quadInstance.setColor(submit.outlineColor());
            this.getVertexBuilder(renderType).putBakedQuad(submit.pose(), quad, this.quadInstance);
         }
      }
   }

   private static PoseStack.Pose computeFoilDecalPose(final ItemDisplayContext type, final PoseStack.Pose pose) {
      PoseStack.Pose foilDecalPose = pose.copy();
      if (type == ItemDisplayContext.GUI) {
         MatrixUtil.mulComponentWise(foilDecalPose.pose(), 0.5F);
      } else if (type.firstPerson()) {
         MatrixUtil.mulComponentWise(foilDecalPose.pose(), 0.75F);
      }

      return foilDecalPose;
   }

   private static int getLayerColorSafe(final int[] layers, final int layer) {
      return layer >= 0 && layer < layers.length ? layers[layer] : -1;
   }

   private static int getLayerColorSafe(final int[] tintLayers, final BakedQuad.MaterialInfo material) {
      return material.isTinted() ? getLayerColorSafe(tintLayers, material.tintIndex()) : -1;
   }

   public record Submit(
      PoseStack.Pose pose,
      ItemDisplayContext displayContext,
      int lightCoords,
      int overlayCoords,
      int outlineColor,
      int[] tintLayers,
      List<BakedQuad> quads,
      ItemStackRenderState.FoilType foilType
   ) implements TranslucentSubmit {
      public boolean hasTranslucency() {
         for (BakedQuad quad : this.quads()) {
            if (quad.materialInfo().itemRenderType().hasBlending()) {
               return true;
            }
         }

         return false;
      }

      @Override
      public float distanceToCameraSq() {
         return TranslucentSubmit.computeDistanceToCameraSq(this.pose.pose());
      }

      @Override
      public FeatureRendererType<ItemFeatureRenderer.Submit> featureType() {
         return ItemFeatureRenderer.TYPE;
      }
   }
}
