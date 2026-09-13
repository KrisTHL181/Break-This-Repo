package net.minecraft.client.renderer.state.level;

import java.util.Set;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;


import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ChunkLoadingRenderState {
    public Set<SectionPos> addedEmptySections = new java.util.HashSet<>();
    public Set<SectionPos> removedEmptySections = new java.util.HashSet<>();
    public Set<ChunkPos> addedLoadedChunks = new java.util.HashSet<>();
    public Set<ChunkPos> removedLoadedChunks = new java.util.HashSet<>();
    public Set<ChunkPos> loadedExpectedChunks = new java.util.HashSet<>();

    public void reset() {
        this.loadedExpectedChunks.clear();
    }
}