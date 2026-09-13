package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.camel.AdultCamelModel;
import net.minecraft.client.model.animal.camel.BabyCamelModel;
import net.minecraft.client.model.animal.camel.CamelModel;
import net.minecraft.client.model.animal.camel.CamelSaddleModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.state.CamelRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.camel.Camel;

public class CamelRenderer extends AgeableMobRenderer<Camel, CamelRenderState, CamelModel> {
   private static final Identifier CAMEL_LOCATION = Identifier.withDefaultNamespace("textures/entity/camel/camel.png");
   private static final Identifier CAMEL_BABY_LOCATION = Identifier.withDefaultNamespace("textures/entity/camel/camel_baby.png");

   public CamelRenderer(final EntityRendererProvider.Context context) {
      super(context, new AdultCamelModel(context.bakeLayer(ModelLayers.CAMEL)), new BabyCamelModel(context.bakeLayer(ModelLayers.CAMEL_BABY)), 0.7F);
      this.addLayer(createCamelSaddleLayer(context, this, EquipmentClientInfo.LayerType.CAMEL_SADDLE, ModelLayers.CAMEL_SADDLE));
   }

   protected static SimpleEquipmentLayer<CamelRenderState, CamelModel, CamelSaddleModel> createCamelSaddleLayer(
      final EntityRendererProvider.Context context,
      final MobRenderer<Camel, CamelRenderState, CamelModel> renderer,
      final EquipmentClientInfo.LayerType saddleLayerType,
      final ModelLayerLocation saddleModelLayer
   ) {
      return new SimpleEquipmentLayer<>(
         renderer, context.getEquipmentRenderer(), saddleLayerType, state -> state.saddle, new CamelSaddleModel(context.bakeLayer(saddleModelLayer)), null
      );
   }

   public Identifier getTextureLocation(final CamelRenderState state) {
      return state.isBaby ? CAMEL_BABY_LOCATION : CAMEL_LOCATION;
   }

   public CamelRenderState createRenderState() {
      return new CamelRenderState();
   }

   public void extractRenderState(final Camel entity, final CamelRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      extractAdditionalState(entity, state, partialTicks);
   }

   public static void extractAdditionalState(final Camel entity, final CamelRenderState state, final float partialTicks) {
      state.saddle = entity.getItemBySlot(EquipmentSlot.SADDLE).copy();
      state.isRidden = entity.isVehicle();
      state.jumpCooldown = getJumpCooldown(entity, partialTicks);
      state.sitAnimationState.copyFrom(entity.sitAnimationState);
      state.sitPoseAnimationState.copyFrom(entity.sitPoseAnimationState);
      state.sitUpAnimationState.copyFrom(entity.sitUpAnimationState);
      state.idleAnimationState.copyFrom(entity.idleAnimationState);
      state.dashAnimationState.copyFrom(entity.dashAnimationState);
   }

   private static float getJumpCooldown(final Camel camel, final float partialTicks) {
      return Math.max(camel.getJumpCooldown() - partialTicks, 0.0F);
   }
}
