package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Function;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LivingEntityEmissiveLayer<S extends LivingEntityRenderState, M extends EntityModel<S>> extends RenderLayer<S, M> {
    private final Function<S, Identifier> textureProvider;
    private final LivingEntityEmissiveLayer.AlphaFunction<S> alphaFunction;
    private final M model;
    private final Function<Identifier, RenderType> bufferProvider;
    private final boolean alwaysVisible;

    public LivingEntityEmissiveLayer(
        final RenderLayerParent<S, M> renderer,
        final Function<S, Identifier> textureProvider,
        final LivingEntityEmissiveLayer.AlphaFunction<S> alphaFunction,
        final M model,
        final Function<Identifier, RenderType> bufferProvider,
        final boolean alwaysVisible
    ) {
        super(renderer);
        this.textureProvider = textureProvider;
        this.alphaFunction = alphaFunction;
        this.model = model;
        this.bufferProvider = bufferProvider;
        this.alwaysVisible = alwaysVisible;
    }

    public void submit(
        final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final int lightCoords, final S state, final float yRot, final float xRot
    ) {
        if (!state.isInvisible || this.alwaysVisible) {
            float alpha = this.alphaFunction.apply(state, state.ageInTicks);
            if (!(alpha <= 1.0E-5F)) {
                int color = ARGB.white(alpha);
                RenderType renderType = this.bufferProvider.apply(this.textureProvider.apply(state));
                submitNodeCollector.order(1)
                    .submitModel(
                        this.model,
                        state,
                        poseStack,
                        renderType,
                        lightCoords,
                        LivingEntityRenderer.getOverlayCoords(state, 0.0F),
                        color,
                        null,
                        state.outlineColor,
                        null
                    );
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public interface AlphaFunction<S extends LivingEntityRenderState> {
        float apply(final S state, final float ageInTicks);
    }
}