package net.minecraft.client.renderer.block;

import java.util.Map;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BlockStateModelSet {
    private final Map<BlockState, BlockStateModel> modelByState;
    private final BlockStateModel missingModel;

    public BlockStateModelSet(final Map<BlockState, BlockStateModel> modelByState, final BlockStateModel missingModel) {
        this.modelByState = modelByState;
        this.missingModel = missingModel;
    }

    public BlockStateModel get(final BlockState state) {
        return this.modelByState.getOrDefault(state, this.missingModel);
    }

    public BlockStateModel missingModel() {
        return this.missingModel;
    }

    public Material.Baked getParticleMaterial(final BlockState blockState) {
        return this.get(blockState).particleMaterial();
    }
}