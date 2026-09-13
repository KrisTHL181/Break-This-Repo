package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HuskRenderer extends ZombieRenderer {
   private static final Identifier HUSK_LOCATION = Identifier.withDefaultNamespace("textures/entity/zombie/husk.png");

   public HuskRenderer(EntityRendererProvider.Context p_174180_) {
      super(p_174180_, ModelLayers.HUSK, ModelLayers.HUSK_BABY, ModelLayers.HUSK_ARMOR, ModelLayers.HUSK_BABY_ARMOR);
   }

   @Override
   public Identifier getTextureLocation(ZombieRenderState p_454232_) {
      return HUSK_LOCATION;
   }
}
