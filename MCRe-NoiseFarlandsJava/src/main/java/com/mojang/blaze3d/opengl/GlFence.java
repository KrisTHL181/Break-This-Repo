package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.buffers.GpuFence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GlFence implements GpuFence {
    private final GlCommandEncoder encoder;
    private final long submitIndex;
    private boolean closedOrCompleted;

    GlFence(final GlCommandEncoder encoder) {
        this.encoder = encoder;
        this.submitIndex = encoder.currentSubmitIndex();
    }

    @Override
    public void close() {
        this.closedOrCompleted = true;
    }

    @Override
    public boolean awaitCompletion(final long timeoutNS) {
        if (this.closedOrCompleted) {
            return true;
        }

        this.closedOrCompleted = this.encoder.awaitSubmit(this.submitIndex, timeoutNS);
        return this.closedOrCompleted;
    }
}