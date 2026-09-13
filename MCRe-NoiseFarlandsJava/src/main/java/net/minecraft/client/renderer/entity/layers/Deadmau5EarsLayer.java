package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerEarsModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class Deadmau5EarsLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private final HumanoidModel<AvatarRenderState> model;

    public Deadmau5EarsLayer(final RenderLayerParent<AvatarRenderState, PlayerModel> renderer, final EntityModelSet modelSet) {
        super(renderer);
        this.model = new PlayerEarsModel(modelSet.bakeLayer(ModelLayers.PLAYER_EARS));
    }

    public void submit(
        final PoseStack poseStack,
        final SubmitNodeCollector submitNodeCollector,
        final int lightCoords,
        final AvatarRenderState state,
        final float yRot,
        final float xRot
    ) {
        if (state.showExtraEars && !state.isInvisible) {
            int overlayCoords = LivingEntityRenderer.getOverlayCoords(state, 0.0F);
            submitNodeCollector.submitModel(
                this.model, state, poseStack, RenderTypes.entitySolid(state.skin.body().texturePath()), lightCoords, overlayCoords, state.outlineColor, null
            );
        }
    }
}