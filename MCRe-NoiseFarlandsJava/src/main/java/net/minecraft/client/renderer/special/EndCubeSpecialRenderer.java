package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Consumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.AbstractEndPortalRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.StringRepresentable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public class EndCubeSpecialRenderer implements NoDataSpecialModelRenderer {
    private final RenderType renderType;

    public EndCubeSpecialRenderer(final RenderType renderType) {
        this.renderType = renderType;
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
        AbstractEndPortalRenderer.submitSpecial(this.renderType, poseStack, submitNodeCollector);
    }

    @Override
    public void getExtents(final Consumer<Vector3fc> output) {
        AbstractEndPortalRenderer.getExtents(output);
    }

    @OnlyIn(Dist.CLIENT)
    public enum Type implements StringRepresentable {
        PORTAL("portal"),
        GATEWAY("gateway");

        public static final Codec<EndCubeSpecialRenderer.Type> CODEC = StringRepresentable.fromEnum(EndCubeSpecialRenderer.Type::values);
        private final String name;

        Type(final String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record Unbaked(EndCubeSpecialRenderer.Type effect) implements NoDataSpecialModelRenderer.Unbaked {
        public static final MapCodec<EndCubeSpecialRenderer.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(EndCubeSpecialRenderer.Type.CODEC.fieldOf("effect").forGetter(EndCubeSpecialRenderer.Unbaked::effect))
                .apply(i, EndCubeSpecialRenderer.Unbaked::new)
        );

        @Override
        public SpecialModelRenderer<Void> bake(final SpecialModelRenderer.BakingContext context) {
            return new EndCubeSpecialRenderer(switch (this.effect) {
                case PORTAL -> RenderTypes.endPortal();
                case GATEWAY -> RenderTypes.endGateway();
            });
        }

        @Override
        public MapCodec<EndCubeSpecialRenderer.Unbaked> type() {
            return MAP_CODEC;
        }
    }
}