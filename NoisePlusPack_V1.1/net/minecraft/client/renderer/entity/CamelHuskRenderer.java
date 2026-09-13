package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.camel.CamelModel;
import net.minecraft.client.model.animal.camel.CamelSaddleModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.state.CamelRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CamelHuskRenderer extends CamelRenderer {
   private static final Identifier CAMEL_HUSK_LOCATION = Identifier.withDefaultNamespace("textures/entity/camel/camel_husk.png");

   public CamelHuskRenderer(EntityRendererProvider.Context p_457886_) {
      super(p_457886_);
   }

   @Override
   protected SimpleEquipmentLayer<CamelRenderState, CamelModel, CamelSaddleModel> createCamelSaddleLayer(EntityRendererProvider.Context p_454201_) {
      return new SimpleEquipmentLayer<>(
         this,
         p_454201_.getEquipmentRenderer(),
         EquipmentClientInfo.LayerType.CAMEL_HUSK_SADDLE,
         p_455720_ -> p_455720_.saddle,
         new CamelSaddleModel(p_454201_.bakeLayer(ModelLayers.CAMEL_HUSK_SADDLE)),
         new CamelSaddleModel(p_454201_.bakeLayer(ModelLayers.CAMEL_HUSK_BABY_SADDLE))
      );
   }

   @Override
   public Identifier getTextureLocation(CamelRenderState p_452515_) {
      return CAMEL_HUSK_LOCATION;
   }
}
