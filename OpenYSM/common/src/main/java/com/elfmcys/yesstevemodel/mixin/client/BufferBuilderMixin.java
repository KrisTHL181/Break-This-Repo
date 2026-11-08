package com.elfmcys.yesstevemodel.mixin.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import rip.ysm.annotations.BufferBuilderMapping;

import java.nio.ByteBuffer;

@Mixin(BufferBuilder.class) // OYSM PATCH
public abstract class BufferBuilderMixin {
    @Shadow @BufferBuilderMapping("buffer_builder_buffer")
    private ByteBuffer buffer;

    @Shadow @BufferBuilderMapping("buffer_builder_vertices")
    private int vertices;

    @Shadow @BufferBuilderMapping("buffer_builder_nextElementByte")
    private int nextElementByte;

    @Shadow @BufferBuilderMapping("buffer_builder_ensureCapacity")
    protected abstract void ensureCapacity(int size);

    @Shadow @BufferBuilderMapping("buffer_builder_mode")
    private VertexFormat.Mode mode;
}
