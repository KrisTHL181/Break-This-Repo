package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.animal.fish.TropicalFishLargeModel;
import net.minecraft.client.model.animal.fish.TropicalFishSmallModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.TropicalFishPatternLayer;
import net.minecraft.client.renderer.entity.state.TropicalFishRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.fish.TropicalFish;

public class TropicalFishRenderer extends MobRenderer<TropicalFish, TropicalFishRenderState, EntityModel<TropicalFishRenderState>> {
   private final EntityModel<TropicalFishRenderState> smallModel = this.getModel();
   private final EntityModel<TropicalFishRenderState> largeModel;
   private static final Identifier SMALL_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fish/tropical_a.png");
   private static final Identifier LARGE_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fish/tropical_b.png");

   public TropicalFishRenderer(final EntityRendererProvider.Context context) {
      super(context, new TropicalFishSmallModel(context.bakeLayer(ModelLayers.TROPICAL_FISH_SMALL)), 0.15F);
      this.largeModel = new TropicalFishLargeModel(context.bakeLayer(ModelLayers.TROPICAL_FISH_LARGE));
      this.addLayer(new TropicalFishPatternLayer(this, context.getModelSet()));
   }

   public Identifier getTextureLocation(final TropicalFishRenderState state) {
      return switch (state.pattern.base()) {
         case SMALL -> SMALL_TEXTURE;
         case LARGE -> LARGE_TEXTURE;
      };
   }

   public TropicalFishRenderState createRenderState() {
      return new TropicalFishRenderState();
   }

   public void extractRenderState(final TropicalFish entity, final TropicalFishRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.pattern = entity.getPattern();
      state.baseColor = entity.getBaseColor().getTextureDiffuseColor();
      state.patternColor = entity.getPatternColor().getTextureDiffuseColor();
   }

   public void submit(
      final TropicalFishRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera
   ) {
      this.model = switch (state.pattern.base()) {
         case SMALL -> this.smallModel;
         case LARGE -> this.largeModel;
      };
      super.submit(state, poseStack, submitNodeCollector, camera);
   }

   protected int getModelTint(final TropicalFishRenderState state) {
      return state.baseColor;
   }

   protected void setupRotations(final TropicalFishRenderState state, final PoseStack poseStack, final float bodyRot, final float entityScale) {
      super.setupRotations(state, poseStack, bodyRot, entityScale);
      float bodyZRot = 4.3F * Mth.sin(0.6F * state.ageInTicks);
      poseStack.mulPose(Axis.YP.rotationDegrees(bodyZRot));
      if (!state.isInWater) {
         poseStack.translate(0.2F, 0.1F, 0.0F);
         poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
      }
   }
}
