package net.minecraft.client.renderer;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.shaders.UniformType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BindGroupLayouts {
    public static final BindGroupLayout DYNAMIC_TRANSFORMS = BindGroupLayout.builder().withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).build();
    public static final BindGroupLayout PROJECTION = BindGroupLayout.builder().withUniform("Projection", UniformType.UNIFORM_BUFFER).build();
    public static final BindGroupLayout MATRICES_PROJECTION = BindGroupLayout.builder()
        .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
        .withUniform("Projection", UniformType.UNIFORM_BUFFER)
        .build();
    public static final BindGroupLayout CHUNK_SECTION = BindGroupLayout.builder().withUniform("ChunkSection", UniformType.UNIFORM_BUFFER).build();
    // MCRe：26.3 MultiDrawIndirect 移植——共享地形 uniform（ModelView 矩阵 + 纹理尺寸），每帧一次
    public static final BindGroupLayout TERRAIN_INFO = BindGroupLayout.builder().withUniform("TerrainUniform", UniformType.UNIFORM_BUFFER).build();
    public static final BindGroupLayout FOG = BindGroupLayout.builder().withUniform("Fog", UniformType.UNIFORM_BUFFER).build();
    public static final BindGroupLayout GLOBALS = BindGroupLayout.builder().withUniform("Globals", UniformType.UNIFORM_BUFFER).build();
    public static final BindGroupLayout LIGHTING = BindGroupLayout.builder().withUniform("Lighting", UniformType.UNIFORM_BUFFER).build();
    public static final BindGroupLayout SAMPLER0 = BindGroupLayout.builder().withSampler("Sampler0").build();
    public static final BindGroupLayout SAMPLER1 = BindGroupLayout.builder().withSampler("Sampler1").build();
    public static final BindGroupLayout SAMPLER2 = BindGroupLayout.builder().withSampler("Sampler2").build();
    public static final BindGroupLayout SAMPLER0_SAMPLER2 = BindGroupLayout.builder().withSampler("Sampler0").withSampler("Sampler2").build();
    public static final BindGroupLayout SAMPLER0_SAMPLER1 = BindGroupLayout.builder().withSampler("Sampler0").withSampler("Sampler1").build();
    public static final BindGroupLayout SAMPLER0_SAMPLER1_SAMPLER2 = BindGroupLayout.builder()
        .withSampler("Sampler0")
        .withSampler("Sampler1")
        .withSampler("Sampler2")
        .build();
    public static final BindGroupLayout CLOUD_INFO = BindGroupLayout.builder()
        .withUniform("CloudInfo", UniformType.UNIFORM_BUFFER)
        .withUniform("CloudFaces", UniformType.TEXEL_BUFFER, GpuFormat.R8_SINT)
        .build();
    public static final BindGroupLayout DISSOLVE_MASK_SAMPLER = BindGroupLayout.builder().withSampler("DissolveMaskSampler").build();
    public static final BindGroupLayout IN_SAMPLER = BindGroupLayout.builder().withSampler("InSampler").build();
    public static final BindGroupLayout LIGHTMAP_INFO = BindGroupLayout.builder().withUniform("LightmapInfo", UniformType.UNIFORM_BUFFER).build();
    public static final BindGroupLayout SPRITE_ANIMATION_INFO = BindGroupLayout.builder()
        .withUniform("SpriteAnimationInfo", UniformType.UNIFORM_BUFFER)
        .build();
    public static final BindGroupLayout SPRITE = BindGroupLayout.builder().withSampler("Sprite").build();
    public static final BindGroupLayout CURRENT_SPRITE_NEXT_SPRITE = BindGroupLayout.builder().withSampler("CurrentSprite").withSampler("NextSprite").build();

    private BindGroupLayouts() {
    }
}