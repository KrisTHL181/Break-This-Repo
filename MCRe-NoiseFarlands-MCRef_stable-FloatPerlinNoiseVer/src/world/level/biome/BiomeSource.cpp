#include "BiomeSource.h"
#include "Biome.h"
#include "../Level.h"
#include "../ChunkPos.h"

const float BiomeSource::zoom = 2 * 1;
const float BiomeSource::tempScale = zoom / 80.0f;
const float BiomeSource::downfallScale = zoom / 40.0f;
const float BiomeSource::noiseScale = 1 / 4.0f;

BiomeSource::BiomeSource()
    : temperatureMap(NULL),
      downfallMap(NULL),
      noiseMap(NULL),
      lenTemperatures(0),
      lenDownfalls(0),
      lenNoises(0),
      lenBiomes(0),
      temperatures(NULL),
      downfalls(NULL),
      noises(NULL),
      biomes(NULL)
{
    biomes = new Biome*[16 * 16];
}

BiomeSource::BiomeSource(Level* level)
    : rndTemperature(level->getSeed() * 9871),
      rndDownfall(level->getSeed() * 39811),
      rndNoise(level->getSeed() * 543321),
      lenTemperatures(0),
      lenDownfalls(0),
      lenNoises(0),
      lenBiomes(0),
      temperatures(NULL),
      downfalls(NULL),
      noises(NULL),
      biomes(NULL)
{
    temperatureMap = new PerlinSimplexNoise(&rndTemperature, 4);
    downfallMap   = new PerlinSimplexNoise(&rndDownfall, 4);
    noiseMap      = new PerlinSimplexNoise(&rndNoise, 2);

    biomes = new Biome*[16 * 16];
    temperatures = new float[16 * 16];
downfalls    = new float[16 * 16];
noises       = new float[16 * 16];
}

BiomeSource::~BiomeSource()
{
    delete temperatureMap;
    delete downfallMap;
    delete noiseMap;
    delete[] temperatures;
    delete[] downfalls;
    delete[] noises;
    delete[] biomes;
}

Biome* BiomeSource::getBiome(const ChunkPos& chunk)
{
    return getBiome(chunk.x << 4, chunk.z << 4);
}

Biome* BiomeSource::getBiome(int x, int z)
{
    return getBiomeBlock(x, z, 1, 1)[0];
}

Biome** BiomeSource::getBiomeBlock(int x, int z, int w, int h)
{
    biomes = getBiomeBlock(biomes, x, z, w, h);
    return biomes;
}

Biome** BiomeSource::getBiomeBlock(Biome** biomes__, int x, int z, int w, int h)
{
    double tx = (x + m_offsetX) * m_scaleX;
    double tz = (z + m_offsetZ) * m_scaleZ;

    // 使用 int 坐标调用 8 参数版本 getRegion（2D）
    temperatures = temperatureMap->getRegion(temperatures, (int)tx, (int)tz,
                                             w, h, tempScale, tempScale, 0.25f);
    downfalls    = downfallMap->getRegion(downfalls,    (int)tx, (int)tz,
                                             w, h, downfallScale, downfallScale, 0.3333f);
    noises       = noiseMap->getRegion(noises,         (int)tx, (int)tz,
                                             w, h, noiseScale, noiseScale, 0.588f);

    int pp = 0;
    for (int yy = 0; yy < w; yy++) {
        for (int xx = 0; xx < h; xx++) {
            double noiseVal = (noises[pp] * 1.1 + 0.5);

            double split2 = 0.01;
            double split1 = 1 - split2;
            double temperature = (temperatures[pp] * 0.15 + 0.7) * split1 + noiseVal * split2;
            split2 = 0.002;
            split1 = 1 - split2;
            double downfall = (downfalls[pp] * 0.15 + 0.5) * split1 + noiseVal * split2;

            temperature = 1 - ((1 - temperature) * (1 - temperature));
            if (temperature < 0) temperature = 0;
            if (downfall   < 0) downfall   = 0;
            if (temperature > 1) temperature = 1;
            if (downfall   > 1) downfall   = 1;

            temperatures[pp] = temperature;
            downfalls[pp]    = downfall;
            biomes[pp++] = Biome::getBiome((float)temperature, (float)downfall);
        }
    }

    return biomes;
}
