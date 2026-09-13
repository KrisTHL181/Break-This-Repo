package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.camel.CamelModel;
import net.minecraft.client.model.animal.camel.CamelSaddleModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.state.CamelRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CamelRenderer extends AgeableMobRenderer<Camel, CamelRenderState, CamelModel> {
   private static final Identifier CAMEL_LOCATION = Identifier.withDefaultNamespace("textures/entity/camel/camel.png");

   public CamelRenderer(EntityRendererProvider.Context p_251790_) {
      super(p_251790_, new CamelModel(p_251790_.bakeLayer(ModelLayers.CAMEL)), new CamelModel(p_251790_.bakeLayer(ModelLayers.CAMEL_BABY)), 0.7F);
      this.addLayer(this.createCamelSaddleLayer(p_251790_));
   }

   protected SimpleEquipmentLayer<CamelRenderState, CamelModel, CamelSaddleModel> createCamelSaddleLayer(EntityRendererProvider.Context p_454241_) {
      return new SimpleEquipmentLayer<>(
         this,
         p_454241_.getEquipmentRenderer(),
         EquipmentClientInfo.LayerType.CAMEL_SADDLE,
         p_395190_ -> p_395190_.saddle,
         new CamelSaddleModel(p_454241_.bakeLayer(ModelLayers.CAMEL_SADDLE)),
         new CamelSaddleModel(p_454241_.bakeLayer(ModelLayers.CAMEL_BABY_SADDLE))
      );
   }

   public Identifier getTextureLocation(CamelRenderState p_368992_) {
      return CAMEL_LOCATION;
   }

   public CamelRenderState createRenderState() {
      return new CamelRenderState();
   }

   public void extractRenderState(Camel p_361457_, CamelRenderState p_363176_, float p_363399_) {
      super.extractRenderState(p_361457_, p_363176_, p_363399_);
      p_363176_.saddle = p_361457_.getItemBySlot(EquipmentSlot.SADDLE).copy();
      p_363176_.isRidden = p_361457_.isVehicle();
      p_363176_.jumpCooldown = Math.max(p_361457_.getJumpCooldown() - p_363399_, 0.0F);
      p_363176_.sitAnimationState.copyFrom(p_361457_.sitAnimationState);
      p_363176_.sitPoseAnimationState.copyFrom(p_361457_.sitPoseAnimationState);
      p_363176_.sitUpAnimationState.copyFrom(p_361457_.sitUpAnimationState);
      p_363176_.idleAnimationState.copyFrom(p_361457_.idleAnimationState);
      p_363176_.dashAnimationState.copyFrom(p_361457_.dashAnimationState);
   }
}
