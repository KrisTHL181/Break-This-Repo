package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.illager.IllagerModel;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.IllagerRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.illager.Pillager;

public class PillagerRenderer extends IllagerRenderer<Pillager, IllagerRenderState> {
   private static final Identifier PILLAGER = Identifier.withDefaultNamespace("textures/entity/illager/pillager.png");

   public PillagerRenderer(final EntityRendererProvider.Context context) {
      super(context, new IllagerModel<>(context.bakeLayer(ModelLayers.PILLAGER)), 0.5F);
      this.addLayer(new ItemInHandLayer<>(this));
   }

   public Identifier getTextureLocation(final IllagerRenderState state) {
      return PILLAGER;
   }

   public IllagerRenderState createRenderState() {
      return new IllagerRenderState();
   }
}
