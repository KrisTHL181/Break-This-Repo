package net.minecraft.client.renderer.block.model.properties.conditional;

import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class IsXmas implements ConditionalBlockModelProperty {
    @Override
    public boolean get(final BlockState blockState) {
        return ChestRenderer.xmasTextures();
    }
}