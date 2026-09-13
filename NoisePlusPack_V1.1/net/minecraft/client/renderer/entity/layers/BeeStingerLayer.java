package net.minecraft.client.renderer.entity.layers;

import net.minecraft.client.model.animal.bee.BeeStingerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BeeStingerLayer<M extends PlayerModel> extends StuckInBodyLayer<M, Unit> {
   private static final Identifier BEE_STINGER_LOCATION = Identifier.withDefaultNamespace("textures/entity/bee/bee_stinger.png");

   public BeeStingerLayer(LivingEntityRenderer<?, AvatarRenderState, M> p_116580_, EntityRendererProvider.Context p_367387_) {
      super(
         p_116580_,
         new BeeStingerModel(p_367387_.bakeLayer(ModelLayers.BEE_STINGER)),
         Unit.INSTANCE,
         BEE_STINGER_LOCATION,
         StuckInBodyLayer.PlacementStyle.ON_SURFACE
      );
   }

   @Override
   protected int numStuck(AvatarRenderState p_431024_) {
      return p_431024_.stingerCount;
   }
}
