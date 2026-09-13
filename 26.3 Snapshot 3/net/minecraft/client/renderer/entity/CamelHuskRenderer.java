package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.camel.AdultCamelModel;
import net.minecraft.client.model.animal.camel.CamelModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.CamelRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.camel.Camel;

public class CamelHuskRenderer extends MobRenderer<Camel, CamelRenderState, CamelModel> {
   private static final Identifier CAMEL_HUSK_LOCATION = Identifier.withDefaultNamespace("textures/entity/camel/camel_husk.png");

   public CamelHuskRenderer(final EntityRendererProvider.Context context) {
      super(context, new AdultCamelModel(context.bakeLayer(ModelLayers.CAMEL)), 0.7F);
      this.addLayer(CamelRenderer.createCamelSaddleLayer(context, this, EquipmentClientInfo.LayerType.CAMEL_HUSK_SADDLE, ModelLayers.CAMEL_HUSK_SADDLE));
   }

   public Identifier getTextureLocation(final CamelRenderState state) {
      return CAMEL_HUSK_LOCATION;
   }

   public CamelRenderState createRenderState() {
      return new CamelRenderState();
   }

   public void extractRenderState(final Camel entity, final CamelRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      CamelRenderer.extractAdditionalState(entity, state, partialTicks);
   }
}
