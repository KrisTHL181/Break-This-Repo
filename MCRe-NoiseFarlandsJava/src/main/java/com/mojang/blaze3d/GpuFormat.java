package com.mojang.blaze3d;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum GpuFormat {
    R8_UNORM(GpuFormat.ComponentType.UNORM_8, 1),
    R8_SNORM(GpuFormat.ComponentType.SNORM_8, 1),
    RG8_UNORM(GpuFormat.ComponentType.UNORM_8, 2),
    RG8_SNORM(GpuFormat.ComponentType.SNORM_8, 2),
    RGB8_UNORM(GpuFormat.ComponentType.UNORM_8, 3),
    RGB8_SNORM(GpuFormat.ComponentType.SNORM_8, 3),
    RGBA8_UNORM(GpuFormat.ComponentType.UNORM_8, 4),
    RGBA8_SNORM(GpuFormat.ComponentType.SNORM_8, 4),
    R16_UNORM(GpuFormat.ComponentType.UNORM_16, 1),
    R16_SNORM(GpuFormat.ComponentType.SNORM_16, 1),
    RG16_UNORM(GpuFormat.ComponentType.UNORM_16, 2),
    RG16_SNORM(GpuFormat.ComponentType.SNORM_16, 2),
    RGB16_UNORM(GpuFormat.ComponentType.UNORM_16, 3),
    RGB16_SNORM(GpuFormat.ComponentType.SNORM_16, 3),
    RGBA16_UNORM(GpuFormat.ComponentType.UNORM_16, 4),
    RGBA16_SNORM(GpuFormat.ComponentType.SNORM_16, 4),
    R8_UINT(GpuFormat.ComponentType.UINT_8, 1),
    R8_SINT(GpuFormat.ComponentType.SINT_8, 1),
    RG8_UINT(GpuFormat.ComponentType.UINT_8, 2),
    RG8_SINT(GpuFormat.ComponentType.SINT_8, 2),
    RGB8_UINT(GpuFormat.ComponentType.UINT_8, 3),
    RGB8_SINT(GpuFormat.ComponentType.SINT_8, 3),
    RGBA8_UINT(GpuFormat.ComponentType.UINT_8, 4),
    RGBA8_SINT(GpuFormat.ComponentType.SINT_8, 4),
    R16_UINT(GpuFormat.ComponentType.UINT_16, 1),
    R16_SINT(GpuFormat.ComponentType.SINT_16, 1),
    RG16_UINT(GpuFormat.ComponentType.UINT_16, 2),
    RG16_SINT(GpuFormat.ComponentType.SINT_16, 2),
    RGB16_UINT(GpuFormat.ComponentType.UINT_16, 3),
    RGB16_SINT(GpuFormat.ComponentType.SINT_16, 3),
    RGBA16_UINT(GpuFormat.ComponentType.UINT_16, 4),
    RGBA16_SINT(GpuFormat.ComponentType.SINT_16, 4),
    R32_UINT(GpuFormat.ComponentType.UINT_32, 1),
    R32_SINT(GpuFormat.ComponentType.SINT_32, 1),
    RG32_UINT(GpuFormat.ComponentType.UINT_32, 2),
    RG32_SINT(GpuFormat.ComponentType.SINT_32, 2),
    RGB32_UINT(GpuFormat.ComponentType.UINT_32, 3),
    RGB32_SINT(GpuFormat.ComponentType.SINT_32, 3),
    RGBA32_UINT(GpuFormat.ComponentType.UINT_32, 4),
    RGBA32_SINT(GpuFormat.ComponentType.SINT_32, 4),
    R16_FLOAT(GpuFormat.ComponentType.FLOAT_16, 1),
    RG16_FLOAT(GpuFormat.ComponentType.FLOAT_16, 2),
    RGB16_FLOAT(GpuFormat.ComponentType.FLOAT_16, 3),
    RGBA16_FLOAT(GpuFormat.ComponentType.FLOAT_16, 4),
    R32_FLOAT(GpuFormat.ComponentType.FLOAT_32, 1),
    RG32_FLOAT(GpuFormat.ComponentType.FLOAT_32, 2),
    RGB32_FLOAT(GpuFormat.ComponentType.FLOAT_32, 3),
    RGBA32_FLOAT(GpuFormat.ComponentType.FLOAT_32, 4),
    RGB10A2_UNORM(GpuFormat.ComponentType.OPAQUE_32, 1),
    RGB10A2_UINT(GpuFormat.ComponentType.OPAQUE_32, 1),
    RG11B10_FLOAT(GpuFormat.ComponentType.OPAQUE_32, 1),
    D32_FLOAT(GpuFormat.ComponentType.OPAQUE_32, 1),
    D32_FLOAT_S8_UINT(GpuFormat.ComponentType.OPAQUE_64, 1),
    D24_UNORM_S8_UINT(GpuFormat.ComponentType.OPAQUE_32, 1),
    D16_UNORM(GpuFormat.ComponentType.OPAQUE_16, 1),
    S8_UINT(GpuFormat.ComponentType.OPAQUE_8, 1);

    private final GpuFormat.ComponentType componentType;
    private final int componentCount;

    GpuFormat(final GpuFormat.ComponentType componentType, final int componentCount) {
        this.componentType = componentType;
        this.componentCount = componentCount;
    }

    public int blockSize() {
        return this.componentType.byteSize() * this.componentCount;
    }

    public int byteAlignment() {
        return this.componentType.byteSize();
    }

    public GpuFormat.ComponentType componentType() {
        return this.componentType;
    }

    public int componentCount() {
        return this.componentCount;
    }

    public boolean hasColorAspect() {
        return !this.hasDepthAspect() && !this.hasStencilAspect();
    }

    public boolean hasDepthAspect() {
        return this == D32_FLOAT || this == D32_FLOAT_S8_UINT || this == D24_UNORM_S8_UINT || this == D16_UNORM;
    }

    public boolean hasStencilAspect() {
        return this == S8_UINT || this == D32_FLOAT_S8_UINT || this == D24_UNORM_S8_UINT;
    }

    @OnlyIn(Dist.CLIENT)
    public enum ComponentType {
        UNORM_8(1),
        SNORM_8(1),
        UINT_8(1),
        SINT_8(1),
        UNORM_16(2),
        SNORM_16(2),
        UINT_16(2),
        SINT_16(2),
        FLOAT_16(2),
        UINT_32(4),
        SINT_32(4),
        FLOAT_32(4),
        OPAQUE_8(1),
        OPAQUE_16(2),
        OPAQUE_32(4),
        OPAQUE_64(8);

        private final int byteSize;

        ComponentType(final int byteSize) {
            this.byteSize = byteSize;
        }

        public int byteSize() {
            return this.byteSize;
        }
    }
}