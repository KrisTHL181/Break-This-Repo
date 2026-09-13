package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.ConduitRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public class ConduitSpecialRenderer implements NoDataSpecialModelRenderer {
    private final SpriteGetter sprites;
    private final ModelPart model;

    public ConduitSpecialRenderer(final SpriteGetter sprites, final ModelPart model) {
        this.sprites = sprites;
        this.model = model;
    }

    @Override
    public void submit(
        final PoseStack poseStack,
        final SubmitNodeCollector submitNodeCollector,
        final int lightCoords,
        final int overlayCoords,
        final boolean hasFoil,
        final int outlineColor
    ) {
        submitNodeCollector.submitModelPart(
            this.model,
            poseStack,
            ConduitRenderer.SHELL_TEXTURE.renderType(RenderTypes::entitySolid),
            lightCoords,
            overlayCoords,
            this.sprites.get(ConduitRenderer.SHELL_TEXTURE),
            -1,
            null,
            outlineColor
        );
    }

    @Override
    public void getExtents(final Consumer<Vector3fc> output) {
        PoseStack poseStack = new PoseStack();
        this.model.getExtentsForGui(poseStack, output);
    }

    @OnlyIn(Dist.CLIENT)
    public record Unbaked() implements NoDataSpecialModelRenderer.Unbaked {
        public static final MapCodec<ConduitSpecialRenderer.Unbaked> MAP_CODEC = MapCodec.unit(new ConduitSpecialRenderer.Unbaked());

        @Override
        public MapCodec<ConduitSpecialRenderer.Unbaked> type() {
            return MAP_CODEC;
        }

        public ConduitSpecialRenderer bake(final SpecialModelRenderer.BakingContext context) {
            return new ConduitSpecialRenderer(context.sprites(), context.entityModelSet().bakeLayer(ModelLayers.CONDUIT_SHELL));
        }
    }
}