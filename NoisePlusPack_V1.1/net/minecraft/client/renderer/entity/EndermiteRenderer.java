package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.endermite.EndermiteModel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EndermiteRenderer extends MobRenderer<Endermite, LivingEntityRenderState, EndermiteModel> {
   private static final Identifier ENDERMITE_LOCATION = Identifier.withDefaultNamespace("textures/entity/endermite.png");

   public EndermiteRenderer(EntityRendererProvider.Context p_173994_) {
      super(p_173994_, new EndermiteModel(p_173994_.bakeLayer(ModelLayers.ENDERMITE)), 0.3F);
   }

   @Override
   protected float getFlipDegrees() {
      return 180.0F;
   }

   @Override
   public Identifier getTextureLocation(LivingEntityRenderState p_364996_) {
      return ENDERMITE_LOCATION;
   }

   public LivingEntityRenderState createRenderState() {
      return new LivingEntityRenderState();
   }
}
