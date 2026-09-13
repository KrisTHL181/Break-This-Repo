package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.animal.wolf.WolfModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.WolfRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;

public class WolfCollarLayer extends RenderLayer<WolfRenderState, WolfModel> {
   private static final Identifier WOLF_COLLAR_LOCATION = Identifier.withDefaultNamespace("textures/entity/wolf/wolf_collar.png");
   private static final Identifier WOLF_BABY_COLLAR_LOCATION = Identifier.withDefaultNamespace("textures/entity/wolf/wolf_collar_baby.png");

   public WolfCollarLayer(final RenderLayerParent<WolfRenderState, WolfModel> renderer) {
      super(renderer);
   }

   public void submit(
      final PoseStack poseStack,
      final SubmitNodeCollector submitNodeCollector,
      final int lightCoords,
      final WolfRenderState state,
      final float yRot,
      final float xRot
   ) {
      DyeColor collarColor = state.collarColor;
      if (collarColor != null && !state.isInvisible) {
         int color = collarColor.getTextureDiffuseColor();
         Identifier collarLocation = state.isBaby ? WOLF_BABY_COLLAR_LOCATION : WOLF_COLLAR_LOCATION;
         submitNodeCollector.order(1)
            .submitModel(
               this.getParentModel(),
               state,
               poseStack,
               RenderTypes.entityCutout(collarLocation),
               lightCoords,
               OverlayTexture.NO_OVERLAY,
               color,
               null,
               state.outlineColor
            );
      }
   }
}
