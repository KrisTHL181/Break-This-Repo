package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.ravager.RavagerModel;
import net.minecraft.client.renderer.entity.state.RavagerRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RavagerRenderer extends MobRenderer<Ravager, RavagerRenderState, RavagerModel> {
   private static final Identifier TEXTURE_LOCATION = Identifier.withDefaultNamespace("textures/entity/illager/ravager.png");

   public RavagerRenderer(EntityRendererProvider.Context p_174362_) {
      super(p_174362_, new RavagerModel(p_174362_.bakeLayer(ModelLayers.RAVAGER)), 1.1F);
   }

   public Identifier getTextureLocation(RavagerRenderState p_456424_) {
      return TEXTURE_LOCATION;
   }

   public RavagerRenderState createRenderState() {
      return new RavagerRenderState();
   }

   public void extractRenderState(Ravager p_364295_, RavagerRenderState p_364078_, float p_362823_) {
      super.extractRenderState(p_364295_, p_364078_, p_362823_);
      p_364078_.stunnedTicksRemaining = p_364295_.getStunnedTick() > 0.0F ? p_364295_.getStunnedTick() - p_362823_ : 0.0F;
      p_364078_.attackTicksRemaining = p_364295_.getAttackTick() > 0.0F ? p_364295_.getAttackTick() - p_362823_ : 0.0F;
      if (p_364295_.getRoarTick() > 0) {
         p_364078_.roarAnimation = (20 - p_364295_.getRoarTick() + p_362823_) / 20.0F;
      } else {
         p_364078_.roarAnimation = 0.0F;
      }
   }
}
