package net.minecraft.client.renderer.chunk;

import com.mojang.blaze3d.pipeline.RenderTarget;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum ChunkSectionLayerGroup {
    OPAQUE(ChunkSectionLayer.SOLID, ChunkSectionLayer.CUTOUT),
    TRANSLUCENT(ChunkSectionLayer.TRANSLUCENT);

    private final String label;
    private final ChunkSectionLayer[] layers;

    ChunkSectionLayerGroup(final ChunkSectionLayer... layers) {
        this.layers = layers;
        this.label = this.toString().toLowerCase(Locale.ROOT);
    }

    public String label() {
        return this.label;
    }

    public ChunkSectionLayer[] layers() {
        return this.layers;
    }

    public RenderTarget outputTarget() {
        Minecraft minecraft = Minecraft.getInstance();

        RenderTarget renderTarget = switch (this) {
            case TRANSLUCENT -> minecraft.levelRenderer.translucentTarget();
            default -> minecraft.gameRenderer.mainRenderTarget();
        };
        return renderTarget != null ? renderTarget : minecraft.gameRenderer.mainRenderTarget();
    }
}