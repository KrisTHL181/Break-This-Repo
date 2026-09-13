package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.animal.sheep.SheepFurModel;
import net.minecraft.client.model.animal.sheep.SheepModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.SheepRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SheepWoolLayer extends RenderLayer<SheepRenderState, SheepModel> {
    private static final Identifier SHEEP_WOOL_LOCATION = Identifier.withDefaultNamespace("textures/entity/sheep/sheep_wool.png");
    private static final Identifier BABY_SHEEP_WOOL_LOCATION = Identifier.withDefaultNamespace("textures/entity/sheep/sheep_wool_baby.png");
    private final EntityModel<SheepRenderState> adultModel;
    private final EntityModel<SheepRenderState> babyModel;

    public SheepWoolLayer(final RenderLayerParent<SheepRenderState, SheepModel> renderer, final EntityModelSet modelSet) {
        super(renderer);
        this.adultModel = new SheepFurModel(modelSet.bakeLayer(ModelLayers.SHEEP_WOOL));
        this.babyModel = new SheepFurModel(modelSet.bakeLayer(ModelLayers.SHEEP_BABY_WOOL));
    }

    public void submit(
        final PoseStack poseStack,
        final SubmitNodeCollector submitNodeCollector,
        final int lightCoords,
        final SheepRenderState state,
        final float yRot,
        final float xRot
    ) {
        if (!state.isSheared) {
            EntityModel<SheepRenderState> model = state.isBaby ? this.babyModel : this.adultModel;
            Identifier location = state.isBaby ? BABY_SHEEP_WOOL_LOCATION : SHEEP_WOOL_LOCATION;
            if (state.isInvisible) {
                if (state.appearsGlowing()) {
                    submitNodeCollector.submitModel(
                        model,
                        state,
                        poseStack,
                        RenderTypes.outline(location),
                        lightCoords,
                        LivingEntityRenderer.getOverlayCoords(state, 0.0F),
                        -16777216,
                        null,
                        state.outlineColor,
                        null
                    );
                }
            } else {
                coloredCutoutModelCopyLayerRender(
                    model, location, poseStack, submitNodeCollector, lightCoords, state, state.getWoolColor(), state.isBaby ? 1 : 0
                );
            }
        }
    }
}