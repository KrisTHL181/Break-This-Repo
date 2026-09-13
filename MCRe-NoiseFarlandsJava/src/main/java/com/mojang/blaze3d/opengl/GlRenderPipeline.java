package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record GlRenderPipeline(RenderPipeline info, GlProgram program) implements CompiledRenderPipeline {
    @Override
    public boolean isValid() {
        return this.program != GlProgram.INVALID_PROGRAM;
    }
}