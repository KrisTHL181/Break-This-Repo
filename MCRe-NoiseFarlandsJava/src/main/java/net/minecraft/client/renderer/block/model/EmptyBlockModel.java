package net.minecraft.client.renderer.block.model;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4fc;

@OnlyIn(Dist.CLIENT)
public class EmptyBlockModel implements BlockModel {
    public static final BlockModel INSTANCE = new EmptyBlockModel();

    @Override
    public void update(final BlockModelRenderState output, final BlockState blockState, final BlockDisplayContext displayContext, final long seed) {
    }

    @OnlyIn(Dist.CLIENT)
    public record Unbaked() implements BlockModel.Unbaked {
        @Override
        public BlockModel bake(final BlockModel.BakingContext context, final Matrix4fc transformation) {
            return EmptyBlockModel.INSTANCE;
        }
    }
}