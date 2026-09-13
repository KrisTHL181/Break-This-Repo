package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.state.MinecartRenderState;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;

public class MinecartRenderer extends AbstractMinecartRenderer<AbstractMinecart, MinecartRenderState> {
   public MinecartRenderer(final EntityRendererProvider.Context context, final ModelLayerLocation model) {
      super(context, model);
   }

   public MinecartRenderState createRenderState() {
      return new MinecartRenderState();
   }
}
