package net.minecraft.client.renderer.entity.layers;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.projectile.ArrowModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.TippableArrowRenderer;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ArrowLayer<M extends PlayerModel> extends StuckInBodyLayer<M, ArrowRenderState> {
   public ArrowLayer(LivingEntityRenderer<?, AvatarRenderState, M> p_174466_, EntityRendererProvider.Context p_174465_) {
      super(
         p_174466_,
         new ArrowModel(p_174465_.bakeLayer(ModelLayers.ARROW)),
         new ArrowRenderState(),
         TippableArrowRenderer.NORMAL_ARROW_LOCATION,
         StuckInBodyLayer.PlacementStyle.IN_CUBE
      );
   }

   @Override
   protected int numStuck(AvatarRenderState p_429839_) {
      return p_429839_.arrowCount;
   }
}
