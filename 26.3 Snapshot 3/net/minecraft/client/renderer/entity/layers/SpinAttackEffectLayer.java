package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.effects.SpinAttackEffectModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public class SpinAttackEffectLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
   public static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/trident/trident_riptide.png");
   private final SpinAttackEffectModel model;

   public SpinAttackEffectLayer(final RenderLayerParent<AvatarRenderState, PlayerModel> renderer, final EntityModelSet modelSet) {
      super(renderer);
      this.model = new SpinAttackEffectModel(modelSet.bakeLayer(ModelLayers.PLAYER_SPIN_ATTACK));
   }

   public void submit(
      final PoseStack poseStack,
      final SubmitNodeCollector submitNodeCollector,
      final int lightCoords,
      final AvatarRenderState state,
      final float yRot,
      final float xRot
   ) {
      if (state.isAutoSpinAttack) {
         submitNodeCollector.submitModel(this.model, state, poseStack, TEXTURE, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
      }
   }
}
