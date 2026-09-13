#ifndef NET_MINECRAFT_WORLD_LEVEL_BIOME__BiomeSource_H__
#define NET_MINECRAFT_WORLD_LEVEL_BIOME__BiomeSource_H__

#include "../../../util/Random.h"
#include "../levelgen/synth/PerlinNoise.h"

typedef PerlinNoise PerlinSimplexNoise;
class Level;
class Biome;
class ChunkPos;

class BiomeSource
{
protected:
    BiomeSource();
public:
    BiomeSource(Level* level);
    virtual ~BiomeSource();

    float* temperatures;
    float* downfalls;
    float* noises;

    double m_offsetX = 0.0;
    double m_offsetZ = 0.0;
    double  m_scaleX = 1.0f;
    double  m_scaleZ = 1.0f;

    int lenTemperatures;
    int lenDownfalls;
    int lenNoises;
    int lenBiomes;

    virtual Biome* getBiome(const ChunkPos& chunk);
    virtual Biome* getBiome(int x, int z);

    // 获取一块区域内的生物群系（2D）
    virtual Biome** getBiomeBlock(int x, int z, int w, int h);
    void setWorldTransform(double offsetX, double offsetZ, double scaleX, double scaleZ) {
        m_offsetX = offsetX;
        m_offsetZ = offsetZ;
        m_scaleX = scaleX;
        m_scaleZ = scaleZ;
    }

private:
    virtual Biome** getBiomeBlock(Biome** biomes, int x, int z, int w, int h);

    Biome** biomes;
    PerlinSimplexNoise* temperatureMap;
    PerlinSimplexNoise* downfallMap;
    PerlinSimplexNoise* noiseMap;

    Random rndTemperature;
    Random rndDownfall;
    Random rndNoise;

    static const float zoom;
    static const float tempScale;
    static const float downfallScale;
    static const float noiseScale;
};

#endif
