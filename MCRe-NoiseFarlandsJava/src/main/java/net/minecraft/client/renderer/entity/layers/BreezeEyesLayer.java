package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.breeze.BreezeModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.BreezeRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BreezeEyesLayer extends RenderLayer<BreezeRenderState, BreezeModel> {
    private static final RenderType BREEZE_EYES = RenderTypes.breezeEyes(Identifier.withDefaultNamespace("textures/entity/breeze/breeze_eyes.png"));
    private final BreezeModel model;

    public BreezeEyesLayer(final RenderLayerParent<BreezeRenderState, BreezeModel> renderer, final EntityModelSet modelSet) {
        super(renderer);
        this.model = new BreezeModel(modelSet.bakeLayer(ModelLayers.BREEZE_EYES));
    }

    public void submit(
        final PoseStack poseStack,
        final SubmitNodeCollector submitNodeCollector,
        final int lightCoords,
        final BreezeRenderState state,
        final float yRot,
        final float xRot
    ) {
        submitNodeCollector.order(1).submitModel(this.model, state, poseStack, BREEZE_EYES, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
    }
}