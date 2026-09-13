package net.minecraft.client.renderer.entity.layers;

import net.minecraft.client.model.monster.enderman.EndermanModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.EndermanRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class EnderEyesLayer extends EyesLayer<EndermanRenderState, EndermanModel<EndermanRenderState>> {
   private static final RenderType ENDERMAN_EYES = RenderTypes.eyes(Identifier.withDefaultNamespace("textures/entity/enderman/enderman_eyes.png"));

   public EnderEyesLayer(final RenderLayerParent<EndermanRenderState, EndermanModel<EndermanRenderState>> renderer) {
      super(renderer);
   }

   @Override
   public RenderType renderType() {
      return ENDERMAN_EYES;
   }
}
