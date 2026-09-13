package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.warden.WardenModel;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.entity.state.WardenRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.warden.Warden;

public class WardenRenderer extends MobRenderer<Warden, WardenRenderState, WardenModel> {
   private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/warden/warden.png");
   private static final Identifier BIOLUMINESCENT_LAYER_TEXTURE = Identifier.withDefaultNamespace("textures/entity/warden/warden_bioluminescent_layer.png");
   private static final Identifier HEART_TEXTURE = Identifier.withDefaultNamespace("textures/entity/warden/warden_heart.png");
   private static final Identifier PULSATING_SPOTS_TEXTURE_1 = Identifier.withDefaultNamespace("textures/entity/warden/warden_pulsating_spots_1.png");
   private static final Identifier PULSATING_SPOTS_TEXTURE_2 = Identifier.withDefaultNamespace("textures/entity/warden/warden_pulsating_spots_2.png");

   public WardenRenderer(final EntityRendererProvider.Context context) {
      super(context, new WardenModel(context.bakeLayer(ModelLayers.WARDEN)), 0.9F);
      WardenModel bioluminescentModel = new WardenModel(context.bakeLayer(ModelLayers.WARDEN_BIOLUMINESCENT));
      WardenModel pulsatingSpotsModel = new WardenModel(context.bakeLayer(ModelLayers.WARDEN_PULSATING_SPOTS));
      WardenModel tendrilsModel = new WardenModel(context.bakeLayer(ModelLayers.WARDEN_TENDRILS));
      WardenModel heartModel = new WardenModel(context.bakeLayer(ModelLayers.WARDEN_HEART));
      this.addLayer(
         new LivingEntityEmissiveLayer<>(
            this, renderState -> BIOLUMINESCENT_LAYER_TEXTURE, (warden, ageInTicks) -> 1.0F, bioluminescentModel, RenderTypes::entityTranslucentEmissive, false
         )
      );
      this.addLayer(
         new LivingEntityEmissiveLayer<>(
            this,
            renderState -> PULSATING_SPOTS_TEXTURE_1,
            (warden, ageInTicks) -> Math.max(0.0F, Mth.cos(ageInTicks * 0.045F) * 0.25F),
            pulsatingSpotsModel,
            RenderTypes::entityTranslucentEmissive,
            false
         )
      );
      this.addLayer(
         new LivingEntityEmissiveLayer<>(
            this,
            renderState -> PULSATING_SPOTS_TEXTURE_2,
            (warden, ageInTicks) -> Math.max(0.0F, Mth.cos(ageInTicks * 0.045F + (float) Math.PI) * 0.25F),
            pulsatingSpotsModel,
            RenderTypes::entityTranslucentEmissive,
            false
         )
      );
      this.addLayer(
         new LivingEntityEmissiveLayer<>(
            this, renderState -> TEXTURE, (warden, ageInTicks) -> warden.tendrilAnimation, tendrilsModel, RenderTypes::entityTranslucentEmissive, false
         )
      );
      this.addLayer(
         new LivingEntityEmissiveLayer<>(
            this, renderState -> HEART_TEXTURE, (warden, ageInTicks) -> warden.heartAnimation, heartModel, RenderTypes::entityTranslucentEmissive, false
         )
      );
   }

   public Identifier getTextureLocation(final WardenRenderState state) {
      return TEXTURE;
   }

   public WardenRenderState createRenderState() {
      return new WardenRenderState();
   }

   public void extractRenderState(final Warden entity, final WardenRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.tendrilAnimation = entity.getTendrilAnimation(partialTicks);
      state.heartAnimation = entity.getHeartAnimation(partialTicks);
      state.roarAnimationState.copyFrom(entity.roarAnimationState);
      state.sniffAnimationState.copyFrom(entity.sniffAnimationState);
      state.emergeAnimationState.copyFrom(entity.emergeAnimationState);
      state.diggingAnimationState.copyFrom(entity.diggingAnimationState);
      state.attackAnimationState.copyFrom(entity.attackAnimationState);
      state.sonicBoomAnimationState.copyFrom(entity.sonicBoomAnimationState);
   }
}
