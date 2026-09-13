package net.minecraft.client.renderer.entity;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import net.minecraft.client.model.AdultAndBabyModelPair;
import net.minecraft.client.model.animal.pig.BabyPigModel;
import net.minecraft.client.model.animal.pig.ColdPigModel;
import net.minecraft.client.model.animal.pig.PigModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.state.PigRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.pig.PigVariant;

public class PigRenderer extends MobRenderer<Pig, PigRenderState, PigModel> {
   private final Map<PigVariant.ModelType, AdultAndBabyModelPair<PigModel>> models;

   public PigRenderer(final EntityRendererProvider.Context context) {
      super(context, new PigModel(context.bakeLayer(ModelLayers.PIG)), 0.7F);
      this.models = bakeModels(context);
      this.addLayer(
         new SimpleEquipmentLayer<>(
            this,
            context.getEquipmentRenderer(),
            EquipmentClientInfo.LayerType.PIG_SADDLE,
            state -> state.saddle,
            new PigModel(context.bakeLayer(ModelLayers.PIG_SADDLE)),
            null
         )
      );
   }

   private static Map<PigVariant.ModelType, AdultAndBabyModelPair<PigModel>> bakeModels(final EntityRendererProvider.Context context) {
      return Maps.newEnumMap(
         Map.of(
            PigVariant.ModelType.NORMAL,
            new AdultAndBabyModelPair<>(new PigModel(context.bakeLayer(ModelLayers.PIG)), new BabyPigModel(context.bakeLayer(ModelLayers.PIG_BABY))),
            PigVariant.ModelType.COLD,
            new AdultAndBabyModelPair<>(new ColdPigModel(context.bakeLayer(ModelLayers.COLD_PIG)), new BabyPigModel(context.bakeLayer(ModelLayers.PIG_BABY)))
         )
      );
   }

   public void submit(final PigRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera) {
      if (state.variant != null) {
         this.model = this.models.get(state.variant.modelAndTexture().model()).getModel(state.isBaby);
         super.submit(state, poseStack, submitNodeCollector, camera);
      }
   }

   public Identifier getTextureLocation(final PigRenderState state) {
      return state.variant == null
         ? MissingTextureAtlasSprite.getLocation()
         : (state.isBaby ? state.variant.babyTexture().texturePath() : state.variant.modelAndTexture().asset().texturePath());
   }

   public PigRenderState createRenderState() {
      return new PigRenderState();
   }

   public void extractRenderState(final Pig entity, final PigRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.saddle = entity.getItemBySlot(EquipmentSlot.SADDLE).copy();
      state.variant = entity.getVariant().value();
   }
}
