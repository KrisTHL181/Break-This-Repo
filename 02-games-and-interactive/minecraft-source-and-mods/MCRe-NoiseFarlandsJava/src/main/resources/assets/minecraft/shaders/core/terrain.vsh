#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <minecraft:terrainuniform.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:sample_lightmap.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;

#ifdef MULTIDRAW_TERRAIN
// MCRe：MultiDrawIndirect——section 数据从 instanced vertex binding 1 读取（CPU 已算相机相对偏移）
in ivec3 ChunkPosition;
in float InChunkVisibility;
#else
#moj_import <minecraft:chunksection.glsl>
#endif

uniform sampler2D Sampler2;

out float sphericalVertexDistance;
out float cylindricalVertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;
out float chunkVisibility;

void main() {
#ifdef MULTIDRAW_TERRAIN
    vec3 pos = Position + vec3(ChunkPosition) + CameraOffset;
    chunkVisibility = InChunkVisibility;
#else
    vec3 pos = Position + vec3(ChunkPosition) + CameraOffset;
    chunkVisibility = ChunkVisibility;
#endif
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    sphericalVertexDistance = fog_spherical_distance(pos);
    cylindricalVertexDistance = fog_cylindrical_distance(pos);
    vertexColor = Color * sample_lightmap(Sampler2, UV2);
    texCoord0 = UV0;
}
