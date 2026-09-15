package net.minecraft.client.renderer.state.gui;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record PanoramaRenderState(float spin) {
}