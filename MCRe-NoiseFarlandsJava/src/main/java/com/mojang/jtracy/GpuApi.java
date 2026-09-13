package com.mojang.jtracy;

/**
 * An API that is being used by a {@link GpuContext}.
 */
public enum GpuApi {
    /**
     * An invalid or unknown API context.
     */
    INVALID(0),

    /**
     * An OpenGL (or GL ES) context.
     */
    OPENGL(1),

    /**
     * A Vulkan context.
     */
    VULKAN(2),

    /**
     * An OpenCL context.
     */
    OPENCL(3),

    /**
     * A DirectX 12 context.
     */
    DIRECT3D_12(4),

    /**
     * A DirectX 11 context.
     */
    DIRECT3D_11(5),
    ;

    private final int id;

    GpuApi(final int id) {
        this.id = id;
    }

    int getId() {
        return id;
    }
}
