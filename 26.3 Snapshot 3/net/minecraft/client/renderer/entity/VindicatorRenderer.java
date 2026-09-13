package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.illager.IllagerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.IllagerRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.illager.Vindicator;

public class VindicatorRenderer extends IllagerRenderer<Vindicator, IllagerRenderState> {
   private static final Identifier VINDICATOR = Identifier.withDefaultNamespace("textures/entity/illager/vindicator.png");

   public VindicatorRenderer(final EntityRendererProvider.Context context) {
      super(context, new IllagerModel<>(context.bakeLayer(ModelLayers.VINDICATOR)), 0.5F);
      this.addLayer(
         new ItemInHandLayer<IllagerRenderState, IllagerModel<IllagerRenderState>>(this) {
            public void submit(
               final PoseStack poseStack,
               final SubmitNodeCollector submitNodeCollector,
               final int lightCoords,
               final IllagerRenderState state,
               final float yRot,
               final float xRot
            ) {
               if (state.isAggressive) {
                  super.submit(poseStack, submitNodeCollector, lightCoords, state, yRot, xRot);
               }
            }
         }
      );
   }

   public Identifier getTextureLocation(final IllagerRenderState state) {
      return VINDICATOR;
   }

   public IllagerRenderState createRenderState() {
      return new IllagerRenderState();
   }
}
