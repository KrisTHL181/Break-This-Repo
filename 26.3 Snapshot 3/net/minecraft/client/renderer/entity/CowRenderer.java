package net.minecraft.client.renderer.entity;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import net.minecraft.client.model.AdultAndBabyModelPair;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.CowRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.cow.CowVariant;

public class CowRenderer extends MobRenderer<Cow, CowRenderState, CowModel> {
   private final Map<CowVariant.ModelType, AdultAndBabyModelPair<CowModel>> models;

   public CowRenderer(final EntityRendererProvider.Context context) {
      super(context, new CowModel(context.bakeLayer(ModelLayers.COW)), 0.7F);
      this.models = bakeModels(context);
   }

   private static Map<CowVariant.ModelType, AdultAndBabyModelPair<CowModel>> bakeModels(final EntityRendererProvider.Context context) {
      return Maps.newEnumMap(
         Map.of(
            CowVariant.ModelType.NORMAL,
            new AdultAndBabyModelPair<>(new CowModel(context.bakeLayer(ModelLayers.COW)), new CowModel(context.bakeLayer(ModelLayers.COW_BABY))),
            CowVariant.ModelType.WARM,
            new AdultAndBabyModelPair<>(new CowModel(context.bakeLayer(ModelLayers.WARM_COW)), new CowModel(context.bakeLayer(ModelLayers.WARM_COW_BABY))),
            CowVariant.ModelType.COLD,
            new AdultAndBabyModelPair<>(new CowModel(context.bakeLayer(ModelLayers.COLD_COW)), new CowModel(context.bakeLayer(ModelLayers.COLD_COW_BABY)))
         )
      );
   }

   public Identifier getTextureLocation(final CowRenderState state) {
      return state.variant == null
         ? MissingTextureAtlasSprite.getLocation()
         : (state.isBaby ? state.variant.babyTexture().texturePath() : state.variant.modelAndTexture().asset().texturePath());
   }

   public CowRenderState createRenderState() {
      return new CowRenderState();
   }

   public void extractRenderState(final Cow entity, final CowRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.variant = entity.getVariant().value();
   }

   public void submit(final CowRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera) {
      if (state.variant != null) {
         this.model = this.models.get(state.variant.modelAndTexture().model()).getModel(state.isBaby);
         super.submit(state, poseStack, submitNodeCollector, camera);
      }
   }
}
