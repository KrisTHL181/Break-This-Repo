package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.strider.StriderModel;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.state.StriderRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Strider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class StriderRenderer extends AgeableMobRenderer<Strider, StriderRenderState, StriderModel> {
   private static final Identifier STRIDER_LOCATION = Identifier.withDefaultNamespace("textures/entity/strider/strider.png");
   private static final Identifier COLD_LOCATION = Identifier.withDefaultNamespace("textures/entity/strider/strider_cold.png");
   private static final float SHADOW_RADIUS = 0.5F;

   public StriderRenderer(EntityRendererProvider.Context p_174411_) {
      super(p_174411_, new StriderModel(p_174411_.bakeLayer(ModelLayers.STRIDER)), new StriderModel(p_174411_.bakeLayer(ModelLayers.STRIDER_BABY)), 0.5F);
      this.addLayer(
         new SimpleEquipmentLayer<>(
            this,
            p_174411_.getEquipmentRenderer(),
            EquipmentClientInfo.LayerType.STRIDER_SADDLE,
            p_393866_ -> p_393866_.saddle,
            new StriderModel(p_174411_.bakeLayer(ModelLayers.STRIDER_SADDLE)),
            new StriderModel(p_174411_.bakeLayer(ModelLayers.STRIDER_BABY_SADDLE))
         )
      );
   }

   public Identifier getTextureLocation(StriderRenderState p_361677_) {
      return p_361677_.isSuffocating ? COLD_LOCATION : STRIDER_LOCATION;
   }

   protected float getShadowRadius(StriderRenderState p_364573_) {
      float f = super.getShadowRadius(p_364573_);
      return p_364573_.isBaby ? f * 0.5F : f;
   }

   public StriderRenderState createRenderState() {
      return new StriderRenderState();
   }

   public void extractRenderState(Strider p_361862_, StriderRenderState p_361393_, float p_362076_) {
      super.extractRenderState(p_361862_, p_361393_, p_362076_);
      p_361393_.saddle = p_361862_.getItemBySlot(EquipmentSlot.SADDLE).copy();
      p_361393_.isSuffocating = p_361862_.isSuffocating();
      p_361393_.isRidden = p_361862_.isVehicle();
   }

   protected boolean isShaking(StriderRenderState p_370098_) {
      return super.isShaking(p_370098_) || p_370098_.isSuffocating;
   }
}
