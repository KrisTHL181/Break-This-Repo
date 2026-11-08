package net.minecraft.client.renderer.state.level;

import net.minecraft.client.renderer.chunk.RenderSectionRegion;

public record SectionUpdateRenderState(long sectionNode, boolean playerChanged, RenderSectionRegion region) {
}
