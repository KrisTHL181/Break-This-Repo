#version 330

// MCRe：26.3 MultiDrawIndirect 移植——共享地形 uniform（每帧一次）
// ModelView 矩阵 + 纹理尺寸，替代原 ChunkSection UBO 中每区块重复的矩阵
layout(std140) uniform TerrainUniform {
    mat4 ModelViewMat;
    ivec2 TextureSize;
};
