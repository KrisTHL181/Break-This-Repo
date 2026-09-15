#version 330

// MCRe：26.3 MultiDrawIndirect 移植——ChunkSection 精简为 16 字节（矩阵移至共享 TerrainUniform）
layout(std140) uniform ChunkSection {
    ivec3 ChunkPosition;
    float ChunkVisibility;
};
