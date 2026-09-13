package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.state.OptionsRenderState;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record FeatureFrameContext(
    OptionsRenderState options,
    Font font,
    BlockStateModelSet blockStateModelSet,
    BlockColors blockColors,
    TextureManager textureManager,
    AtlasManager atlasManager,
    GpuTextureView lightmap,
    StagedVertexBuffer stagedVertexBuffer
) {
}