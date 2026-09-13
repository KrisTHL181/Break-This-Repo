package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Consumer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.statue.CopperGolemStatueModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.animal.golem.CopperGolemOxidationLevels;
import net.minecraft.world.level.block.CopperGolemStatueBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public class CopperGolemStatueSpecialRenderer implements NoDataSpecialModelRenderer {
    private final CopperGolemStatueModel model;
    private final Identifier texture;

    public CopperGolemStatueSpecialRenderer(final CopperGolemStatueModel model, final Identifier texture) {
        this.model = model;
        this.texture = texture;
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
        submitNodeCollector.submitModel(this.model, Unit.INSTANCE, poseStack, this.texture, lightCoords, overlayCoords, outlineColor, null);
    }

    @Override
    public void getExtents(final Consumer<Vector3fc> output) {
        PoseStack poseStack = new PoseStack();
        this.model.setupAnim(Unit.INSTANCE);
        this.model.root().getExtentsForGui(poseStack, output);
    }

    @OnlyIn(Dist.CLIENT)
    public record Unbaked(Identifier texture, CopperGolemStatueBlock.Pose pose) implements NoDataSpecialModelRenderer.Unbaked {
        public static final MapCodec<CopperGolemStatueSpecialRenderer.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(
                    Identifier.CODEC.fieldOf("texture").forGetter(CopperGolemStatueSpecialRenderer.Unbaked::texture),
                    CopperGolemStatueBlock.Pose.CODEC.fieldOf("pose").forGetter(CopperGolemStatueSpecialRenderer.Unbaked::pose)
                )
                .apply(i, CopperGolemStatueSpecialRenderer.Unbaked::new)
        );

        public Unbaked(final WeatheringCopper.WeatherState state, final CopperGolemStatueBlock.Pose pose) {
            this(CopperGolemOxidationLevels.getOxidationLevel(state).texture(), pose);
        }

        @Override
        public MapCodec<CopperGolemStatueSpecialRenderer.Unbaked> type() {
            return MAP_CODEC;
        }

        public CopperGolemStatueSpecialRenderer bake(final SpecialModelRenderer.BakingContext context) {
            CopperGolemStatueModel model = new CopperGolemStatueModel(context.entityModelSet().bakeLayer(getModel(this.pose)));
            return new CopperGolemStatueSpecialRenderer(model, this.texture);
        }

        private static ModelLayerLocation getModel(final CopperGolemStatueBlock.Pose pose) {
            return switch (pose) {
                case STANDING -> ModelLayers.COPPER_GOLEM;
                case SITTING -> ModelLayers.COPPER_GOLEM_SITTING;
                case STAR -> ModelLayers.COPPER_GOLEM_STAR;
                case RUNNING -> ModelLayers.COPPER_GOLEM_RUNNING;
            };
        }
    }
}