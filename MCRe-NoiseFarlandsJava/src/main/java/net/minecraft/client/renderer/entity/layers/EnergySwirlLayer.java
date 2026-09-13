package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class EnergySwirlLayer<S extends EntityRenderState, M extends EntityModel<S>> extends RenderLayer<S, M> {
    public EnergySwirlLayer(final RenderLayerParent<S, M> renderer) {
        super(renderer);
    }

    @Override
    public void submit(
        final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final int lightCoords, final S state, final float yRot, final float xRot
    ) {
        if (this.isPowered(state)) {
            float t = state.ageInTicks;
            M model = this.model();
            submitNodeCollector.order(1)
                .submitModel(
                    model,
                    state,
                    poseStack,
                    RenderTypes.energySwirl(this.getTextureLocation(), this.xOffset(t) % 1.0F, t * 0.01F % 1.0F),
                    lightCoords,
                    OverlayTexture.NO_OVERLAY,
                    -8355712,
                    null,
                    state.outlineColor,
                    null
                );
        }
    }

    protected abstract boolean isPowered(S state);

    protected abstract float xOffset(final float t);

    protected abstract Identifier getTextureLocation();

    protected abstract M model();
}