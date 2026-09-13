package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.bell.BellModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BellRenderer;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public class BellSpecialRenderer implements NoDataSpecialModelRenderer {
    private static final BellModel.State STATE = new BellModel.State(0.0F, null);
    private final BellModel model;
    private final SpriteGetter sprites;

    public BellSpecialRenderer(final SpriteGetter sprites, final BellModel model) {
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
        submitNodeCollector.submitModel(
            this.model, STATE, poseStack, lightCoords, overlayCoords, -1, BellRenderer.BELL_TEXTURE, this.sprites, outlineColor, null
        );
    }

    @Override
    public void getExtents(final Consumer<Vector3fc> output) {
        PoseStack poseStack = new PoseStack();
        this.model.setupAnim(STATE);
        this.model.root().getExtentsForGui(poseStack, output);
    }

    @OnlyIn(Dist.CLIENT)
    public record Unbaked() implements NoDataSpecialModelRenderer.Unbaked {
        public static final MapCodec<BellSpecialRenderer.Unbaked> MAP_CODEC = MapCodec.unit(new BellSpecialRenderer.Unbaked());

        @Override
        public MapCodec<BellSpecialRenderer.Unbaked> type() {
            return MAP_CODEC;
        }

        public BellSpecialRenderer bake(final SpecialModelRenderer.BakingContext context) {
            return new BellSpecialRenderer(context.sprites(), new BellModel(context.entityModelSet().bakeLayer(ModelLayers.BELL)));
        }
    }
}